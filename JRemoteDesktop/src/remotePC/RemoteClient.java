package remotePC;

import java.awt.AWTException;
import java.awt.Dimension;
import java.awt.MenuItem;
import java.awt.PopupMenu;
import java.awt.Rectangle;
import java.awt.SystemTray;
import java.awt.Toolkit;
import java.awt.TrayIcon;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ObjectInputStream;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutionException;

import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingWorker;

import tests.DP;
import utils.DataUtils;

public class RemoteClient {

	public static void main(String[] args) throws Exception {
		new RemoteClient(DataUtils.HANDSHAKE_PORT);
	}
	
	JLabel label;
	String address;
	String password = "";
	
	SwingWorker vb;
	
	public RemoteClient(int port) throws Exception
	{
		addTrey();
		listen(port);
		//new InputReceiver(address, DataUtils.INPUT_PORT);

	}
	
	public void updateLabel(Object fps)
	{
		label.setText("Broadcasting to " + address + " at " + fps.toString().substring(0, 5) + " FPS");
	}
	
	public void listen(int port) throws Exception
	{
		ServerSocket server = new ServerSocket(port);
		Socket socket = server.accept();
		ObjectInputStream stream = new ObjectInputStream(socket.getInputStream());
		DP.print("listening");
		
		boolean keepAlive = true;
		
		try{
			while(keepAlive)
			{
				String message = stream.readObject().toString();
				InetSocketAddress insaddress = (InetSocketAddress) socket.getRemoteSocketAddress();
				address = insaddress.getHostName();
				
				if("connect".equals(message.substring(0, 7)))
				{
					if("".equals(password))
					{
						int answer = JOptionPane.showConfirmDialog(null, "Accept request for remote access from " + address + "?",
								"Connection Request", JOptionPane.OK_CANCEL_OPTION);
						if(answer == JOptionPane.OK_OPTION)
						{
							startBroadcast();
						}
						else
						{
							socket.close();
							server.close();
							server = new ServerSocket(port);
							socket = server.accept();
							stream = new ObjectInputStream(socket.getInputStream());
						}
					}
					else
					{
						int passcode = Integer.parseInt(message.substring(7));
						if(passcode == password.hashCode())
						{
							startBroadcast();
							JOptionPane.showMessageDialog(null, "Remote access connection has been started.");
						}
						else
						{
							DP.print("Access attempt with incorrect password.");
							socket.close();
							server.close();
							server = new ServerSocket(port);
							socket = server.accept();
							stream = new ObjectInputStream(socket.getInputStream());

						}
					}
				}
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
	
	public void startBroadcast()
	{
		Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
		vb = new VideoBroadcaster(new Rectangle(screenSize.width, screenSize.height), address, DataUtils.VIDEO_PORT);
		vb.execute();
		
		JFrame frame = new JFrame();
		label = new JLabel("Broadcasting to " + address + " at 0.00000 fps");
		frame.getContentPane().add(label);
		frame.pack();
		frame.setVisible(true);
		frame.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		frame.addWindowListener(new WindowListener(){

			@Override
			public void windowOpened(WindowEvent e) {
			}
			@Override
			public void windowClosing(WindowEvent e) {
			}
			@Override
			public void windowClosed(WindowEvent e) {
				vb.cancel(true);
			}
			@Override
			public void windowIconified(WindowEvent e) {
			}
			@Override
			public void windowDeiconified(WindowEvent e) {
			}
			@Override
			public void windowActivated(WindowEvent e) {
			}
			@Override
			public void windowDeactivated(WindowEvent e) {
			}
			
		});
		
		new InputReceiver(address, DataUtils.INPUT_PORT);
		
		try {
			vb.addPropertyChangeListener(new PropertyChangeListener(){

				@Override
				public void propertyChange(
						PropertyChangeEvent evt) {
					if("fps".equals(evt.getPropertyName()))
					{
						updateLabel(evt.getNewValue());
					}
				}
				
			});
			vb.get();
		} catch (InterruptedException | ExecutionException e) {
			e.printStackTrace();
			Toolkit.getDefaultToolkit().beep();
			DP.popup("Fatal error in broadcaster");
			System.exit(1);
		}

	}
	
	public void addTrey()
	{
		if (!SystemTray.isSupported()) {
            System.out.println("SystemTray is not supported");
            return;
        }
        final PopupMenu popup = new PopupMenu();
        
        final TrayIcon trayIcon =
                new TrayIcon(Toolkit.getDefaultToolkit().createImage("icons/java16.gif"));
        final SystemTray tray = SystemTray.getSystemTray();
       
        // Create a pop-up menu components
 
       
        //Add components to pop-up menu
        MenuItem setPassword = new MenuItem("Set Password");
        popup.add(setPassword);
        
        setPassword.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				password = DP.askForPassword();
			}
        });

        MenuItem exitItem = new MenuItem("Exit");
        popup.add(exitItem);
        
        exitItem.addActionListener(new ActionListener(){
			@Override
			public void actionPerformed(ActionEvent e) {
				System.exit(0);
			}
        });

        trayIcon.setPopupMenu(popup);
        trayIcon.setToolTip("JAccess Client");
       
        try {
            tray.add(trayIcon);
        } catch (AWTException e) {
            DP.popup("TrayIcon could not be added.\nClient will run in background" );
        }
	}
	
}
