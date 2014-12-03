package main;

import java.util.ArrayList;
import java.util.Arrays;

import javax.swing.JFrame;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;

import user_view.RealTime_Video_Showing;

public class test {

	public static void main(String[] args) throws Exception {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Mat img=Highgui.imread("test images/motiontest.jpg");
    	Imgproc.threshold(img, img, 35, 255, Imgproc.THRESH_BINARY);
        Mat draw=img.clone();
    	Imgproc.cvtColor(img, img, Imgproc.COLOR_BGR2GRAY);

    	//poblem choosing the right corner to go to
    	ArrayList<Pair<int[],Integer>> a= BlobDetection.findBlobs(img,BlobDetection.BASIC_IDENTIFICATION);
    	System.out.println(Arrays.toString(a.get(0).getValue0()));
    	
    	JFrame f = new JFrame();
        f.setSize(img.cols(),img.rows());
        RealTime_Video_Showing panel = new RealTime_Video_Showing(img.cols(),img.rows());
		f.add(panel);
        f.pack();
        f.setVisible(true);
        
        Core.circle(draw, new Point(a.get(0).getValue0()[0],a.get(0).getValue0()[1]), 3, new Scalar(255,0,0), -1);
        panel.setImage(draw);
	}

}
