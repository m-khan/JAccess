package accessingPC;

import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.util.concurrent.ExecutionException;

import javax.swing.SwingWorker;

import tests.DP;
import utils.DataUtils;

public class VideoReceiver extends SwingWorker<BufferedImage, BufferedImage> {

	private DatagramSocket socket;
	private byte[] packetBuffer;
	
	public VideoReceiver(int port)
	{
		super();
		
		try {
			socket = new DatagramSocket(port);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		
		packetBuffer = new byte[DataUtils.PACKET_SIZE];
		
	}
	
	@Override
	protected BufferedImage doInBackground() throws Exception {

		DatagramPacket packet = new DatagramPacket(packetBuffer, DataUtils.PACKET_SIZE);

		//Start thread loop
		while(!this.isCancelled())
		{
			DP.print("Waiting for packet...");
			socket.receive(packet);
			DP.print("Received " + packet.getLength() + " byte packet from " + packet.getAddress().getHostAddress());
			int frameNumber = DataUtils.bytesToInt(packetBuffer, DataUtils.FRAME_ID);
			short packetNumber = DataUtils.bytesToShort(packetBuffer, DataUtils.PACKET_ID);
			short packetSize = DataUtils.bytesToShort(packetBuffer, DataUtils.DATA_SIZE);
			DP.print("Frame Number: " + frameNumber + " Packet: " + packetNumber + " " + packetSize + " bytes of data");
		}
		
		
		return null;
	}

	
	
	public static void main(String[] args) {
		SwingWorker<BufferedImage, BufferedImage> vr = new VideoReceiver(DataUtils.VIDEO_PORT);
		vr.execute();
		
		try {
			vr.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			System.exit(1);
		}
	}
}
