package person_identification;

import main.Main;

import org.javatuples.Pair;
import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Scalar;
import org.opencv.objdetect.CascadeClassifier;

public class DetectPerson {

	//TODO get rid of this hardcoding
    private static final String FrontalCascadeClassifierFile="E:/Code Workspace/.external libraries/opencv 2.4.9/sources/data/lbpcascades/lbpcascade_frontalface.xml";
    private static final String ProfileCascadeClassifierFile="E:/Code Workspace/.external libraries/opencv 2.4.9/sources/data/lbpcascades/lbpcascade_profileface.xml";
    
    //combined HSB values for various skin colors
    private static final Pair<Scalar,Scalar> skinColors=Pair.with(new Scalar(4, 76, 51), new Scalar(14, 179, 230));
    

    /*
     * Priority should be 
     * 
     * large blob with face >
     * small blob with face >
     * lots of skin on blob >
     * little skin on blob >
     * large blob no detection >
     * small blob no detection
     */
    public static int isBlobHuman(Mat img){
        int blobArea=img.width()*img.height();
        int frameArea=Main.curFrame.width()*Main.curFrame.height();
        
        //first check if we can find a face in this blob. If so, we have a guaranteed person.
        //the priority must be > than 2 things, so add the maximum 2 times 
        MatOfRect faceDetections = new MatOfRect();
        //check for a frontally facing face
        CascadeClassifier frontalFaceDetector = new CascadeClassifier(FrontalCascadeClassifierFile);
        frontalFaceDetector.detectMultiScale(img, faceDetections);
        if(faceDetections.toArray().length>0){
            System.out.println("Detected face");//DEBUGGING
            return blobArea+(frameArea*2);
        }
        //check for a face in profile
        CascadeClassifier profileFaceDetector = new CascadeClassifier(ProfileCascadeClassifierFile);
        profileFaceDetector.detectMultiScale(img, faceDetections);
        if(faceDetections.toArray().length>0){
            System.out.println("Detected face");//DEBUGGING
            return blobArea+(frameArea*2);
        }
        
        //next check for skin, see if the blob contains skin color
        //this can be falsely positive (peach walls) or falsely negative (weird lighting)
        Mat skin=new Mat();
        Core.inRange(img, skinColors.getValue0(), skinColors.getValue1(), skin);
        //if there is a non-insignificant amount of skin
        int amountOfSkin=Core.countNonZero(skin);
        if(amountOfSkin>img.cols()*3){
            System.out.println("Detected skin");//DEBUGGING
            //the priority must be > than 1 thing, so add the maximum 1 time
            return amountOfSkin+frameArea;
        }
        
        //if we find neither, the size of the blob is the priority
        return blobArea;
    }
    
}
