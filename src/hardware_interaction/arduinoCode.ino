#include <Servo.h>

Servo servoX;
Servo servoY;
Servo servoT;
int command=0;

void setup() {
 Serial.begin(9600);
 servoX.attach(9);
 servoY.attach(8);
 servoT.attach(7);
 servoX.write(0);//back to 0 degrees
 servoY.write(0);//back to 0 degrees
 
 delay(1000);//wait for a second
}

void loop() {
	//get the type of command we will receive to optimize byte reading
	if(Serial.available() && command==0) {
    	command = Serial.read();
    }
    //if we've received a move command and we have 2 bytes to receive, we have our shooting coordinates.
    if(Serial.available()>=2 && command==1){
    	int xmove=Serial.read();
    	int ymove=Serial.read();
	    servoX.write(xmove); 
	    servoY.write(ymove); 
	    //reset the stored command after it is executed
	    command=0;
	}
	//if we have received a shoot command, we dont need any more data
	if(command==2){
		servoT.write(90);
		servoT.write(0);
		command=0;
	}
}
