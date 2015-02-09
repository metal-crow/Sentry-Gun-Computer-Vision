package preprocessing;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.javatuples.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import processing.MassDetectionandObjectPriority;

public class ImagePartitioning {
	
    //method options for determining priority of blobs. Used in method.
    public static final int
        BASIC_IDENTIFICATION=0,
        LASER_IDENTIFICATION=1,
        PERSON_IDENTIFICATION=2;
    
    private static final int minBlobArea=35;
    
    /**
     * Get array of all blobs in image. Blob will be represented by bounding box.
     * Give every blob to a thread which will generate an outline with it an nearby blobs
     * THE MOVEMENT MAKES AN OUTLINE. find outline, fill in, thats a person.
     * @return 
     */
	public static ArrayList<Pair<int[], Integer>> OutlineBlobDetection(Mat img, int identification){
	    //first we have to get the blobs in an array as bounding box forms.
        ArrayList<Rect> blobs=new ArrayList<Rect>();
	    List<MatOfPoint> contours = new ArrayList<MatOfPoint>();
	    Imgproc.findContours(img.clone(), contours, new Mat(), Imgproc.RETR_EXTERNAL , Imgproc.CHAIN_APPROX_NONE);
	    for(MatOfPoint points:contours){
	        Rect blobbound=Imgproc.boundingRect(points);
	        if(blobbound.width*blobbound.height>minBlobArea){
	            blobs.add(blobbound);
	        }
	    }
	    
	    ExecutorService executor = Executors.newFixedThreadPool(blobs.size());
	    ArrayList<Future<int[]>> tasks = new ArrayList<Future<int[]>>(blobs.size());
	    
	    //pass each starting blob and the rest of the blobs to a thread. 1 thread for every starting blob and potential outline
	    for(int i=0;i<blobs.size();i++){
	        @SuppressWarnings("unchecked")
            GenerateBlobFromOutline t=new GenerateBlobFromOutline(i, (ArrayList<Rect>)blobs.clone(),img);
	        tasks.add(executor.submit(t));
	    }
	    
	    ArrayList<int[]> blobsFromOutlinespreDup = new ArrayList<int[]>();
	    
        //join the threads
	    executor.shutdown();
	    for(Future<int[]> t:tasks){
	        try {
	            blobsFromOutlinespreDup.add(t.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
	    }
	    
	    //get rid of duplicate blob outlines (have to do this after all threads are finished to prevent thread collisions)
	    ArrayList<int[]> blobsFromOutlines = new ArrayList<int[]>();
	    int color=7;//FIXME only works at this color and above. Probably important to find out why.
	    for(int[] startPointforOutlineBlob:blobsFromOutlinespreDup){
            //check to make sure this point is in white space
            if(img.get(startPointforOutlineBlob[1], startPointforOutlineBlob[0])[0]==255){
                //avoid duplicates by floodfilling this blob so that other points in the same blob wont be in white space
                Imgproc.floodFill(img, new Mat(), new Point(startPointforOutlineBlob[0],startPointforOutlineBlob[1]), new Scalar(color));
                color++;
                blobsFromOutlines.add(startPointforOutlineBlob);
            }
	    }
	    
	    //pass each blob location and the mat to MassDetectionandObjectPriority thread 
	    ExecutorService executorMSOPP = Executors.newFixedThreadPool(blobsFromOutlines.size());
	    ArrayList<Future<Pair<int[],Integer>>> tasksMSOP = new ArrayList<Future<Pair<int[],Integer>>>(blobsFromOutlines.size());
	    
	    for(int[] point:blobsFromOutlines){
	        Callable<Pair<int[],Integer>> thread=new MassDetectionandObjectPriority(img, (int)img.get(point[1], point[0])[0], identification);
	        tasksMSOP.add(executorMSOPP.submit(thread));
	    }
	    
	    //get the results of the threads
	    executorMSOPP.shutdown();
	    ArrayList<Pair<int[], Integer>> targets=new ArrayList<Pair<int[], Integer>>(tasksMSOP.size());
	    for(Future<Pair<int[],Integer>> task:tasksMSOP){
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
	 * Basic discrete blob detection. Fragment the image and give each fragment to a thread.
	 * The thread will go through the image, when we find a white pixel floodfill the blob, and give it to a massdetection thread.
	 * Yes i know this is a lot of code repeat from FragmentationSplitting, this is easier than figuring out how to combine the two methods.
	 * @param img
	 * @param identification
	 * @return
	 */
	public static ArrayList<Pair<int[], Integer>> BasicBlobDetection(Mat img, int fragments, int identification){
		ExecutorService executor = Executors.newFixedThreadPool((int) Math.pow(fragments,2));
	    ArrayList<Future<ArrayList<Pair<int[],Integer>>>> tasks = new ArrayList<Future<ArrayList<Pair<int[],Integer>>>>((int) Math.pow(fragments,2));
	    
	    //truncating in worst case looses a pixel for each edge fragment
        int fragmentWidth=(img.width()-1)/fragments;
        int fragmentHeight=(img.height()-1)/fragments;
        //need to give each thread a unique starting color
        int colorincrement=256/(((int) Math.pow(fragments,2))-1);
        int curcolor=1;
        
        int y=0;
        while(y+fragmentHeight<img.height()){
            int x=0;
            while(x+fragmentWidth<img.width()){
                //>OpenCV   >Rows are actually collums
                Mat fragment=img.submat(y, y+fragmentHeight, x, x+fragmentWidth);
                Callable<ArrayList<Pair<int[],Integer>>> thread =new GetDiscreteBlobsFromFragments(fragment,y,x,identification,img,curcolor);
                tasks.add(executor.submit(thread));
                x+=fragmentWidth;
                curcolor+=colorincrement;
            }
            y+=fragmentHeight;
        }
        
        executor.shutdown();
        ArrayList<Pair<int[], Integer>> targets=new ArrayList<Pair<int[], Integer>>(tasks.size());
        for(Future<ArrayList<Pair<int[],Integer>>> task:tasks){
            try {
                    targets.addAll(task.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return targets;
	}
	
	/**
	 * Fragment the image, give each fragment to a thread for at rest detection
	 * @param img the current frame (the normal color one)
	 * @param fragments The square root of whatever number of fragments you want.
	 * @return 
	 */
	public static ArrayList<Pair<int[], Integer>> FragmentationSplitting(Mat img, int fragments){
	    ExecutorService executor = Executors.newFixedThreadPool((int) Math.pow(fragments,2));
	    ArrayList<Future<Pair<int[],Integer>>> tasks = new ArrayList<Future<Pair<int[],Integer>>>((int) Math.pow(fragments,2));
	    
	    //truncating in worst case looses a pixel for each edge fragment
        int fragmentWidth=(img.width()-1)/fragments;
        int fragmentHeight=(img.height()-1)/fragments;
        
        int y=0;
        while(y+fragmentHeight<img.height()){
            int x=0;
            while(x+fragmentWidth<img.width()){
                //>OpenCV   >Rows are actually collums
                Mat fragment=img.submat(y, y+fragmentHeight, x, x+fragmentWidth);
                Callable<Pair<int[],Integer>> thread =new MassDetectionandObjectPriority(y,x,fragment);
                tasks.add(executor.submit(thread));
                x+=fragmentWidth;
            }
            y+=fragmentHeight;
        }
        
        executor.shutdown();
        ArrayList<Pair<int[], Integer>> targets=new ArrayList<Pair<int[], Integer>>(tasks.size());
        for(Future<Pair<int[],Integer>> task:tasks){
            try {
                    targets.add(task.get());
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return targets;
	}

}
