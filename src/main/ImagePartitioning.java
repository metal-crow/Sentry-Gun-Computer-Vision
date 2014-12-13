package main;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

public class ImagePartitioning {
	
	//method options for determining priority of blobs. Used in method.
	public static final int
		BASIC_IDENTIFICATION=0,
		LASER_IDENTIFICATION=1,
		PERSON_IDENTIFICATION=2;
	
	private static final int minBlobAreaPX=3000;

	/**
	 * Find solid color blobs in a binary image. White is assumed to be the blobs of interest
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
	public static ArrayList<Pair<int[], Integer>> BlobDetection(Mat img, int identification) throws Exception{
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
    				
                    //if blob had enough pixels to be a valid blob (not noise, artifacts, save threads)
    				//TODO is there a way to get this from flood fill? Would be a lot faster than this.
    		        Mat singleBlob=new Mat();
    		        Core.inRange(img, new Scalar(blobColor), new Scalar(blobColor), singleBlob);
    		        int areaOfBlob=Core.countNonZero(singleBlob);
    		        singleBlob.release();
    		        
    		        if(areaOfBlob>=minBlobAreaPX){
        				//get the blob's center of mass and its priority in a thread.
        				Callable<Pair<int[],Integer>> thread=new MassDetectionandObjectPriority(img, blobColor, identification);
        				tasks.add(executor.submit(thread));
                        blobColor++;
    		        }
    		        else{
    		        	//if this blob is too small, delete it
    		        	Imgproc.floodFill(img, new Mat(), new Point(x,y), new Scalar(0));
    		        }
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

	/**
	 * Split the image into equal sized fragments, and check for a person in each fragment.
	 * Used for at rest person detection
	 * @param fragments the total number of fragments to split the image into.
	 */
	public static ArrayList<Pair<int[], Integer>> FragmentationSplitting(int fragments) {
        ExecutorService executor = Executors.newFixedThreadPool(fragments);
		ArrayList<Future<Pair<int[],Integer>>> tasks = new ArrayList<Future<Pair<int[],Integer>>>(fragments);

		//fragment the image
		//we want each fragment to be as close to a square as possible to maximize available area to analyze
		int fragmentSide=Math.round((float)Math.sqrt((Main.curFrame.width()*Main.curFrame.height())/fragments));
		System.out.println(fragmentSide);
		int numberOfFragmentsPerRow=Math.round(Main.curFrame.width()/fragmentSide);
		int fragmentCount=1;
		
		int y=0;
		while(y+fragmentSide<Main.curFrame.height()){
			int x=0;
			while(x+fragmentSide<Main.curFrame.width()){
                System.out.println((y+fragmentSide)+"/"+Main.curFrame.height()+" "+(x+fragmentSide)+"/"+Main.curFrame.width());
				//To fully honor the correct # of fragment, the right and bottom side edge fragment have to encompass the
				//remaining image space, and may have to expand/shrink by a maximum of half its side length
				Mat fragment;
				//right edge but not bottom
				if(fragmentCount%numberOfFragmentsPerRow==0 && fragmentCount+numberOfFragmentsPerRow<fragments){
				    System.out.println("R");
					fragment=Main.curFrame.submat(y, y+fragmentSide, x, Main.curFrame.width());
				}
				//bottom right edge
				else if(fragmentCount%numberOfFragmentsPerRow==0 && fragmentCount+numberOfFragmentsPerRow>=fragments){
				    System.out.println("BR");
                    fragment=Main.curFrame.submat(y, Main.curFrame.height(), x, Main.curFrame.width());
				}else{
				    System.out.println("normal");
					fragment=Main.curFrame.submat(y, y+fragmentSide, x, x+fragmentSide);
				}
				Callable<Pair<int[],Integer>> thread=new MassDetectionandObjectPriority(y,x,fragment);
				tasks.add(executor.submit(thread));
				fragmentCount++;
				
				x+=fragmentSide;
			}
			y+=fragmentSide;
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
