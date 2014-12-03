package main;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.javatuples.Pair;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

public class BlobDetection {
	
	//method options for determining priority of blobs. Used in method.
	public static final int
		BASIC_IDENTIFICATION=0,
		LASER_IDENTIFICATION=1,
		PERSON_IDENTIFICATION=2;

	/**
	 * Find blobs in a binary image. White is assumed to be the blobs of interest
	 * @param img a binary image. This is manipulated by this method.
	 * @param identification The type of blob this is, and how to classify its priority.
	 * <ul>
	 * <li>Use <code>BASIC_IDENTIFICATION</code> for computing priority based on blob size.
	 * <li>Use <code>LASER_IDENTIFICATION</code> for computing priority based on likelihood the blob is the laser pointer.
	 * <li>Use <code>PERSON_IDENTIFICATION</code> for computing priority based on likelihood the blob is a person.
	 * </ul>
	 * @return an array containing the center of each blob, and the blob's priority
	 * @throws Exception if the supplied priority identification type is invalid
	 */
	public static ArrayList<Pair<int[], Integer>> findBlobs(Mat img, int identification) throws Exception{
		if(identification<0 || identification>2){
			throw new Exception("Invalid priority identifier.");
		}
		//Is a SimpleBlobDetector http://docs.opencv.org/modules/features2d/doc/common_interfaces_of_feature_detectors.html#simpleblobdetector better for this?
    	int blobColor=100;//note:this will cause errors for >254 blobs in an image
        ExecutorService executor = Executors.newCachedThreadPool();
		ArrayList<Future<Pair<int[],Integer>>> tasks = new ArrayList<Future<Pair<int[],Integer>>>();

    	for(int y=0;y<img.rows();y++){
    		for(int x=0;x<img.cols();x++){
    			//the top left corner of a blob
    			if(img.get(y, x)[0]==255){
    				//flood fill the blob
    				Imgproc.floodFill(img, new Mat(), new Point(x,y), new Scalar(blobColor));
                    Highgui.imwrite("test images/"+blobColor+"flood fill.png", img);

    				//get the blob's center of mass and its priority in a thread.
    				Callable<Pair<int[],Integer>> thread=new MassDetectionandObjectPriority(img, blobColor, identification);
    				tasks.add(executor.submit(thread));
                    blobColor++;
    			}
    		}
    	}
    	
    	//get the results of the threads
    	ArrayList<Pair<int[], Integer>> targets=new ArrayList<Pair<int[], Integer>>(tasks.size());
    	
    	for(Future<Pair<int[],Integer>> task:tasks){
    		try {
				targets.add(task.get());
			} catch (InterruptedException | ExecutionException e) {
				e.printStackTrace();
			}
    	}
		
    	//return threads results
		return targets;
	}
}