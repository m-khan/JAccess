package utils;

import java.awt.event.InputEvent;
import java.io.Serializable;

public class Input implements Serializable{

	private static final long serialVersionUID = -3972763649724452203L;
	public static final int MOUSE_PRESSED = 0;
	public static final int MOUSE_RELEASED = 1;
	public static final int KEY_PRESSED = 2;
	public static final int KEY_RELEASED = 3;
	public static final int MOUSE_MOVED = 4;
	public static final int MOUSE_WHEEL = 5;
	public final InputEvent event;
	public final int type;
	
	public Input(InputEvent e, int type) {
		this.event = e;
		this.type = type;
		
	}	
}
