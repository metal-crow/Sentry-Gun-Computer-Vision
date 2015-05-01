package main;

import org.javatuples.Pair;

/**
 * Used to wrap the target coordinates and the target found status.
 * Also to control various access permissions (user setting this cannot be overwritten) 
 */
public class Target {

    private static boolean foundTarget=false;
    private static Pair<int[], Integer> targetdata=Pair.with(new int[]{-1,-1}, 7);//the min priority for the laser;
    private static boolean locked=false;//permissions. User gui can set this, and main thread cannot overwrite
    
    public void GUIsetTarget(boolean found,Pair<int[], Integer> newtargetdata){
        foundTarget=found;
        targetdata=newtargetdata;
        locked=true;
    }
    
    public void setTarget(boolean found,Pair<int[], Integer> newtargetdata){
        if(!locked){
            foundTarget=found;
            targetdata=newtargetdata;
        }
    }
    
    public int priority(){
        return targetdata.getValue1();
    }
    
    public int[] coords(){
        return targetdata.getValue0();
    }
    
    /**reset coord and lock status*/
    public void reset() {
        foundTarget=false;
        targetdata=Pair.with(new int[]{-1,-1}, 7);//the min priority for the laser
        locked=false;
    }
    
    public boolean foundTarget(){
        return foundTarget;
    }
}
