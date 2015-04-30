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
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
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
	
	Map<Integer, Chunk> cache = new ConcurrentHashMap<Integer, Chunk>();

	int seedFrameCount = 0;
	int seedFreq = 10;
	
	public VideoBroadcaster(Rectangle screenSize, String hostName, int port) {
		super();
		this.screenSize = screenSize;
		
		try {
			address = InetAddress.getByName(hostName);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			DP.popup("Hostname Unknown: " + hostName);
			System.exit(1);
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
			DP.popup("Failed to setup broadcaster");
			System.exit(1);
		}
	}
	
	public void setAddress(InetAddress newAddress)
	{
		address = newAddress;
	}

	@Override
	protected Void doInBackground() throws Exception {
		
		DP.print("Starting Broadcast to " + address);
		int width = Chunk.WIDTH;
		int height = Chunk.HEIGHT;
		double lastTime = System.nanoTime();
		final int cores = Runtime.getRuntime().availableProcessors();
		
		while(!this.isCancelled())
		{
			
			ExecutorService pool = Executors.newFixedThreadPool(cores);

			BufferedImage screencap = rob.createScreenCapture(screenSize);
			
			for(short x = 0; x < screenSize.width + width; x += width)
			{
				for(short y = 0; y < screenSize.height + height; y += height)
				{
					Chunk c = new Chunk(null, x, y);
					pool.execute(new SenderThread(c, screencap));
				}
			}
			
			pool.shutdown();

			pool.awaitTermination(1, TimeUnit.SECONDS);
			
			//Frame stuff
            double fps = 1000000000.0 / (System.nanoTime() - lastTime); //one second(nano) divided by amount of time it takes for one frame to finish
            lastTime = System.nanoTime();
            
            firePropertyChange("fps", null, fps);
			
            if(seedFrameCount++ == seedFreq)
            {
            	newSeed();
            }
            
		}
		return null;
	}
	
	public void newSeed()
	{
    	cache.clear();
    	seedFrameCount = 0;
	}
	
	class SenderThread implements Runnable
	{
		Chunk c;
		BufferedImage screencap;
		
		private SenderThread(Chunk c, BufferedImage screen)
		{
			this.c = c;
			screencap = screen;
		}
		
		@Override
		public void run() {
			try {
				encodeAndSend();
			} catch (Exception e) {
				DP.err(e);
				// Do nothing, this is lossy anyway
			}
		}
	
		public void encodeAndSend() throws Exception
		{
			
			int width = Chunk.WIDTH;
			int height = Chunk.HEIGHT;
			int imageType = DataUtils.IMAGE_TYPE;

			BufferedImage img = new BufferedImage(width, height, imageType);
			Graphics2D g2 = img.createGraphics();
			g2.drawImage(screencap, 0, 0, width, height, c.x, c.y, c.x+width, c.y+height, null);
			g2.dispose();
			
			c.img = img;
			int key = DataUtils.combine((short)c.x, (short)c.y);

			if(!cache.containsKey(key) || !cache.get(key).equals(c))
			{
				cache.put(key, c);
				ByteBuffer toSend = DataUtils.encode2(c.img);
				sendPacket(toSend, c.x, c.y);
			}
	
		}

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
		
		synchronized(socket)
		{
			socket.send(packet);
		}

		return true;
	}
	
	
	//Simple test broadcaster, recommend running the remote client instead.
	public static void main(String[] args)
	{
		String address = args.length > 0 ? args[0] : JOptionPane.showInputDialog("Broadcast to IP: ");
		
		SwingWorker vb = new VideoBroadcaster(new Rectangle(1920, 1080), address, DataUtils.VIDEO_PORT);
		vb.execute();
		
		JFrame frame = new JFrame();
		frame.getContentPane().add(new JLabel("Broadcasting to " + address));
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		new InputReceiver(address, DataUtils.INPUT_PORT);
		
		try {
			vb.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			DP.popup("Fatal error in broadcaster");
			System.exit(1);
		}
	}
	

}
