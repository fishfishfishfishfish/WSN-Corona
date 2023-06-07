package au.edu.usyd.corona.gui.util;


import java.awt.Component;
import java.awt.Dialog;

import javax.swing.JOptionPane;

public class DialogUtils {
	public static final int WRAP_CHARS = 80;
	
	private DialogUtils() {
		// hidden constructor
	}
	
	public static void showMultilineError(Component parent, String title, String message) {
		showMultilineMessage(parent, title, message, JOptionPane.ERROR_MESSAGE, false);
	}
	
	public static void showMultilineInformation(Component parent, String title, String message) {
		showMultilineMessage(parent, title, message, JOptionPane.INFORMATION_MESSAGE, false);
	}
	
	public static void showMultilineWarning(Component parent, String title, String message) {
		showMultilineMessage(parent, title, message, JOptionPane.WARNING_MESSAGE, false);
	}
	
	private static void showMultilineMessage(Component parent, String title, String message, int type, boolean preformatted) {
		// wraps the message if applicable
		if (!preformatted) {
			StringBuffer b = new StringBuffer();
			int width = 0;
			for (String s : message.split("\\s+")) {
				if (width + s.length() > WRAP_CHARS) {
					b.append('\n');
					width = 0;
				}
				else if (width != 0) {
					b.append(' ');
					width++;
				}
				b.append(s);
				width += s.length();
			}
			message = b.toString();
		}
		
		// create and show the dialog
		JOptionPane pane = new JOptionPane(message, type, JOptionPane.DEFAULT_OPTION);
		Dialog dialog = pane.createDialog(parent, title);
		dialog.setVisible(true);
		dialog.dispose();
	}
}
