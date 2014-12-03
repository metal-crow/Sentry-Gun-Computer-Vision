package main;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
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
		Imgproc.findContours(img, contours, new Mat(), Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_SIMPLE);
		Rect box=Imgproc.boundingRect(contours.get(0));
		
		//find the center of mass in this area
		//do a binary search on the image in both dimensions. Split the image into 4 parts, find the side with the larger # of blob pixels, resize the image to that segment, and repeat.
		//When we reach 1 px width and height, we now have x,y coordinate for center of mass.
		int[] rect={box.x,box.y,box.x+box.width,box.y+box.height};
		
		while(blobimg.width()>1 && blobimg.height()>1){
			//split image and see which corner has more blob
			int totalpixels=blobimg.width()*blobimg.height();
			int countLT = totalpixels-Core.countNonZero(blobimg.submat(0, blobimg.rows()/2, 0, blobimg.cols()/2));
			int countRT = totalpixels-Core.countNonZero(blobimg.submat(0, blobimg.rows()/2, blobimg.cols()/2, blobimg.cols()));
			int countLB = totalpixels-Core.countNonZero(blobimg.submat(blobimg.rows()/2, blobimg.rows(), 0, blobimg.cols()/2));
			int countRB = totalpixels-Core.countNonZero(blobimg.submat(blobimg.rows()/2, blobimg.rows(), blobimg.cols()/2, blobimg.cols()));
			
			if(countLT>countRT && countLT>countLB && countLT>countRB){
				rect=new int[]{rect[0], rect[1], (rect[0]+rect[2])/2, (rect[1]+rect[3])/2};
				blobimg=new Mat(blobimg, new Rect(0, 0, blobimg.cols()/2, blobimg.rows()/2));
			}
			else if(countRT>countLT && countRT>countLB && countRT>countRB){
				rect=new int[]{(rect[0]+rect[2])/2, rect[1], rect[2], (rect[1]+rect[3])/2};
				blobimg=new Mat(blobimg, new Rect(blobimg.cols()/2, 0, blobimg.cols()/2, blobimg.rows()/2));
			}
			else if(countLB>countLT && countLB>countRT && countLB>countRB){
				rect=new int[]{rect[0], (rect[1]+rect[3])/2, (rect[0]+rect[2])/2, rect[3]};
				blobimg=new Mat(blobimg, new Rect(0, blobimg.rows()/2, blobimg.cols()/2, blobimg.rows()/2));
			}
			else{
				rect=new int[]{(rect[0]+rect[2])/2, (rect[1]+rect[3])/2, rect[2], rect[3]};
				blobimg=new Mat(blobimg, new Rect(blobimg.cols()/2, blobimg.rows()/2, blobimg.cols()/2, blobimg.rows()/2));
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
