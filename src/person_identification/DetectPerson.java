package person_identification;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.highgui.Highgui;
import org.opencv.objdetect.CascadeClassifier;

public class DetectPerson {

    private static final String FrontalCascadeClassifierFile="E:/Code Workspace/.external libraries/opencv 2.4.9/sources/data/lbpcascades/lbpcascade_frontalface.xml";
    private static final String ProfileCascadeClassifierFile="E:/Code Workspace/.external libraries/opencv 2.4.9/sources/data/lbpcascades/lbpcascade_profileface.xml";
    
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
        
        Mat image = Highgui.imread("testing/circle blob.png");
        CascadeClassifier frontalFaceDetector = new CascadeClassifier(FrontalCascadeClassifierFile);
        
        // Detect faces in the image.
        // MatOfRect is a special container class for Rect.
        MatOfRect faceDetections = new MatOfRect();
        frontalFaceDetector.detectMultiScale(image, faceDetections);

        System.out.println(String.format("Detected %s faces", faceDetections.toArray().length));

        // Draw a bounding box around each face.
        for (Rect rect : faceDetections.toArray()) {
            Core.rectangle(image, new Point(rect.x, rect.y), new Point(rect.x + rect.width, rect.y + rect.height), new Scalar(0, 255, 0));
        }
        
        // Save the visualized detection.
        Highgui.imwrite("testing/faceafter.jpg", image);
    }
}
