#include <Servo.h>

Servo servoX;
Servo servoY;

void setup() {
 Serial.begin(9600);
 servoX.attach(9);
 servoY.attach(8);
 servoX.write(0);//back to 0 degrees
 servoY.write(0);//back to 0 degrees
 
 delay(1000);//wait for a second
}

void loop() {
 if(Serial.available() >= 3) {
    int command = Serial.read();
    if(command==0){
    	int xmove=Serial.read();
    	int ymove=Serial.read();
	    servoX.write(xmove); 
	    servoY.write(ymove); 
	}
	Serial.flush();
 }
}
