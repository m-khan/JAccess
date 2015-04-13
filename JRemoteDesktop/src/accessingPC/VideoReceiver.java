package accessingPC;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;

import javax.imageio.ImageIO;
import javax.swing.SwingWorker;

import utils.Chunk;
import utils.DataUtils;

public class VideoReceiver extends SwingWorker<BufferedImage, BufferedImage> {

	private DatagramSocket socket;
	private byte[] packetBuffer;
	
	private BufferedImage currentScreen;
	
	public VideoReceiver(int port) throws SocketException
	{
		super();
		
		socket = new DatagramSocket(port);
		socket.setReceiveBufferSize(131072);
		packetBuffer = new byte[65535];
	}
	
	@Override
	protected BufferedImage doInBackground() throws Exception {

		DatagramPacket packet = new DatagramPacket(packetBuffer, 65535);
		
		while(!this.isCancelled())
		{
			socket.receive(packet);
			int frameX = DataUtils.bytesToInt(packetBuffer, DataUtils.FRAME_X);
			int frameY = DataUtils.bytesToInt(packetBuffer, DataUtils.FRAME_Y);
			short packetSize = DataUtils.bytesToShort(packetBuffer, DataUtils.DATA_SIZE);
			FramePacket pack = new FramePacket(frameX, frameY, packetBuffer, DataUtils.DATA_IND, packetSize);
			sendImage(pack);
 
		}
		
		return null;
	}

	private void sendImage(FramePacket pack)
	{
		BufferedImage img = DataUtils.decode2(pack.getData());
		Chunk c = new Chunk(img, pack.x, pack.y);
		
		//Fires event with image and position
		this.firePropertyChange("chunk", null, c);
	}
	
	class FramePacket
	{
		//immutable object to hold individual packet data
		final int x;
		final int y;
		private byte[] data;
		private FramePacket(int x, int y, byte[] data, int dataOffset, int dataLength) {
			super();
			//we need dataLength because the packetBuffer might have trailing garbage
			this.x = x;
			this.y = y;
			this.data = new byte[dataLength];
			System.arraycopy(data, dataOffset, this.data, 0, dataLength);
		}
		
		public byte[] getData()
		{
			return data;
		}
	}
}
