package utils;

public class DataUtils {

	public static final int PACKET_SIZE = 1472;
	public static final int FRAME_ID = 0;
	public static final int PACKET_ID = 4;
	public static final int DATA_SIZE = 6;
	public static final int DATA_IND = 8;
	public static final int VIDEO_PORT = 9527;
	
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
}
