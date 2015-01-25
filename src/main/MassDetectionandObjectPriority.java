package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.MatOfPoint2f;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import person_identification.DetectPerson;

/**
 * Given an image containing a blob, and its color, find its center of mass.
 */
public class MassDetectionandObjectPriority implements Callable<Pair<int[],Integer>>{
	
	private Mat img;
	private int color;
	private int identificationType;
	private boolean onlyPersonDetection;
	private int[] personDetectionPoint;
	
	/**
	 * @return the center of mass of the blob (x,y), and its priority as a target
	 */
	private Pair<int[],Integer> BlobInformation(){
		//remove all other blobs from image, and convert to binary mat
		//get a mask of the color
		Mat blobimg=new Mat();
		Core.inRange(img, new Scalar(color), new Scalar(color), blobimg);
        //Highgui.imwrite("testing/blob/"+Main.frame_count+" "+img.hashCode()+"output.jpg",blobimg);
		//get a bounding rectangle around the blob
		//is findCountours really the fastest and best method for this?
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(blobimg.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_NONE);
		Rect boundingbox=Imgproc.boundingRect(contours.get(0));//there should always be only one (flood fill from BlobDetection)
		
		//find the center of mass in this area
		//do a binary search on the image in the both dimension.
		//Split the image into 4 parts, find the corner with the larger # of blob pixels, store that area in rect, and repeat.
		//When we reach 1 px width and height of the rect, we now have x,y coordinate for center of mass.
		int[] rect={boundingbox.x,boundingbox.y,boundingbox.x+boundingbox.width,boundingbox.y+boundingbox.height};
		
		while(rect[3]-rect[1]>1 && rect[2]-rect[0]>1){			
			//split image and see which corner has more blob
			int countLT = Core.countNonZero(blobimg.submat(rect[1], (rect[1]+rect[3])/2, rect[0], (rect[0]+rect[2])/2));
			int countRT = Core.countNonZero(blobimg.submat(rect[1], (rect[1]+rect[3])/2, (rect[0]+rect[2])/2, rect[2]));
			int countLB = Core.countNonZero(blobimg.submat((rect[1]+rect[3])/2, rect[3], rect[0], (rect[0]+rect[2])/2));
			int countRB = Core.countNonZero(blobimg.submat((rect[1]+rect[3])/2, rect[3], (rect[0]+rect[2])/2, rect[2]));
						
			if(countLT>countRT && countLT>countLB && countLT>countRB){
				rect=new int[]{rect[0], rect[1], (rect[0]+rect[2])/2, (rect[1]+rect[3])/2};
			}
			else if(countRT>countLT && countRT>countLB && countRT>countRB){
				rect=new int[]{(rect[0]+rect[2])/2, rect[1], rect[2], (rect[1]+rect[3])/2};
			}
			else if(countLB>countLT && countLB>countRT && countLB>countRB){
				rect=new int[]{rect[0], (rect[1]+rect[3])/2, (rect[0]+rect[2])/2, rect[3]};
			}
			//all corners being equal, this will bias towards the bottom right
			else{
				rect=new int[]{(rect[0]+rect[2])/2, (rect[1]+rect[3])/2, rect[2], rect[3]};
			}
		}
		
		int[] point={rect[0],rect[1]};
		int priority=0;
		
		if(identificationType==ImagePartitioning.BASIC_IDENTIFICATION){
		    //calculate priority based on size of blob
	        int blobArea=Core.countNonZero(blobimg.submat(boundingbox.y, boundingbox.y+boundingbox.height, boundingbox.x, boundingbox.x+boundingbox.width));
		    priority=blobArea;
		}
		else if(identificationType==ImagePartitioning.LASER_IDENTIFICATION){
		    int blobArea=Core.countNonZero(blobimg.submat(boundingbox.y, boundingbox.y+boundingbox.height, boundingbox.x, boundingbox.x+boundingbox.width));
		    
	        //density of the area (# of blob pixels/area of rectangle)
            double density=(double)blobArea/(double)(boundingbox.width*boundingbox.height);

            //check circularity of blob (more likely to be laser point)
            //convert contour to matofpoint2f for use in finding perimeter
            MatOfPoint2f peremeter = new MatOfPoint2f();
            contours.get(0).convertTo(peremeter, CvType.CV_32FC2);
            
            double circularity=4*Math.PI*blobArea/Math.pow(Imgproc.arcLength(peremeter,true),2);
            
            //convert both to a ranking mechanism for the likelihood this is a laser point
            priority=(int) ((density*circularity)*100);
            //System.out.println("laser density "+density+" laser circularity "+circularity);//DEBUGGING
		}
		else if(identificationType==ImagePartitioning.PERSON_IDENTIFICATION){
		    //get a mask of the original image of the blob
		    Mat originalblob=new Mat();
		    //make sure we dont just get a mask of the blob, but everything inside the blob as well (i.e a movement crescent)
		    originalblob=Main.curFrame.submat(boundingbox);
		    
		    Mat test=Main.curFrame.clone();
		    Core.rectangle(test, new Point(boundingbox.x,boundingbox.y), new Point(boundingbox.x+boundingbox.width,boundingbox.y+boundingbox.height), new Scalar(0,255,0));
	        //Highgui.imwrite("testing/person/"+Main.frame_count+" "+boundingbox.x+"output.jpg",test);
	        
		    //find likelihood this is a person
		    priority=DetectPerson.isBlobHuman(originalblob);
		}

		return Pair.with(point, priority);
	}
	
	/**
	 * @param imgpointer the image
	 * @param colorOfBlob the color of the blob
	 * @param identification The type of blob this is, and how to classify its priority.
	 */
	public MassDetectionandObjectPriority(Mat imgpointer,int colorOfBlob, int identification) {
		identificationType=identification;
		img=imgpointer;
		color=colorOfBlob;
	}
	
	/**
	 * Use this constructor if the image should only be put through DetectPerson. Use for the full color at rest detection, when we dont have a binary mat.
	 * @param img
	 */
	public MassDetectionandObjectPriority(int y, int x, Mat img) {
		this.img=img;
		personDetectionPoint=new int[]{x+(img.width()/2),y+(img.height()/2)};
		this.onlyPersonDetection=true;
	}
	
	@Override
	public Pair<int[],Integer> call() throws Exception {
		if(onlyPersonDetection){
			return Pair.with(personDetectionPoint, DetectPerson.isBlobHuman(img));
		}else{
			return BlobInformation();
		}
	}
	
	public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Mat img=Highgui.imread("testing/circle.png");
    	Imgproc.threshold(img, img, 35, 255, Imgproc.THRESH_BINARY);
    	Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);
    	
    	MassDetectionandObjectPriority a= new MassDetectionandObjectPriority(img, 255, 1);
    	Pair<int[],Integer> r=a.call();
    	System.out.println(Arrays.toString(r.getValue0()));
	}
}
