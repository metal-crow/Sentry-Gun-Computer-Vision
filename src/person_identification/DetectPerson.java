package person_identification;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

public class DetectPerson {

    private static final String FrontalCascadeClassifierFile="E:/Code Workspace/.external libraries/opencv 2.4.9/sources/data/lbpcascades/lbpcascade_frontalface.xml";
    private static final String ProfileCascadeClassifierFile="E:/Code Workspace/.external libraries/opencv 2.4.9/sources/data/lbpcascades/lbpcascade_profileface.xml";
    
    //combined HSB values for various skin colors
    private static final Pair<Scalar,Scalar> skinColors=Pair.with(new Scalar(9, 30, 20), new Scalar(28, 70, 90));

    public static int isBlobHuman(Mat img){
        int blobArea=Core.countNonZero(img);//TODO figure out priority

        //first check if we can find a face in this blob. If so, we have a guaranteed person.
        MatOfRect faceDetections = new MatOfRect();
        //check for a frontally facing face
        CascadeClassifier frontalFaceDetector = new CascadeClassifier(FrontalCascadeClassifierFile);
        frontalFaceDetector.detectMultiScale(img, faceDetections);
        if(faceDetections.toArray().length>0){
            return blobArea;
        }
        //check for a face in profile
        CascadeClassifier profileFaceDetector = new CascadeClassifier(ProfileCascadeClassifierFile);
        profileFaceDetector.detectMultiScale(img, faceDetections);
        if(faceDetections.toArray().length>0){
            return blobArea;
        }
        
        //next check for skin, see if the blob contains skin color
        //find HSV values for all skin colors to check for
        return 0;
    }
    
    public static void main(String[] args) {
        System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
        
        Mat image = Highgui.imread("testing/skin colors.png");
        Imgproc.cvtColor(image, image, Imgproc.COLOR_BGR2HSV);
        
        Core.inRange(image, skinColors.getValue0(), skinColors.getValue1(), image);
                
        //Save the visualized detection.
        Highgui.imwrite("testing/skins.jpg", image);
    }
}
