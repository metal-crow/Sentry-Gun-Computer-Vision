Sentry-Gun-Computer-Vision
==========================

Major project for CSH. Detects people for use in a sentry gun.

Uses OpenCV 2.4.9. If you can get 3 to work with java please tell me how. Also JavaTuples.

ToDo (in order)
==========================
1. Work on person detection algorithms
2. Add at rest person detection
3. Build the full hardware
4. Write up this documentation
5. Add controls and gui for the user

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
TODO 


Design of Person Recognition
==========================
TODO