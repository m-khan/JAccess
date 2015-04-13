package remotePC;

import java.awt.AWTException;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import tests.DP;
import utils.Chunk;
import utils.DataUtils;

public class VideoBroadcaster extends SwingWorker<Void, Void> {

	private Rectangle screenSize;
	private InetAddress address;
	private int port;
	private DatagramSocket socket;
	private byte[] packetBuff;
	Robot rob;

	
	public VideoBroadcaster(Rectangle screenSize, String hostName, int port) {
		super();
		this.screenSize = screenSize;
		
		try {
			address = InetAddress.getByName(hostName);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			DP.print("Hostname Unknown");
		}
		
		try {
			rob  = new Robot();
		} catch (AWTException e1) {
			e1.printStackTrace();
			throw new RuntimeException("Failed to initialize robot");
		}
		
		this.port = port;
		packetBuff = new byte[65535];
		
		try {
			socket = new DatagramSocket();
		} catch (IOException e) {
			e.printStackTrace();
			throw new RuntimeException("Failed to setup broadcaster");
		}
	}
	
	public void setAddress(InetAddress newAddress)
	{
		address = newAddress;
	}

	@Override
	protected Void doInBackground() throws Exception {
		
		DP.print("Starting Broadcast");
		int width = Chunk.WIDTH;
		int height = Chunk.HEIGHT;
		
		while(!this.isCancelled())
		{
			BufferedImage screencap = rob.createScreenCapture(screenSize);
			int imageType = DataUtils.IMAGE_TYPE; //screencap.getType();
			
			ArrayList<Chunk> chunks = new ArrayList<Chunk>();
			
			for(int x = 0; x < screenSize.width + width; x += width)
			{
				for(int y = 0; y < screenSize.height + height; y += height)
				{
					BufferedImage img = new BufferedImage(width, height, imageType);
					Graphics2D g2 = img.createGraphics();
					g2.drawImage(screencap, 0, 0, width, height, x, y, x+width, y+height, null);
					g2.dispose();
					
					Chunk c = new Chunk(img, x, y);
					ByteBuffer toSend = DataUtils.encode2(c.img);
					sendPacket(toSend, c.x, c.y);
				}
			}
			
			//DP.print("Screen sent: " + chunks.size() + " parts");
			
		}
		return null;
	}
	
	@Override
	public void done()
	{
		socket.close();
	}
	
	
	public boolean sendPacket(ByteBuffer toSend, int frameX, int frameY) throws Exception
	{
		int packetSize = toSend.remaining() + DataUtils.DATA_IND;
		
		if(toSend.remaining() > 32767) //max value of short
		{
			DP.print("Packet over 32767 bytes, skipping.  Size: " + toSend.remaining());
			return false;
		}

		// Write positional data to packetBuffer
		DataUtils.intToBytes(frameX, packetBuff, DataUtils.FRAME_X);
		DataUtils.intToBytes(frameY, packetBuff, DataUtils.FRAME_Y);
		
		// Write from Frame Buffer to Packet Buffer
		DataUtils.shortToBytes((short) (toSend.remaining()), packetBuff, DataUtils.DATA_SIZE);
		toSend.get(packetBuff, DataUtils.DATA_IND, toSend.remaining());
		
		DatagramPacket packet = new DatagramPacket(packetBuff, packetSize, address, port);
		socket.send(packet);

		return true;
	}
	
	
	//Simple test main, send target address as parameter
	public static void main(String[] args)
	{
		String address = args.length > 0 ? args[0] : "127.0.0.1";
		
		SwingWorker vb = new VideoBroadcaster(new Rectangle(1920, 1080), address, DataUtils.VIDEO_PORT);
		vb.execute();
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(new JLabel("Broadcaster"));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		new InputReceiver("127.0.0.1", DataUtils.INPUT_PORT);
		
		try {
			vb.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			System.exit(1);
		}
	}
	

}
