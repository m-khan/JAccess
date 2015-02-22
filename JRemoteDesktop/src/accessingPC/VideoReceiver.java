package accessingPC;

import java.awt.image.BufferedImage;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.ByteBuffer;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import javax.swing.SwingWorker;

import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.common.JCodecUtil;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

import tests.DP;
import utils.DataUtils;

public class VideoReceiver extends SwingWorker<BufferedImage, BufferedImage> {

	private DatagramSocket socket;
	private byte[] packetBuffer;
	private ByteBuffer decodeBuff;
	private FrameReconstructor reconstructor;
	private H264Decoder decoder;
	private Picture out;
	private int frameStreak = 0;
	private ExecutorService threadPool = Executors.newFixedThreadPool(1);//Runtime.getRuntime().availableProcessors());
	
	public VideoReceiver(int port)
	{
		super();
		
		try {
			socket = new DatagramSocket(port);
			socket.setReceiveBufferSize(131072);
		} catch (SocketException e) {
			e.printStackTrace();
		}
		packetBuffer = new byte[DataUtils.PACKET_SIZE];
		reconstructor = new FrameReconstructor();
		decodeBuff = ByteBuffer.allocate(1920 * 1080 * 6);
		decoder = new H264Decoder();
		out = Picture.create(1920, 1080, ColorSpace.YUV420);
		
	}
	
	@Override
	protected BufferedImage doInBackground() throws Exception {

		DatagramPacket packet = new DatagramPacket(packetBuffer, DataUtils.PACKET_SIZE);
		
		while(!this.isCancelled())
		{
			socket.receive(packet);
			int frameNumber = DataUtils.bytesToInt(packetBuffer, DataUtils.FRAME_ID);
			int packetTotal = DataUtils.bytesToShort(packetBuffer, DataUtils.PACKET_COUNT);
			short packetNumber = DataUtils.bytesToShort(packetBuffer, DataUtils.PACKET_ID);
			short packetSize = DataUtils.bytesToShort(packetBuffer, DataUtils.DATA_SIZE);
			FramePacket pack = new FramePacket(packetNumber, packetBuffer, DataUtils.DATA_IND, packetSize);
			processPacket(pack, frameNumber, packetTotal);
		}
		
		return null;
	}

	private void sendFinishedImage(BufferedImage image)
	{
		//Fires event with image.
		//TODO: Calculate and send frame rate?
		this.firePropertyChange("FrameFinished", null, image);
	}
	
	public void processPacket(FramePacket packet, int frameID, int numPackets) {
		if(!reconstructor.addPacket(packet, frameID))
		{
			//DP.print("New Frame: " + frameID);
			reconstructor.newFrame(frameID, numPackets);
			reconstructor.addPacket(packet, frameID);
		}
	}
	
	class FrameReconstructor
	{
		SortedMap<Integer, PartialFrame> frames;
		
		private FrameReconstructor()
		{
			frames = new TreeMap<Integer, PartialFrame>();
		}
		
		private boolean addPacket(FramePacket toAdd, int frameID)
		{
			
			if(frames.containsKey(new Integer(frameID)))
			{
				//Evaluates to true if the frame has all packets.
				if(frames.get(frameID).addPacket(toAdd))
				{
					threadPool.execute(new DecoderThread(frames.get(frameID)));
					clearOldFrames(frameID);
				}
				
				return true;
			}
			return false;
		}
		
		private boolean newFrame(int frameID, int numPackets)
		{
			PartialFrame newFrame = new PartialFrame(numPackets);
			return frames.put(frameID, newFrame) == null;
		}
		
		private void clearOldFrames(int frameID)
		{
			Iterator<Entry<Integer, PartialFrame>> it = frames.headMap(frameID + 1).entrySet().iterator();
			int count = -1;
			while(it.hasNext())
			{
				count++;
				it.next();
				it.remove();
			}
			if(count > 0) 
			{
				DP.print(count + " frame(s) dropped. (" + frameStreak + " since last)");
				frameStreak = 0;
			}
			else 
			{
				frameStreak++;
			}
		}
	}
	
	//I use a separate thread to decode the frames, so the main thread can keep reading from the socket.
	class DecoderThread implements Runnable
	{
		PartialFrame frame;
		
		private DecoderThread(PartialFrame frame)
		{
			this.frame = frame;
		}
		
		@Override
		public void run() {
			
			FramePacket[] packets = frame.getPackets();
			
			if(frame.getTotalBytes() > decodeBuff.capacity())
				decodeBuff = ByteBuffer.allocate(frame.getTotalBytes());
			
			decodeBuff.clear();
			
			for(FramePacket p : packets)
			{
				decodeBuff.put(p.getData());
			}

			Picture real = decoder.decodeFrame(decodeBuff, out.getData());
			BufferedImage image = JCodecUtil.toBufferedImage(real);
			
			sendFinishedImage(image);
		}
		
	}
	
	class PartialFrame
	{
		//object to store all packets for an individual frame
		private FramePacket[] packets;
		private int numReceived;
		private int totalBytes = 0;
		
		private PartialFrame(int numPackets)
		{
			 packets = new FramePacket[numPackets];
			 numReceived = 0;
		}
		
		private boolean addPacket(FramePacket toAdd)
		{
			//returns true if all packets have been received
			//at the moment, this assumes that no duplicate packets are added.
			packets[toAdd.packetID] = toAdd;
			totalBytes += toAdd.getData().length;
			if(++numReceived == packets.length) return true;
			return false;
		}
		
		public FramePacket[] getPackets()
		{
			return packets;
		}
		
		public int getTotalBytes()
		{
			return totalBytes;
		}
		
	}
	
	class FramePacket
	{
		//immutable object to hold individual packet data
		final int packetID;
		private byte[] data;
		private FramePacket(int packetID, byte[] data, int dataOffset, int dataLength) {
			super();
			//we need dataLength because the packetBuffer might have trailing garbage
			this.packetID = packetID;
			this.data = new byte[dataLength];
			System.arraycopy(data, dataOffset, this.data, 0, dataLength);
		}
		public byte[] getData()
		{
			return data;
		}
	}
	
	//Simple test main
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
