package hardware_interaction;

import gnu.io.CommPortIdentifier;
import gnu.io.NoSuchPortException;
import gnu.io.PortInUseException;
import gnu.io.SerialPort;
import gnu.io.UnsupportedCommOperationException;

import java.io.IOException;
import java.io.OutputStream;

/**
 * I'm connecting to the Arduino through RXTX serial port, and sending bytes. The program on the Arduino receives these bytes and interprets them.
 */
public class ArduinoInteraction {

	private SerialPort serialPort;
	private OutputStream arduinoOut;
	private static final int cameraFOV=90;

    private static final int TIME_OUT = 1000; // Port open timeout
    private static final int DATA_RATE = 9600; // Arduino serial port

    public ArduinoInteraction() throws UnsupportedCommOperationException, PortInUseException, NoSuchPortException, InterruptedException, IOException {
    	serialPort = (SerialPort)CommPortIdentifier.getPortIdentifier("COM3").open("", TIME_OUT);
        
        // set port parameters
        serialPort.setSerialPortParams(DATA_RATE,
                        SerialPort.DATABITS_8,
                        SerialPort.STOPBITS_1,
                        SerialPort.PARITY_NONE);

        // Give the Arduino some time
        Thread.sleep(2000);
        
        arduinoOut=serialPort.getOutputStream();
    }
    
    /**
     * Send the servo(s) the x and y angle converted from the screen coordinated
     * @param area the area we detected we should shoot. The middle of this are the used coordinates
     * @throws IOException
     */
    public void arduinoScreenPositiontoAngle(int[] point, int imgWidth, int imgHeight) throws IOException{
        //write that this command is to move the servos
        arduinoOut.write(1);
        
    	//I get distortion at screen edges. This is dependent on fov of camera. Need this to recalculate.
    	//convert the screen positions to an angle from 0 to cameraFOV for correct angle
        
    	int screenXPos=(point[0]*cameraFOV)/imgWidth;
    	//since screen is flipped, reverse
    	screenXPos=Math.abs(screenXPos-cameraFOV);
		//System.out.println("servo X"+screenXPos);
		arduinoOut.write(new Integer(screenXPos).byteValue());
		
    	int screenYPos=(point[1]*cameraFOV)/imgHeight;
    	screenYPos=Math.abs(screenYPos-cameraFOV);
		//System.out.println("servo Y"+screenYPos);
		arduinoOut.write(new Integer(screenYPos).byteValue());
    }
    
    /**
     * Trigger the gun trigger
     * @throws IOException 
     */
    public void fire() throws IOException{
    	arduinoOut.write(2);
    }
    
    public void flush(){
    	try {
            arduinoOut.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void close(){
    	serialPort.close();
    }
}