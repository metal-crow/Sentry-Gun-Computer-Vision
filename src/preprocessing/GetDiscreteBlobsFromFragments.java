package preprocessing;

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
import org.opencv.imgproc.Imgproc;

import processing.MassDetectionandObjectPriority;

public class GetDiscreteBlobsFromFragments implements Callable<ArrayList<Pair<int[], Integer>>> {
	
	private Mat fragment;
	private int offsetx;
	private int offsety;
	private int identification;
	private Mat fullImg;//this is used to syncronize threads on
	//this is needed because massdetec thread relies on unique colors to differentiate blobs. Poses problem if two threads color in different blobs same color
    private int startingcolor;
    //this is needed because we need to ensure that we dont confuse artifacting and an actual laser dot
    private static final int minSize=36;
	
	public GetDiscreteBlobsFromFragments(Mat fragment,int offsety, int offsetx, int identification, Mat fullImg, int startingcolor) {
		this.fragment=fragment;
		this.offsetx = offsetx;
		this.offsety=offsety;
		this.identification = identification;
		this.fullImg = fullImg;
        this.startingcolor = startingcolor;
	}

	@Override
	public ArrayList<Pair<int[], Integer>> call() throws Exception {
		ExecutorService executor = Executors.newCachedThreadPool();
	    ArrayList<Future<Pair<int[],Integer>>> tasks = new ArrayList<Future<Pair<int[],Integer>>>();

		//go through the fragment and find the blobs in it
		for(int y=0;y<fragment.height();y++){
			for(int x=0;x<fragment.width();x++){
				//if we find a white pixel
				if(fragment.get(y,x)[0]==255){
				    int size=0;
					//floodfill it, and give the fragment and the blobs color to a massdet thread
					synchronized (fullImg) {
						size=Imgproc.floodFill(fragment, new Mat(), new Point(x,y), new Scalar(startingcolor));
					}
					if(size>=minSize){
					    Callable<Pair<int[],Integer>> thread =new MassDetectionandObjectPriority(fragment,startingcolor,identification);
					    tasks.add(executor.submit(thread));
					    startingcolor++;
					}
					//if its too small, wipe this dot to save colors. This DOES make it slower, but saves our limited color range
					else{
					    synchronized (fullImg) {
					        Imgproc.floodFill(fragment, new Mat(), new Point(x,y), new Scalar(0));
					    }
					}
				}
			}
		}
		
		//when we get the threads back, make sure to add the fragment offsets.
        executor.shutdown();
        ArrayList<Pair<int[], Integer>> targets=new ArrayList<Pair<int[], Integer>>(tasks.size());
        for(Future<Pair<int[],Integer>> task:tasks){
            try {
            		Pair<int[], Integer> storage=task.get();
            		int[] posit=storage.getValue0();
            		posit[0]+=offsetx;
            		posit[1]+=offsety;
                    targets.add(storage);
            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        return targets;
	}

}
