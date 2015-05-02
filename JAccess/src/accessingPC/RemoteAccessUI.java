package accessingPC;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.util.LinkedList;

import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingWorker;

import tests.DP;
import utils.Chunk;
import utils.DataUtils;

public class RemoteAccessUI extends JPanel {

	private int screenWidth;
	private int screenHeight;
	private LinkedList<Chunk> newChunks = new LinkedList<Chunk>();
	private final int UPDATE_COUNT = 1;
	
	public RemoteAccessUI(int width, int height, MouseListener ml)
	{
		this(new Rectangle(width, height), ml);
	}
	
	public RemoteAccessUI(Rectangle screenSize, MouseListener ml) {
		screenWidth = screenSize.width;
		screenWidth = screenSize.height;
		
		this.setPreferredSize(new Dimension(screenSize.width, screenSize.height));
		
		this.addMouseListener(ml);
		
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
		
		String address = args.length > 0 ? args[0] : JOptionPane.showInputDialog("Remote IP: ");
		
		if(address == null)
			System.exit(0);
		
		String password = DP.askForPassword();
		
		if(password == null)
			password = "";
		
		Socket socket;
		try {
			socket = new Socket(address, DataUtils.HANDSHAKE_PORT);
			try{
				ObjectOutputStream stream = new ObjectOutputStream(socket.getOutputStream());
				
				stream.writeObject("connect" + password.hashCode());
				socket.close();
	
			}catch(Exception e){
				socket.close();
				throw e;
			}
		} catch (Exception e) {
			DP.popup("Could not connect to remote computer");
			e.printStackTrace();
			System.exit(1);
		}

		
		JFrame frame = new JFrame("Remote Access");
		Rectangle r = new Rectangle(1920, 1080);
		InputBroadcaster in = new InputBroadcaster(address, DataUtils.INPUT_PORT);
		
		JPanel raui = new RemoteAccessUI(r, in);
		
		JScrollPane scroll = new JScrollPane(raui, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		scroll.setWheelScrollingEnabled(false);
		
		frame.add(scroll);
		frame.setBounds(r);
		frame.addKeyListener(in);
		raui.addMouseMotionListener(in);
		frame.addMouseWheelListener(in);
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
	}

	
}
