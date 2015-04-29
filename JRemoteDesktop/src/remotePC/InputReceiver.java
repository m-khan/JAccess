package remotePC;

import java.awt.AWTException;
import java.awt.Robot;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import tests.DP;
import utils.Input;

public class InputReceiver {

	Robot rob;
	final int port;
	final String remoteAddress;
	Queue<Input> queue = new ConcurrentLinkedQueue<Input>();

	public InputReceiver(String remoteAddress, int port)
	{
		this.remoteAddress = remoteAddress;
		this.port = port;
		try {
			rob = new Robot();
		} catch (AWTException e) {
			e.printStackTrace();
		}
		Thread server = new InputServer();
		Thread executor = new InputExecutor();
		server.start();
		executor.start();
		
	}
	
	class InputExecutor extends Thread
	{
		@Override 
		public void run() {
			while(this.isAlive())
			{
				if(!queue.isEmpty())
				{
					Input e = queue.peek();
					try {
						execute(e);
					} catch (Exception e1) {
						queue.poll();
						DP.print("Error executing " + e);
						e1.printStackTrace();
					}
					queue.poll();
					DP.print("Executed: " + e.event);
				}
			}
		}
		
		public void execute(Input e) throws InterruptedException
		{
			switch(e.type) {
			case Input.MOUSE_PRESSED:
				MouseEvent mp = (MouseEvent) e.event;
				rob.mouseMove(mp.getX(), mp.getY());
				Thread.sleep(20);
				rob.mousePress(MouseEvent.getMaskForButton(mp.getButton()));
				break;
			case Input.MOUSE_RELEASED:
				MouseEvent mr = (MouseEvent) e.event;
				rob.mouseMove(mr.getX(), mr.getY());
				Thread.sleep(20);
				rob.mouseRelease(MouseEvent.getMaskForButton(mr.getButton()));
				break;
			case Input.KEY_PRESSED:
				rob.keyPress(((KeyEvent) e.event).getKeyCode());
				break;
			case Input.KEY_RELEASED:
				rob.keyRelease(((KeyEvent) e.event).getKeyCode());
				break;
			case Input.MOUSE_WHEEL:
				rob.mouseWheel(((MouseWheelEvent)e.event).getScrollAmount());
				break;
			case Input.MOUSE_MOVED:
				MouseEvent mm = (MouseEvent) e.event;
				rob.mouseMove(mm.getX(), mm.getY());
			}

		}
	}
	
	class InputServer extends Thread
	{
		@Override
		public void run() {
			
			while(this.isAlive())
			{
				try{
					listen();
				}catch(Exception e){
					e.printStackTrace();
				}
			}
		}
		
		public void listen() throws Exception
		{
			ServerSocket server = new ServerSocket(port);
			Socket socket = server.accept();
			ObjectInputStream stream = new ObjectInputStream(socket.getInputStream());
			DP.print("Connection established, listening for inputs");
			try{
				while(this.isAlive())
				{
					Input evt = (Input) stream.readObject();
					InetSocketAddress insaddress = (InetSocketAddress) socket.getRemoteSocketAddress();
					String address = insaddress.getHostName();

					if(address.equals(remoteAddress))
						queue.add(evt);
				}
				DP.print("Thread stopped, closing connection");
				socket.close();
				server.close();
			}catch(Exception e){
				socket.close();
				server.close();
				throw e;
			}
		}
	}
	
	
}