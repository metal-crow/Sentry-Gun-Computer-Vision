package postprocessing;

import org.opencv.core.Mat;

public class MovementSmoothing {

    /**
     * Using memory, smooth the path from the the previous target to this current one so that it doesnt jitter all over.
     * Change the target location from the passed reference
     * @param objectPermanence
     * @param movementBinaryMat 
     * @param target
     */
    public static void smoothTarget(int[][] objectPermanence, int[] target, Mat movementBinaryMat) {
        //i could do it by finding the next best blob closest to the previous point, but leads to logical rut
        //or could move the target inside the blob closer to the edge, but might make shot miss
    }

    
}
