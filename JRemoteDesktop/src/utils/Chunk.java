package utils;

import java.awt.image.BufferedImage;

public class Chunk
{
	public BufferedImage img;
	public final int x;
	public final int y;
	public static final int WIDTH = 100;
	public static final int HEIGHT = 100;

	public Chunk(BufferedImage img, int x, int y) {
		super();
		this.img = img;
		this.x = x;
		this.y = y;
	}
}
