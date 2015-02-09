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
        //store this starting point
        outline.add(new Point(startingBlob.x+(startingBlob.width/2),startingBlob.y+(startingBlob.height/2)));
        this.blobs = blobs;
        this.img = img;
    }
    
    @Override
    public int[] call(){
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
        fillOutlineOfPoints(outline);
        
        return new int[]{startingBlob.x,startingBlob.y};
    }
    
    /**
     * Given a list of rects(i convert to points), fill the entire interior of the outline of these points.
     * While the current highest point has two points below it,
     * take this highest point and the two lower points and fill them in like a triangle.
     * @param outline
     */
    private void fillOutlineOfPoints(ArrayList<Point> outline){
        do{
            //find current highest point at this time
            Point topPoint = outline.get(0);
            for(Point p:outline){
                if(topPoint.y>p.y){
                    topPoint=p;
                }
            }
            outline.remove(topPoint);
            
            //get the two points below it
            Point[] twoLowerPoints=new Point[]{outline.get(0), outline.get(1)};
            //go through the outline list of points
            for(Point p:outline){
                //when we find a point lower than the top point and we haven't saved it
                if(p.y>topPoint.y && !twoLowerPoints[0].equals(p) && !twoLowerPoints[1].equals(p)){
                    //if its higher than either of our two stored lower points, save it
                    if(twoLowerPoints[0].y>p.y){
                        twoLowerPoints[0]=p;
                    }else if(twoLowerPoints[1].y>p.y){
                        twoLowerPoints[1]=p;
                    }
                }
            }
            
            //fill in the triangle made by these points 
            synchronized (img) {
                Core.fillConvexPoly(img, new MatOfPoint(topPoint,twoLowerPoints[0],twoLowerPoints[1]), new Scalar(255));
            }
        }while(outline.size()>2);
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
                //FIXME simplified distance finder
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
