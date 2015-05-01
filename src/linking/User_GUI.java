package linking;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.imageio.ImageIO;
import javax.swing.JPanel;

import main.Main;

import org.javatuples.Pair;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.highgui.Highgui;


@SuppressWarnings("serial")
public class User_GUI extends JPanel implements MouseListener{
	
	private BufferedImage BMimg;
	private int width;
	private int height;
	
	public User_GUI(int width, int height){
		this.width=width;
		this.height=height;
		BMimg=new BufferedImage(width, height, 1);
        addMouseListener(this);
	}
	
	public Dimension getPreferredSize() {
        return new Dimension(width,height);
    }

	public void setImage(Mat img) throws IOException{
		MatOfByte bytemat = new MatOfByte();

		Highgui.imencode(".jpg", img, bytemat);

		byte[] bytes = bytemat.toArray();

		InputStream in = new ByteArrayInputStream(bytes);

		BMimg = ImageIO.read(in);
		repaint();
	}

	public void paintComponent(Graphics g){  
		try{
			g.drawImage(BMimg, 0, 0, BMimg.getWidth(), BMimg.getHeight(), this);  
		}catch(Exception e){
			System.err.println("Could not draw");
		}
	}

    @Override
    public void mouseClicked(MouseEvent m) {
        Main.target.GUIsetTarget(true, Pair.with(new int[]{m.getX(),m.getY()},7));
    }

    @Override
    public void mouseEntered(MouseEvent arg0) {
    }

    @Override
    public void mouseExited(MouseEvent arg0) {
    }

    @Override
    public void mousePressed(MouseEvent arg0) {
    }

    @Override
    public void mouseReleased(MouseEvent arg0) {
    } 
}
