package accessingPC;

import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.SocketException;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingWorker;

import utils.Chunk;
import utils.DataUtils;

public class RemoteAccessUI extends JPanel {

	private int screenWidth;
	private int screenHeight;
	private LinkedList<Chunk> newChunks = new LinkedList<Chunk>();
	private final int UPDATE_COUNT = 300;
	
	public RemoteAccessUI(int width, int height)
	{
		this(new Rectangle(width, height));
	}
	
	public RemoteAccessUI(Rectangle screenSize) {
		screenWidth = screenSize.width;
		screenWidth = screenSize.height;
		
		try{
		SwingWorker<BufferedImage, BufferedImage> vr = new VideoReceiver(DataUtils.VIDEO_PORT);
		listen(vr);
		vr.execute();
		} catch(SocketException e){
			JOptionPane.showMessageDialog(this, "Error connecting to thing");
			
		}
	}
	
	public void listen(SwingWorker sw)
	{
		sw.addPropertyChangeListener(new ReceiverListener());
	}
	
	public void triggerPaint()
	{
		this.repaint();
	}
	
	@Override
	public void paintComponent(Graphics g)
	{
		//System.out.println("Painting " + newChunks.size() + " chunks");
		
		while(!newChunks.isEmpty())
		{
			Chunk toDraw = newChunks.pop();
			g.drawImage(toDraw.img, toDraw.x, toDraw.y, null);
		}
	}
	
	@Override
	public void update(Graphics g)
	{
		//does nothing
	}
	
	public class ReceiverListener implements PropertyChangeListener
	{
		private int count = 0;
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
			if("chunk".equals(evt.getPropertyName()))
			{
				Chunk chunk = (Chunk) evt.getNewValue();
				if(chunk != null)
				{	
					newChunks.add(chunk);
					count++;
					if(count >= UPDATE_COUNT)
					{
						triggerPaint();
						count = 0;
					}
				}
			}
		}
	}

	//Simple test main
	public static void main(String[] args) {
		JFrame frame = new JFrame("TEST");
		Rectangle r = new Rectangle(1920, 1080);
		
		frame.add(new RemoteAccessUI(r));
		frame.setBounds(r);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	
}