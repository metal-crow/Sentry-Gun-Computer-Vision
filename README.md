Sentry-Gun-Computer-Vision
==========================

Major project for CSH. Detects people for use in a sentry gun.

Uses OpenCV 2.4.9. If you can get 3 to work with java please tell me how. Also JavaTuples.

Notes
==========================
For right now, i've given up with actually detecting "people" with this gun. It is EXTREMLY difficult, and I'm not going to make any breakthroughs soon.
Additonally, all sentry guns on the internet i've looked up primarily use motion tracking as their sole method of identification, and they work pretty well.
For right now, I'm going to stop working on person identification and just use my written motion detection and laser detection code to identify targets.
I'm going to start work on the actual Hardware of the project now.

Hardware
==========================
I've tried stepper motors. It has ended poorly. Too slow, weird responsivness, not actually turning to the angle i send to them, partial steps, bad jittering and jerky movement, being all around more complicated, error prone, and just not working.
I might just have not used them correctly, but a)ive spent enough time on them b)the internet suggests using servos as they are better for high speed high torque actions (i.e the quick, smooth, continual rotations for a sentry gun).

I'm going back to using servos, as suggested by http://projectsentrygun.rudolphlabs.com/make-your-own/servos


Design
==========================
My algorithm is based on the principle that the simplest face is a collection of circles.  
Humans recognize a very simple smiley face (a circle with two "eyes") as a face, despite it being only 3 circles.  
Cascade Classifiers go against this "simplicity" approach, by taking a very precise average of the face, and checking for that average.  
In this way they are very limited, and cannot account for face rotation, distortion, perspective, etc, much less deformed faces.  
  
My algorithm relies on the simplest principle that a face is a rough collection of circles, where one large circle encloses smaller ones.  
This offers a great benefit over Cascade Classifiers for a dynamic environment in finding faces, as circles are very easy to detect and my algorithm doesn’t rely on a single check for faceness.  
However, it poses a problem of a wide range of false positives and negatives, as i now have many, many circles to sift through, and circle generation is not an exact science.  
This can be mitigated somewhat by careful picking of the variables used to determine density of a collection of circles makes a face (there are also other variables, such as the threshold for finding edges in CannyEdges and the breakpoint for ovalness of a circle after which it is assumed a line, but those are subvariables).  
For example, with a basic smiley face, <3 circles makes the face no longer exist, and >3 means that its probably a random collection of circles or noise (a tree with all its leaves, for example). Same principle applies to real life faces, but these variables are much harder to determine.  

Size scaling, where faces are further back or of different sizes, also become a problem with this approach.  
While not fully scalable, my idea was to fragment the image into smaller images, where each thread was given an increasing number of increasingly small fragments of the frame.  
Since my circle finding algorithm finds circles based on their size in relationship to the image, Running my algorithm on each of these fragment could then allow me to effectively crop the image, and my circle generation will find more, smaller circles, in each smaller image. From this I can check for faces the same way I do with other images.  
This is limited to the number of threads a computer has, and thus isnt scalable, but it does not need to scale indefinite, as faces very small or very far back are not likely wanted faces in any case.  
  
An overview of my design for face detection only (ignoring my additions of movement detection and laser pointer detection), is to   
1. Take n threads, split the frame into n*k evenly sized fragments for each thread, and detect circles in each fragment. (See my circle detection description later). I return all these circles in an an array.  
2. I then find all the circles that enclose other circles, and have a density between two values. These values are currently hardcoded after finding them through testing, but I am not sure if they are optimal values or would be better dynamically calculated.  
3. Finally, for each of the enclosing circles, I run some small tests to eliminate bad values. Color detection, interior circle location tests, etc.  
4. I now have an area of the image likely to be a face.  


Circle Detection
==========================
I didnt like/was to stupid to use OpenCV's HoughCircles, so I created my own.  
1. Run the image through CannyEdges to get the edges.  
2. Convert the edges from a mat into each distinct line is an array of points.  
3. Take each of these lines and find of anywhere on them there is a long stretch of straight line or a peaking pixel. Split the line at these point. Return all the lines that are long enough.
4. Go though all the lines and eliminate the ones that are near-duplicates.  
5. Each of these lines is a quarter-circle. Take the center of this quarter-circle.  
6. Check if the radius is too large.  
7. Find angle of the curve to eliminate very shallow angles that are not circles. Take curved line, and basically treat curve as hypotenuse of triangle. This should approximate angle well enough.  
8. If the angle is within params, add to array, storing circle as [center x,center y,radius]  
  
This works pretty well, and when used in conjunction with spliting the image into small and large fragments, circle finding accuracy is quite good.  