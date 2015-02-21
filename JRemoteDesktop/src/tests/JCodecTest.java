package tests;

import java.awt.Image;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;

import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;


public class JCodecTest {

	
	public static void main(String args[])
	{
		try {
			Robot rob = new Robot();
			BufferedImage screencap = rob.createScreenCapture(new Rectangle(1920, 1080));
			
			screencap = (BufferedImage) screencap.getScaledInstance(1400, 900, Image.SCALE_FAST);
			
			JFrame frame = new JFrame();
			frame.getContentPane().add(new JLabel(new ImageIcon(screencap)));
			frame.pack();
			frame.setVisible(true);
			
		
		
		
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	
	
	
}
