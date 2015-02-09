package postprocessing;


public class Memory {

    /**
     * Commit the given target to memory (including no target, stored as coord -1,-1). left shift all previous memorized objects.
     * @param target
     * @param objectPermanence 
     */
    public static void commitObjectToMemory(int[] target, int[][] objectPermanence) {
        //leftshift old memory objects
        for(int i=1;i<objectPermanence.length-1;i++){
            objectPermanence[i-1]=objectPermanence[i];
        }
        //add new memory object
        objectPermanence[objectPermanence.length-1]=target;
    }
}
