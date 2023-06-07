package au.edu.usyd.corona.gui;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.logging.LogManager;
import java.util.logging.Logger;

import javax.swing.*;

import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.util.DialogUtils;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.session.RemoteSessionInterface;
import au.edu.usyd.corona.server.session.RemoteSessionManagerInterface;
import au.edu.usyd.corona.server.user.AccessDeniedException;

/**
 * This is the startup frame for the GUI which presents the user with a place to
 * login to a server.
 * 
 * @author Tim Dawborn
 */
@SuppressWarnings("serial")
public class LoginFrame extends JFrame implements DrawingConstants {
	private static final Logger logger = Logger.getLogger(LoginFrame.class.getCanonicalName());
	
	private final JTextField hostField;
	private final JTextField usernameField;
	private final JPasswordField passwdField;
	private final JButton loginButton;
	
	public LoginFrame() {
		// use OS native L&F if possible
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
			logger.warning(e.getMessage());
		}
		GUIManager.getInstance().setLoginFrame(this);
		
		// general frame properties
		setTitle("Corona");
		setLayout(new BorderLayout());
		setResizable(false);
		getContentPane().setBackground(BLACK);
		
		final JPanel topPanel = new JPanel();
		topPanel.setOpaque(false);
		final JPanel mainContent = new JPanel(new BorderLayout());
		mainContent.setOpaque(false);
		final JPanel adminPanelWrapper = new JPanel();
		adminPanelWrapper.setLayout(new FlowLayout(FlowLayout.CENTER));
		adminPanelWrapper.setOpaque(false);
		final JPanel adminPanel = new JPanel(new SpringLayout());
		adminPanel.setOpaque(false);
		adminPanel.setBorder(BorderFactory.createEmptyBorder(20, 5, 5, 5));
		
		// add the top logo name
		final JLabel topLogo = new JLabel();
		topLogo.setIcon(new ImageIcon("images/corona-single.png"));
		topPanel.add(topLogo);
		add(topPanel, BorderLayout.PAGE_START);
		
		// add the bottom logo image
		final JLabel bottomLogo = new JLabel();
		bottomLogo.setIcon(new ImageIcon("images/sun-single.png"));
		mainContent.add(bottomLogo, BorderLayout.WEST);
		
		final JLabel hostLabel = new JLabel("Host"), userLabel = new JLabel("Username"), passwordLabel = new JLabel("Password");
		hostLabel.setForeground(WHITE);
		userLabel.setForeground(WHITE);
		passwordLabel.setForeground(WHITE);
		adminPanel.add(hostLabel);
		adminPanel.add(hostField = new JTextField("localhost", 20));
		adminPanel.add(userLabel);
		adminPanel.add(usernameField = new JTextField("admin"));
		adminPanel.add(passwordLabel);
		adminPanel.add(passwdField = new JPasswordField("password"));
		adminPanel.add(new JLabel());
		adminPanel.add(loginButton = new JButton("Login"));
		SpringUtils.makeCompactGrid(adminPanel, 4, 2, 0, 0, 5, 5);
		adminPanelWrapper.add(adminPanel);
		mainContent.add(adminPanelWrapper, BorderLayout.EAST);
		
		// add listeners
		loginButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				try {
					RemoteSessionManagerInterface rmiManager = (RemoteSessionManagerInterface) Naming.lookup("//" + hostField.getText() + "/SessionManager");
					RemoteSessionInterface rmi = rmiManager.login(usernameField.getText(), new String(passwdField.getPassword()));
					try {
						updateConnect(rmi);
					}
					catch (AccessDeniedException e1) {
						DialogUtils.showMultilineError(LoginFrame.this, "Access Denied", "Could not update after connected:\n" + e1.getMessage());
					}
				}
				catch (MalformedURLException e1) {
					DialogUtils.showMultilineError(LoginFrame.this, "MalformedURLException", "Malformed URL Exception:\n" + e1.getMessage());
				}
				catch (RemoteException e1) {
					DialogUtils.showMultilineError(LoginFrame.this, "RemoteException", "A fatal remote exception has occured:\n" + e1.getMessage());
				}
				catch (NotBoundException e1) {
					DialogUtils.showMultilineError(LoginFrame.this, "NotBoundException", "A Not Bound Exception has occured:\n" + e1.getMessage());
				}
				catch (AccessDeniedException e1) {
					DialogUtils.showMultilineError(LoginFrame.this, "Could Not Login", "Could not log you in:\n" + e1.getMessage());
				}
			}
		});
		
		// finalize
		add(mainContent, BorderLayout.CENTER);
		pack();
	}
	
	private void updateConnect(RemoteSessionInterface rmi) {
		// sets up global variables
		GUIManager.getInstance().setRemoteSessionInterface(rmi);
		GUIManager.getInstance().setRemoteExceptionNotifier(new MyRemoteExceptionNotifier());
		
		// sets up the caches as notifiers
		try {
			CacheManager.getInstance().clearCaches();
			CacheManager.getInstance().initCaches();
		}
		catch (Exception e) {
			logger.severe(e.getMessage());
			updateDisconnect();
			return;
		}
		
		// swaps focus to the main frame
		setVisible(false);
		new MainFrame();
	}
	
	public void logout() {
		updateDisconnect();
	}
	
	private void updateDisconnect() {
		// empty data from previous session
		GUIManager.getInstance().closeAllFrames();
		CacheManager.getInstance().clearCaches();
		GUIManager.getInstance().clear();
		System.gc();
		
		// show the frame again
		passwdField.setText("");
		setVisible(true);
	}
	
	private class MyRemoteExceptionNotifier implements RemoteExceptionNotifier {
		private final Logger logger = Logger.getLogger(MyRemoteExceptionNotifier.class.getCanonicalName());
		
		private final boolean delayed;
		private boolean doAction = false;
		private volatile boolean isDisconnecting = false;
		
		public MyRemoteExceptionNotifier() {
			this(false);
		}
		
		public MyRemoteExceptionNotifier(boolean delayed) {
			this.delayed = delayed;
		}
		
		public synchronized void notifyHandler(RemoteException e) {
			if (isDisconnecting)
				return;
			
			logger.severe(e.getMessage());
			DialogUtils.showMultilineError(LoginFrame.this, "Remote Exception", "A fatal remote exception has occured:\n" + e.getMessage());
			if (!delayed) {
				isDisconnecting = true;
				updateDisconnect();
			}
			else
				doAction = true;
		}
		
		public void notifyHandlerDelayed() {
			if (doAction) {
				updateDisconnect();
				doAction = false;
			}
		}
		
		public boolean isDelayed() {
			return delayed;
		}
	}
	
	public static void main(String[] args) throws IOException {
		LogManager.getLogManager().readConfiguration(new FileInputStream("resources/logging.properties"));
		LoginFrame f = new LoginFrame();
		f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.setVisible(true);
	}
}
