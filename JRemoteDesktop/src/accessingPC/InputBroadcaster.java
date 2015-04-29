package accessingPC;

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import tests.DP;
import utils.Input;

public class InputBroadcaster implements KeyListener, MouseListener, MouseMotionListener, MouseWheelListener {

	Queue<Input> queue = new ConcurrentLinkedQueue<Input>();
	
	public final String address;
	public final int port;
	Thread thread;
	public final int retryMax = 10;
	int retryCount = 0;
	
	public InputBroadcaster(String address, int port) {
		super();
		this.address = address;
		this.port = port;
		
		thread = new InputClient();
		thread.start();
	}

	public void queueSend(Input e)
	{
		queue.add(e);
	}

	class InputClient extends Thread
	{
		@Override
		public void run() {
			
			while(this.isAlive())
			{
				try {
					sendInputs();
					retryCount = 0;
				} catch (Exception e) {
					if(retryCount++ == retryMax)
					{
						DP.popup("Connection Failed after " + retryMax + " attempts.");
						System.exit(1);
					}
					//DP.print("No Input Server... " + retryCount);
					try {
						Thread.sleep(1000);
					} catch (InterruptedException e1) {
						e1.printStackTrace();
					}
				}
			}
		}
		
		public void sendInputs() throws Exception
		{
			Socket socket = new Socket(address, port);
			try{
				ObjectOutputStream stream = new ObjectOutputStream(socket.getOutputStream());
				
				while(this.isAlive())
				{
					if(!queue.isEmpty())
					{
						Input e = queue.peek();
						stream.writeObject(e);
						queue.poll();
					}
				}
				socket.close();

			}catch(Exception e){
				socket.close();
				throw e;
			}
			
		}
		
	}
	
	//Event handlers, very boring
	@Override
	public void mousePressed(MouseEvent e) {
		queueSend(new Input(e, Input.MOUSE_PRESSED));
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		queueSend(new Input(e, Input.MOUSE_RELEASED));
	}

	@Override
	public void keyPressed(KeyEvent e) {
		queueSend(new Input(e, Input.KEY_PRESSED));
	}

	@Override
	public void keyReleased(KeyEvent e) {
		queueSend(new Input(e, Input.KEY_RELEASED));
	}
	@Override
	public void mouseDragged(MouseEvent e) {
		queueSend(new Input(e, Input.MOUSE_MOVED));
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		queueSend(new Input(e, Input.MOUSE_MOVED));
	}

	@Override
	public void mouseEntered(MouseEvent e) {
	}
	@Override
	public void mouseExited(MouseEvent e) {
	}
	@Override
	public void mouseClicked(MouseEvent e) {
	}
	@Override
	public void keyTyped(KeyEvent e) {
	}

	@Override
	public void mouseWheelMoved(MouseWheelEvent e) {
		queueSend(new Input(e, Input.MOUSE_WHEEL));
	}


}
