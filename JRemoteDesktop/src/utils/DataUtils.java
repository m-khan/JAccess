package utils;

import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.awt.image.WritableRaster;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

import javax.imageio.ImageIO;

import org.apache.commons.io.output.ByteArrayOutputStream;

public class DataUtils {

	public static final int FRAME_X = 0;
	public static final int FRAME_Y = 4;
	public static final int DATA_SIZE = 8;
	public static final int DATA_IND = 10;
	public static final int VIDEO_PORT = 9527;
	public static final int IMAGE_TYPE = BufferedImage.TYPE_BYTE_INDEXED;
	
	public static byte[] intToBytes(int x, byte[] bytes, int startIndex) {
	    //writes 4 bytes from the integer into the byte array, starting at startIndex
		for (int i = 0; i < 4; i++, x >>>= 8)
	        bytes[i + startIndex] = (byte) (x & 0xFF);
	    return bytes;
	}
	
	public static byte[] shortToBytes(short x, byte[] bytes, int startIndex)
	{
	    //writes 4 bytes from the integer into the byte array, starting at startIndex
		for (int i = 0; i < 2; i++, x >>>= 8)
	        bytes[i + startIndex] = (byte) (x & 0xFF);
	    return bytes;		
	}
	
	public static int bytesToInt(byte[] x, int startIndex) {
	    //reads 4 bytes starting at startIndex and turns them into integer
		int value = 0;
	    for(int i = 0; i < 4; i++)
	        value += ((long) x[i + startIndex] & 0xffL) << (8 * i);
	    return value;
	}

	public static short bytesToShort(byte[] x, int startIndex)
	{
		short value = 0;
		for(int i = 0; i < 2; i++)
		{
			value +=((long) x[i + startIndex] & 0xffL) << (8 * i);
		}
		return value;
	}
	
	public static ByteBuffer encode(BufferedImage bi)
	{
		try{
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write( bi, "jpg", baos );
			baos.flush();
			byte[] imageInByte = baos.toByteArray();
			baos.close();
			return ByteBuffer.wrap(imageInByte);
		}catch(IOException e){
			e.printStackTrace();
			return null;
		}
	}
	
	public static BufferedImage decode(byte[] data)
	{
		try {
			InputStream in = new ByteArrayInputStream(data);
			return ImageIO.read(in);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
	}
	
	public static ByteBuffer encode2(BufferedImage bi)
	{
		WritableRaster raster = bi.getRaster();
		DataBufferByte buffer = (DataBufferByte) raster.getDataBuffer();
		byte[] bytes = buffer.getData();
		return ByteBuffer.wrap(bytes);
	}

	public static BufferedImage decode2(byte[] pixels) {
	    BufferedImage image = new BufferedImage(Chunk.WIDTH, Chunk.HEIGHT, IMAGE_TYPE);
	    byte[] imgData = ((DataBufferByte)image.getRaster().getDataBuffer()).getData();
	    System.arraycopy(pixels, 0, imgData, 0, pixels.length);     
	    return image;
	}
}
