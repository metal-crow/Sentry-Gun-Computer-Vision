package preprocessing;

import java.util.ArrayList;
import java.util.concurrent.Callable;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;

public class GenerateBlobFromOutline implements Callable<int[]>{

    private Rect startingBlob;
    private ArrayList<Rect> blobs;
    private Mat img;
    ArrayList<Point> outline=new ArrayList<Point>();
    private static final int minSize=300;

    /**
     * Pick one blob, choose closest blob to it. Connect them. Remove from list of available blobs.
     * Repeat. When we reach a blob that has no other blobs within X range, start at original blob and repeat.
     * When we reach reach a blob that has no other blobs within X range again, connect the last two that were too far from anything else.
     * @param i
     * @param blobs
     * @param img 
     * @return a point in the outline
     */
    public GenerateBlobFromOutline(int i, ArrayList<Rect> blobs, Mat img) {
        this.startingBlob = blobs.get(i);
        //remove starting blob from list
        blobs.remove(i);
        this.blobs = blobs;
        this.img = img;
    }
    
    @Override
    public int[] call(){
        //store this starting point. MUST ensure that is is actually in the blobs whitespace else the method it returns to will find this thread failed.
        boolean exit=false;
        int y=startingBlob.y;
        while(y<startingBlob.y+startingBlob.height && !exit){
            int x=startingBlob.x;
            while(x<startingBlob.x+startingBlob.width && !exit){
                if(img.get(y, x)[0]==255){
                    outline.add(new Point(x,y));
                    exit=true;
                }
                x++;
            }
            y++;
        }
        
        Rect currentBlob=startingBlob;
        Boolean endblob1=false;
        Rect nextblob=getBlobClose(currentBlob);
                
        while(nextblob!=null){
            currentBlob=nextblob;
            //add the newly connected outline blob
            outline.add(new Point(currentBlob.x+(currentBlob.width/2),currentBlob.y+(currentBlob.height/2)));
            //get new next blob
            nextblob=getBlobClose(currentBlob);
            
            //remove the blob from the list
            blobs.remove(currentBlob);
             
            //reached end of one side, save this end
            if(nextblob==null && !endblob1){
                endblob1=true;
                //restart at original blob
                currentBlob=startingBlob;
                nextblob=getBlobClose(currentBlob);
            }
        }
        
        //fill the area we just outlined
        if(outline.size()>2){
            fillOutlineOfPoints(outline);
        }
        //if we didnt find an outline and a single blob ISNT big enough, return null
        else if((startingBlob.width*startingBlob.height)<minSize){
            return null;
        }
        
        //Highgui.imwrite("testing/blob/blob"+Main.frame_count+" "+outline.get(0)+".png", img);//debugging
        
        //if we find an outline or a big enough blob
        return new int[]{(int) outline.get(0).x,(int) outline.get(0).y};
    }
    
    /**
     * Given a list of rects(i convert to points), fill the entire interior of the outline of these points.
     * While the current highest point has two points below it,
     * take this highest point and the two lower points and fill them in like a triangle.
     * @param outline
     */
    private void fillOutlineOfPoints(ArrayList<Point> outline){
        while(outline.size()>2){
            //find highest point
            Point topPoint = outline.get(0);
            for(Point p:outline){
                //if point is above current top point
                if(topPoint.y>p.y){
                    topPoint=p;
                }
            }
            outline.remove(topPoint);
            
            //find second highest point
            Point secondpoint=outline.get(0);
            for(Point p:outline){
                if(secondpoint.y>p.y){
                    secondpoint=p;
                }
            }
            
            //find third highest point
            Point thirdpoint=outline.get(1);
            for(Point p:outline){
                if(thirdpoint.y>p.y && !p.equals(secondpoint)){
                    thirdpoint=p;
                }
            }
            
            //fill in the triangle made by these points 
            synchronized (img) {
                Core.fillConvexPoly(img, new MatOfPoint(topPoint,secondpoint,thirdpoint), new Scalar(255));
            }
        }
    }
    
    /**
     * Get the closes blob at max 100 pixels away
     * @param currentBlob
     * @return the closest blob or null if none are close enough
     */
    private Rect getBlobClose(Rect currentBlob){
        int distance=100;
        int index=-1;
        for(int i=0;i<blobs.size();i++){
            if(!blobs.get(i).equals(currentBlob)){
                //FIXME simplified distance finder.
                int xdistance=Math.abs( (currentBlob.x+(currentBlob.width/2) ) - (blobs.get(i).x+(blobs.get(i).width/2) ));
                int ydistance=Math.abs( (currentBlob.y+(currentBlob.height/2) ) - (blobs.get(i).y+(blobs.get(i).height/2) ));
                int tempdist=(int) Math.hypot(xdistance, ydistance);
                
                if(tempdist<distance){
                    distance=tempdist;
                    index=i;
                }
            }
        }
        if(index==-1){
            return null;
        }else{
            return blobs.get(index);
        }
    }
    

}
