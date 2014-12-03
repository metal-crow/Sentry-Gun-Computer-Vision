package main;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Callable;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

/**
 * Given an image containing a blob, and its color, find its center of mass.
 */
public class MassDetectionandObjectPriority implements Callable<Pair<int[],Integer>>{
	
	private Mat img;
	private int color;
	private int identificationType;
	
	/**
	 * @return the center of mass of the blob (x,y), and its priority as a target
	 */
	private Pair<int[],Integer> BlobInformation(){
		//remove all other blobs from image, and convert to binary mat
		//get a mask of the color
		Mat blobimg=new Mat();
		Core.inRange(img, new Scalar(color), new Scalar(color), blobimg);
		//get a bounding rectangle around the blob
		//is findCountours really the fastest and best method for this?
		List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
		Imgproc.findContours(img.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE);
		Rect box=Imgproc.boundingRect(contours.get(0));
		
		//find the center of mass in this area
		//do a binary search on the image in both dimensions. Split the image into 4 parts, find the side with the larger # of blob pixels, resize the image to that segment, and repeat.
		//When we reach 1 px width and height, we now have x,y coordinate for center of mass.
		int[] rect={box.x,box.y,box.x+box.width,box.y+box.height};
		
		int i=0;
		while(blobimg.width()>1 && blobimg.height()>1){
			Highgui.imwrite("test images/"+color+" "+i+".png", blobimg);
			System.out.println(color+" "+Arrays.toString(rect));
			i++;
			
			//split image and see which corner has more blob
			int countLT = Core.countNonZero(blobimg.submat(rect[0], (rect[0]+rect[2])/2, rect[1], (rect[1]+rect[3])/2));
			int countRT = Core.countNonZero(blobimg.submat((rect[0]+rect[2])/2, rect[2], rect[1], (rect[1]+rect[3])/2));
			int countLB = Core.countNonZero(blobimg.submat(rect[0], (rect[0]+rect[2])/2, (rect[1]+rect[3])/2, rect[3]));
			int countRB = Core.countNonZero(blobimg.submat((rect[0]+rect[2])/2, rect[2], (rect[1]+rect[3])/2, rect[3]));
			
			System.out.println(countLT+" "+countRT+" "+countLB+" "+countRB);
			
			if(countLT>countRT && countLT>countLB && countLT>countRB){
				System.out.println("lt");
				rect=new int[]{rect[0], rect[1], (rect[0]+rect[2])/2, (rect[1]+rect[3])/2};
				blobimg=new Mat(blobimg, new Rect(//TODO));
			}
			else if(countRT>countLT && countRT>countLB && countRT>countRB){
				System.out.println("rt");
				rect=new int[]{(rect[0]+rect[2])/2, rect[1], rect[2], (rect[1]+rect[3])/2};
				blobimg=new Mat(blobimg, new Rect(//TODO));
			}
			else if(countLB>countLT && countLB>countRT && countLB>countRB){
				System.out.println("lb");
				rect=new int[]{rect[0], (rect[1]+rect[3])/2, (rect[0]+rect[2])/2, rect[3]};
				blobimg=new Mat(blobimg, new Rect(//TODO));
			}
			else{
				System.out.println("rb");
				rect=new int[]{(rect[0]+rect[2])/2, (rect[1]+rect[3])/2, rect[2], rect[3]};
				blobimg=new Mat(blobimg, new Rect(//TODO));
			}
		}
		
		int[] point={rect[0]+1,rect[1]+1};
		
		//TODO find priority of blob
		
		//for person identification, a detected face makes it priority 10. Otherwise, check for other features.
		//do in separate file

		return Pair.with(point, 10);
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
	
	@Override
	public Pair<int[],Integer> call() throws Exception {
		return BlobInformation();
	}
}
