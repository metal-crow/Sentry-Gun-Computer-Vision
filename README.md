Sentry-Gun-Computer-Vision
==========================

Major project for CSH. Detects people for use in a sentry gun.

Uses OpenCV 2.4.9 and JavaTuples.
I have a partial C++ port written, but i am unsure if i will finish porting this.

ToDo (in order)
==========================
1. Build the full hardware
2. Add object permanence, memory for objects   
3. Handle turret not shooting at its own bullet  
4. Make it faster per frame (currently 1 order of magnitude too slow for 60fops)  
5. Work on improving person detection algorithms  
6. Add controls and gui for the user  

Design
==========================
This program reads a frame from the input source, storing a buffer of 3 frames at any time.  
The first thing it does is check too see if a green laser pointer dot is present in the frame, as i intend for that to be a manual targeting system.  
To find if the dot is present, i check for any pixels in a range on that frame, resolving a binary image that has those pixels, and pass this frame to the method:BlobDetection.  
If we find a point in the image that is the laser, we stop checking for anything else in the image, and send the point coordinates to the arduino through method:ArduinoInteraction.  
if not, we move to movement detection. Using Differential Images (http://blog.cedric.ws/opencv-simple-motion-detection) i find areas of movement between the 3 buffered frames, resolving a binary mat with the movement in white. This mat is again passed to method:BlobDetection.  
If we find that this movement is a person, we stop checking for anything else in the image, and send the point coordinates of the center of mass to the arduino through method:ArduinoInteraction.  
Finally, if we still have not found any targets, we perform an at rest person detection.  
The frame is split into a number of smaller segments, and each one is passed to method:MassDetectionandObjectPriority, using its secondary non-blob based person detection.  
We again check if we have gotten any targets from this, and if so send the center of the fragment (center of mass is not computed here) to method:ArduinoInteraction.  
We then grab the next frame and advance the buffer.  
  
method:BlobDetection{  
I pass a binary mat frame into this blob detection method, which goes through the image and, upon finding a pixel thats marked as in range (white in the binary mat), it uses floodfill to find the blob that that pixel is a part of, and sends that frame, and the blob in it, to a thread method:MassDetectionandObjectPriority. 
By using floodfill, i also mark this entire blob as read, and continue going through the mat looking for other pixels while ignoring the blobs i have already sent to a thread.  
After reading the entire mat for blobs, i get the results back from the threads, and return an array of the centers of mass of all the blobs, and their likelihood of being valid.  
}  
  
method:MassDetectionandObjectPriority{  
This thread receives a mat containing a blob. From her the thread can diverge to two possible cases.  
In most cases this mat will be accompanied by the color the blob has been floodfilled too. In this case, the blob's center of mass is computer by a 4-corners recursive method.  
Next, depending on what type of blob we are checking for, the thread will run either a check for circularity and density if a laser point is being looked for, or will check if this blob is likely to be a person (see Person Recognition section).  
If the color of the blob is not received, this thread instead only checks for if it can find a person in the image.  
Finally, the point of the center of mass of the blob and the likelihood this blob is what is being looked for is returned.    
}  
  
method:ArduinoInteraction{  
After we establish a connection to the arduino through serial on a com port, this method is called with the onscreen coordinates to point the servo's at.  
I compress the screen coordinates to the angle range the camera is capable of viewing, and send these values over serial to the method:ArduinoCode, in addition to the command indicating these are movement values.  
}  
  
method:ArduinoCode{  
The arduino is continually listening for values over serial, and when it received any data, it checks for a header byte which indicated if it should move the firing servo or the x and y movement servos.  
Depending on the header, it then reads the next n bytes of data, and either moves the servo by that amount, or moves the firing servo 90 degrees and back to 0 degrees.  
}  

Design of Person Recognition
==========================
To check for if the given blob of image contains a person, i currently perform a 3 fold check to compute a numerical likelihood of a person. This likelihood is calculated in respect to frame size in order to be resolution independent and to allow for one check to always return a higher priority than another check (for example, a face will always have a greater likelihood than any amount of found skin).  
Firstly, i use a cascade classifier to check for a face in either frontal or profile view. This, obviously, has the most impact on the likelihood this is a person. If a face is found, the likelihood is returned as double the total area of the frame.  
Secondly, i check for any color values in the range of human skin. If a non-insignificant amount is found (width*3 at min), the likelihood is returned as the total frame area + the area of found skin.  
Finally, if nothing else is found, the area of the blob or the fragment is returned.   