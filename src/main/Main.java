package main;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;

import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JFrame;

import linking.ArduinoInteraction;
import linking.User_GUI;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import preprocessing.ImagePartitioning;
 
/**
 * Class that only have movement detection and laser detection capacities
 */
public class Main {
 
	private static Mat prevFrame;
	public static Mat curFrame;//public so that sub methods can access it without it being passed through their parent
	private static Mat nextFrame;
	public static int frame_count=0;
	
	//Green min hue 42.5,max hue 70
	//Opencv is 0-180 hue range, so if you change this remember
	public static final double[] laser_color_range={67,80};
		
	//sending information to hardware and arduino
	private static ArduinoInteraction arduinoOut;
	public static volatile Target target=new Target();
	
	//Safely exit loop to close program
    private static boolean exit=false;
	
    //give the application a memory. Store last know locations of objects for last 60 frames
    public static int[][] objectPermanence=new int[60][2];
    
    public static void main(String[] args) throws UnsupportedCommOperationException, PortInUseException, NoSuchPortException, InterruptedException, IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        try{
            arduinoOut=new ArduinoInteraction();
        }catch(Exception e){
            System.err.println("Arduino Disabled");
        }
    	
        prevFrame=new Mat();
    	curFrame=new Mat();
    	nextFrame=new Mat();
    	
        //String filename="testing/movement test.avi";
        //VideoCapture video = new VideoCapture(filename);
    	VideoCapture video = new VideoCapture(0);
    	
        video.read(curFrame);
    	video.read(nextFrame);
    	
		//frame for gui
		JFrame f = new JFrame();
        f.setSize(nextFrame.cols(),nextFrame.rows());
        User_GUI panel = new User_GUI(nextFrame.cols(),nextFrame.rows());
		f.add(panel);
        f.pack();
        f.setVisible(true);
        //on window close, safely exit
        f.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(java.awt.event.WindowEvent windowEvent) {
            	exit=true;
            }
        });
        
        while(video.isOpened() && video.grab() && !exit){
            //release the last frame
            prevFrame.release();
            
        	//move the frame buffer of 3 frames forward one
        	prevFrame=curFrame.clone();
        	curFrame=nextFrame.clone();
        	video.retrieve(nextFrame);
        	
        	//DEBUGGING: frame copy to draw on
    		Mat drawImg=nextFrame.clone();
	        long time=System.currentTimeMillis();//DEBUGGING TIME
            	        
	        //Green dot detection
	        ArrayList<Pair<int[], Integer>> laser_points = greenDotDetection(drawImg);
	        //pick the point with the priority greater than the given min (most likely to be laser point)
    		for(Pair<int[], Integer> t:laser_points){
    			if(t.getValue1()>target.priority()){
    			    target.setTarget(t);
    			}
    			if(target.foundTarget()){System.out.println("Laser detected");}
    		}
            
    		
        	//movement detection
    		ArrayList<Pair<int[], Integer>> targets = movementDetection();
    		//make sure we haven't already set the target as laser has priority over movement
    		//if we have any movement
    		if(!target.foundTarget() && targets.size()>0){
	    	    //get the highest priority target (most likely to be a person)
	    		//get any movement to compare other movements with.
	    		target.setTarget(targets.get(0));
	            for(Pair<int[], Integer> t:targets){
	                if(t.getValue1()>target.priority()){
	                    target.setTarget(t);
	                }
	            }
	            System.out.println("Movement detected");
    		}
	        
    		
    		//if we have not found any movement or laser pointer, perform at rest person identification
    		//edge case where the first couple of frames are empty
    		if(!target.foundTarget() && Main.curFrame.width()!=0){
        		targets = ImagePartitioning.FragmentationSplitting(curFrame,4);
        		//if none of these results have detected a face or skin, they will return the size of their fragment as priority
    	        for(Pair<int[], Integer> t:targets){
    	            
    	            if(
	                    //exclude any results with a priority < frameArea (see detectperson for how priority generation works and why this works)
	                    t.getValue1()>(curFrame.width()*curFrame.height()) && t.getValue1()>target.priority()
    	              )
    	            {
                        target.setTarget(t);
    	            }
    	        }
    	        if(target.foundTarget()){System.out.println("Rest detection found "+(target.priority()>(2*(Main.curFrame.width()*Main.curFrame.height()))?"a face":"skin"));}
    		}
    		
    		
    		//if we have found a target, send its coordinates to the arduino and shoot
    		if(target.foundTarget()){
    		    try{
                    System.out.println("Target coord: "+Arrays.toString(target.coords()));//DEBUGGING
                    if(arduinoOut!=null){
                        arduinoOut.arduinoScreenPositiontoAngle(target.coords(),curFrame.width(),curFrame.height());
                        arduinoOut.fire();
                    }
                    Core.circle(drawImg, new Point(target.coords()[0],target.coords()[1]), 4, new Scalar(0,0,255),-1);//DEBUGGING
    		    }catch(IOException e){
    		        System.err.println("Error firing");
    		    }
    		}
    		
	        System.out.println("Time for frame "+frame_count+" (millisec) : "+(System.currentTimeMillis()-time)+"-------------------------------");
	        
			try {
				panel.setImage(drawImg);
		        //Highgui.imwrite("testing/test/"+frame_count+"output.jpg",drawImg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			//free gui frame copy
			drawImg.release();
			
    		//reset shooting info
			target.reset();
			if(arduinoOut!=null){
			    arduinoOut.flush();
			}
    		frame_count++;
        }
        
        System.out.println("Exit");
        if(arduinoOut!=null){
            arduinoOut.close();
        }
        video.release();
        f.setVisible(false);
        f.dispose();
        // Wait 2 seconds for the port to close, then shutdown
        Thread.sleep(2000);
    }
    
    /**
     * Get a area in the video that is movement
     */
    private static ArrayList<Pair<int[], Integer>> movementDetection(){
    	//get the difference between the frames
    	//this is done based on http://blog.cedric.ws/opencv-simple-motion-detection
    	Mat absDiff1=new Mat();
    	Core.absdiff(prevFrame, nextFrame, absDiff1);
    	Mat absDiff2=new Mat();
    	Core.absdiff(curFrame, nextFrame, absDiff2);
    	
        //also do a blur to reduce artifacts and blend areas of movement for better movement detection
    	Imgproc.blur(absDiff1, absDiff1, new Size(6,6));
        Imgproc.blur(absDiff2, absDiff2, new Size(6,6));

        Mat frameDiff=new Mat();
    	Core.bitwise_and(absDiff1, absDiff2, frameDiff);
    	absDiff1.release();
    	absDiff2.release();
        Imgproc.cvtColor(frameDiff, frameDiff, Imgproc.COLOR_BGR2GRAY);
    	Imgproc.threshold(frameDiff, frameDiff, 35, 255, Imgproc.THRESH_BINARY);
    	
    	//now that be have the mat of movement, get each unique blob of movement in it.
    	ArrayList<Pair<int[], Integer>> store = ImagePartitioning.OutlineBlobDetection(frameDiff,ImagePartitioning.PERSON_IDENTIFICATION);
    	
        //Highgui.imwrite("testing/movement/"+frame_count+"output.jpg",frameDiff);
        frameDiff.release();

        return store;
    }
    
    /** find green dot in image */
    private static ArrayList<Pair<int[], Integer>> greenDotDetection(Mat drawImg){
    	//Green dot detection
    	Mat hsv_channel = new Mat();
        Imgproc.cvtColor(nextFrame,hsv_channel, Imgproc.COLOR_BGR2HSV);
        //Hue,Saturation,Brightness
        Core.inRange(hsv_channel,new Scalar(laser_color_range[0],75,180),new Scalar(laser_color_range[1],255,255),hsv_channel);
        ArrayList<Mat> laser_binary_channels = new ArrayList<Mat>(3);
        Core.split(hsv_channel, laser_binary_channels);
        
        //Highgui.imwrite("testing/laser/"+frame_count+"output LZ.jpg",hsv_channel);
        ArrayList<Pair<int[], Integer>> result = ImagePartitioning.BasicBlobDetection(laser_binary_channels.get(laser_binary_channels.size()-1),4,ImagePartitioning.LASER_IDENTIFICATION);
        //frees
        hsv_channel.release();
        for(Mat m:laser_binary_channels){
            m.release();
        }
        return result;
    }
}