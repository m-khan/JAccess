package utils;

import java.awt.image.BufferedImage;
import java.util.Arrays;

public class Chunk
{
	public BufferedImage img;
	public final int x;
	public final int y;
	public static final int WIDTH = 120;
	public static final int HEIGHT = 120;

	public Chunk(BufferedImage img, int x, int y) {
		super();
		this.img = img;
		this.x = x;
		this.y = y;
	}

	public boolean equals(Object other)
	{
		Chunk o = (Chunk) other;
		byte[] tb = DataUtils.getBytes(this.img);
		byte[] ob = DataUtils.getBytes(o.img);
		
		return Arrays.equals(tb, ob);
	}
}
