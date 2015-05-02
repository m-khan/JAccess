package tests;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

public class DP {
	
	public static boolean silent = false;
	
	//Debug Printer
	public static void print(String s)
	{
		if(!silent) System.out.println(s);
	}
	
	public static void err(Exception e)
	{
		if(!silent) e.printStackTrace();
	}
	
	public static void popup(String m)
	{
		if(!silent) JOptionPane.showMessageDialog(null, m);
	}
	
	public static String askForPassword()
	{
		JPanel panel = new JPanel();
		JLabel label = new JLabel("Enter a password:");
		JPasswordField pass = new JPasswordField(10);
		panel.add(label);
		panel.add(pass);
		String[] options = new String[]{"OK", "Cancel"};
		int option = JOptionPane.showOptionDialog(null, panel, "",
		                         JOptionPane.NO_OPTION, JOptionPane.PLAIN_MESSAGE,
		                         null, options, options[1]);
		if(option == 0) // pressing OK button
		{
		    char[] password = pass.getPassword();
		    return new String(password);
		}
		
		return null;
	}
	
}
