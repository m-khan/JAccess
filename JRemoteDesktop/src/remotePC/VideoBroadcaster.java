package remotePC;

import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.SwingWorker;

import org.jcodec.codecs.h264.H264Encoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.encode.ConstantRateControl;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.scale.RgbToYuv420;

import tests.DP;
import utils.DataUtils;

public class VideoBroadcaster extends SwingWorker<Void, Void> {

	private Rectangle screenSize;
	private Picture toEncode;
	private RgbToYuv420 transform;
	private H264Encoder encoder;
	private ByteBuffer _out;
	private InetAddress address;
	private int port;
	private DatagramSocket socket;
	private byte[] packetBuff;

	public VideoBroadcaster(Rectangle screenSize, String hostName, int port) {
		super();
		this.screenSize = screenSize;
		
		try {
			address = InetAddress.getByName(hostName);
		} catch (UnknownHostException e1) {
			e1.printStackTrace();
			DP.print("Hostname Unknown");
		}
		
		this.port = port;
		toEncode = null;
		transform = new RgbToYuv420(0,0);
		encoder = new H264Encoder(new ConstantRateControl(512)); //TODO: look into RateControl
		_out = ByteBuffer.allocate(screenSize.width * screenSize.height * 6);
		packetBuff = new byte[DataUtils.PACKET_SIZE];
		
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
		int frameCount = 0;
		
		while(!this.isCancelled())
		{
			Robot rob = new Robot();
			BufferedImage screencap = rob.createScreenCapture(screenSize);
			ByteBuffer toSend = encodeImage(screencap);
			DP.print("Sending frame " + frameCount + ": " + toSend.remaining());
			if(sendFrame(toSend, frameCount++)) DP.print("Frame sent succesfully");
		}
		
		return null;
	}
	
	@Override
	public void done()
	{
		socket.close();
	}
	
	
	public boolean sendFrame(ByteBuffer toSend, int frameID) throws Exception
	{
		//Write frame ID into the packet buffer
		DataUtils.intToBytes(frameID, packetBuff, DataUtils.FRAME_ID);
		List<ByteBuffer> nalUnits = H264Utils.splitFrame(toSend);
		
		
		//Write totalPackets into the packet buffer
		short totalPackets = (short) nalUnits.size();
		DataUtils.shortToBytes(totalPackets, packetBuff, DataUtils.PACKET_COUNT);
		
		//Start packet loop - send packets till the bytebuffer is empty
		short packetIndex = 0;
		for(ByteBuffer b : nalUnits)
		{
			//Write packet number into the buffer
			DataUtils.shortToBytes(packetIndex++, packetBuff, DataUtils.PACKET_ID);
			
			//Write from Frame Buffer to Packet Buffer
			DataUtils.shortToBytes((short) (DataUtils.PACKET_SIZE - DataUtils.DATA_IND), packetBuff, DataUtils.DATA_SIZE);
			toSend.get(packetBuff, DataUtils.DATA_IND, DataUtils.PACKET_SIZE - DataUtils.DATA_IND);
			
			DatagramPacket packet = new DatagramPacket(packetBuff, DataUtils.PACKET_SIZE, address, port);
			socket.send(packet);
			
		}
		
		return true;
	}
	
	
	public ByteBuffer encodeImage(BufferedImage image)
	{
		toEncode = biToYuv420(image);
		
		_out.clear();
		ByteBuffer toReturn = encoder.encodeFrame(_out, toEncode);

		return toReturn;
	}
	
	private Picture biToYuv420(BufferedImage bi)
	{   
	    DataBuffer imgdata = bi.getRaster().getDataBuffer();
	    int[] ypix = new int[imgdata.getSize()];
	    int[] upix = new int[ imgdata.getSize() >> 2 ];
	    int[] vpix = new int[ imgdata.getSize() >> 2 ];
	    int ipx = 0, uvoff = 0;

	    for (int h = 0; h < bi.getHeight(); h++) {
	        for (int w = 0; w < bi.getWidth();  w++) {
	            int elem = imgdata.getElem(ipx);
	            int r = 0x0ff & (elem >>> 16);
	            int g = 0x0ff & (elem >>> 8);
	            int b = 0x0ff & elem;
	            ypix[ipx] = ((66 * r + 129 * g + 25 * b) >> 8) + 16;
	            if ((0 != w % 2) && (0 != h % 2)) {
	                upix[uvoff] = (( -38 * r + -74 * g + 112 * b) >> 8) + 128;
	                vpix[uvoff] = (( 112 * r + -94 * g + -18 * b) >> 8) + 128;
	                uvoff++;
	            }
	            ipx++;
	        }
	    }
	    int[][] pix = { ypix, upix, vpix, null };
	    return new Picture(bi.getWidth(), bi.getHeight(), pix, ColorSpace.YUV420);
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
		
		try {
			vb.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			System.exit(1);
		}
	}

}
