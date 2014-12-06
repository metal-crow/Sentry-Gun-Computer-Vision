package main;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.UnsupportedCommOperationException;
import hardware_interaction.ArduinoInteraction;

import java.awt.event.WindowAdapter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JFrame;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.highgui.VideoCapture;
import org.opencv.imgproc.Imgproc;

import user_view.RealTime_Video_Showing;
 
/**
 * Class that only have movement detection and laser detection capacities
 */
public class Main {
 
	private static Mat prevFrame;
	private static Mat curFrame;
	private static Mat nextFrame;
	private static int frame_count=0;
	public static int frameArea;

	//Green min hue 42.5,max hue 70
	//Opencv is 0-180 hue range, so if you change this remember
	public static final double[] laser_color_range={67,80};
		
	//sending information to hardware and arduino
	private static ArduinoInteraction arduinoOut;
	public static boolean foundTarget=false;
	
	//saftly exit loop to close program
    private static boolean exit=false;
	
    public static void main(String[] args) throws UnsupportedCommOperationException, PortInUseException, NoSuchPortException, InterruptedException, IOException {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        arduinoOut=new ArduinoInteraction();
    	
        prevFrame=new Mat();
    	curFrame=new Mat();
    	nextFrame=new Mat();
    	
        String filename="testing/green laser.avi";
        VideoCapture video = new VideoCapture(filename);
    	
        //VideoCapture video = new VideoCapture(0);
        video.read(curFrame);
    	video.read(nextFrame);
    	frameArea=nextFrame.rows()*nextFrame.cols();
    	
		//frame to view the video in real time
		JFrame f = new JFrame();
        f.setSize(nextFrame.cols(),nextFrame.rows());
        RealTime_Video_Showing panel = new RealTime_Video_Showing(nextFrame.cols(),nextFrame.rows());
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
        	//move the frame buffer of 3 frames forward one
        	prevFrame=curFrame.clone();
        	curFrame=nextFrame.clone();
        	video.retrieve(nextFrame);
        	
        	//DEBUGGING: frame copy to draw on
    		Mat drawImg=nextFrame.clone();
	        long time=System.currentTimeMillis();//DEBUGGING TIME
	        
	        
	        //Green dot detection
	        ArrayList<Pair<int[], Integer>> laser_points = null;
			try {
				laser_points = greenDotDetection(drawImg);
			} catch (Exception e1) {
				System.err.println("Laser pointer error");
				e1.printStackTrace();
			}
	        
	        //pick the point with the highest priority (most likely to be laser point)
			Pair<int[], Integer> lasertarget=Pair.with(new int[]{}, -1);
    		for(Pair<int[], Integer> t:laser_points){
    			if(t.getValue1()>lasertarget.getValue1()){
    				lasertarget=t;
    				foundTarget=true;
    			}
    		}
	        
	        //if we find the laser, set the middle of the rectangle as the point to be shot
	        if(foundTarget){
	        	try {
	        		arduinoOut.arduinoScreenPositiontoAngle(lasertarget.getValue0(),curFrame.width(),curFrame.height());
				} catch (IOException e) {
					e.printStackTrace();
				}
                System.out.println("Laser coord: "+Arrays.toString(lasertarget.getValue0()));
	        	Core.circle(drawImg, new Point(lasertarget.getValue0()[0],lasertarget.getValue0()[1]), 3, new Scalar(255,0,0),-1);//DEBUGGING
	        }
	        
            
        	//movement detection
    		ArrayList<Pair<int[], Integer>> targets = null;
			try {
				targets = movementDetection();
			} catch (Exception e1) {
				System.err.println("Movement error");
				e1.printStackTrace();
			}
    		
    		//laser has priority over movement
    		//if we have any movement
    		if(!foundTarget && targets.size()>0){
	    		foundTarget=true;

	    		//get the highest priority target (most likely to be a person)
	    		Pair<int[], Integer> movementtarget=targets.get(0);
	    		for(Pair<int[], Integer> t:targets){
	    			if(t.getValue1()>movementtarget.getValue1()){
	    				movementtarget=t;
	    			}
	    		}
	    		System.out.println("Movement coord: "+Arrays.toString(movementtarget.getValue0()));
	    		
    			//set the coordinate as the point to be shot
	        	try {
	        		arduinoOut.arduinoScreenPositiontoAngle(movementtarget.getValue0(),curFrame.width(),curFrame.height());
				} catch (IOException e) {
					e.printStackTrace();
				}
	        	Core.circle(drawImg, new Point(movementtarget.getValue0()[0],movementtarget.getValue0()[1]), 4, new Scalar(0,0,255),-1);//DEBUGGING
    		}
	        
    		//TODO if we have not found any movement or laser pointer, perform at rest person identification
    		if(!foundTarget){
    			
    		}
    		
    		//if we have found a target, we have already sent its coordinates to the arduino. now shoot
    		if(foundTarget){
    			arduinoOut.fire();
    		}
    		
	        System.out.println("Time for frame "+frame_count+" (millisec) : "+(System.currentTimeMillis()-time));
	        
	        //DEBUG WRITING FOR TESTING
			try {
				panel.setImage(drawImg);
		        //Highgui.imwrite("testing/test/"+frame_count+"output.jpg",drawImg);
			} catch (IOException e) {
				e.printStackTrace();
			}
			
    		//reset shooting info
			foundTarget=false;
    		arduinoOut.flush();
    		
    		frame_count++;
        }
        
        System.out.println("Exit");
        arduinoOut.close();
        video.release();
        f.setVisible(false);
        f.dispose();
        // Wait 2 seconds for the port to close, then shutdown
        Thread.sleep(2000);
    }
    
	/**Get a area in the video that is movement */
    private static ArrayList<Pair<int[], Integer>> movementDetection() throws Exception{
    	//get the difference between the frames
    	//this is done based on http://blog.cedric.ws/opencv-simple-motion-detection
    	Mat absDiff1=new Mat();
    	Core.absdiff(prevFrame, nextFrame, absDiff1);
    	Mat absDiff2=new Mat();
    	Core.absdiff(curFrame, nextFrame, absDiff2);
    	Mat frameDiff=new Mat();
    	Core.bitwise_and(absDiff1, absDiff2, frameDiff);
    	Imgproc.threshold(frameDiff, frameDiff, 35, 255, Imgproc.THRESH_BINARY);
        Imgproc.cvtColor(frameDiff, frameDiff, Imgproc.COLOR_BGR2GRAY);
		//Highgui.imwrite("testing/movement/"+frame_count+"output FD.jpg",frameDiff);
    	
    	//now that be have the mat of movement, get each unique blob of movement in it.
    	return BlobDetection.findSolidBlobs(frameDiff,BlobDetection.BASIC_IDENTIFICATION);
    }
    
    /** find green dot in image */
    private static ArrayList<Pair<int[], Integer>> greenDotDetection(Mat drawImg) throws Exception{
    	//Green dot detection
    	Mat hsv_channel = new Mat();
        Imgproc.cvtColor(nextFrame,hsv_channel, Imgproc.COLOR_BGR2HSV);
        //Hue,Saturation,Brightness
        Core.inRange(hsv_channel,new Scalar(laser_color_range[0],75,180),new Scalar(laser_color_range[1],255,255),hsv_channel);
        ArrayList<Mat> laser_binary_channels = new ArrayList<Mat>(3);
        Core.split(hsv_channel, laser_binary_channels);
        //Highgui.imwrite("testing/laser/"+frame_count+"output LZ.jpg",hsv_channel);
        
        return BlobDetection.findSolidBlobs(laser_binary_channels.get(laser_binary_channels.size()-1),BlobDetection.LASER_IDENTIFICATION);
    }
}