package au.edu.usyd.corona.gui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * The main frame of the system that appears after a login
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
public class MainFrame extends JFrame implements DrawingConstants {
	public MainFrame() {
		GUIManager.getInstance().registerFrame(this);
		
		// general frame properties
		setTitle("Corona");
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		
		// add the top logo name
		final JLabel topLogo = new JLabel();
		topLogo.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		topLogo.setIcon(new ImageIcon("images/corona-single-black.png"));
		final JPanel topContainer = new JPanel(new FlowLayout(FlowLayout.CENTER));
		topContainer.add(topLogo);
		topContainer.setOpaque(true);
		add(topContainer, BorderLayout.NORTH);
		
		// add the disconnect panel
		final JPanel disconnectPanel = new JPanel();
		initDisconnectPanel(disconnectPanel);
		
		// add the admin panel
		final JPanel adminPanel = new JPanel();
		initAdminPanel(adminPanel);
		
		// side container for the side panels
		final JPanel sideContainer = new JPanel();
		sideContainer.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		sideContainer.setLayout(new BoxLayout(sideContainer, BoxLayout.Y_AXIS));
		sideContainer.add(disconnectPanel);
		sideContainer.add(Box.createRigidArea(new Dimension(0, 5)));
		sideContainer.add(adminPanel);
		sideContainer.add(Box.createVerticalGlue());
		add(sideContainer, BorderLayout.WEST);
		
		// add the table of queries
		final QueriesTablePanel queries = new QueriesTablePanel();
		queries.setBorder(BorderFactory.createTitledBorder("Queries"));
		
		// container to house the queries table
		final JPanel mainContainer = new JPanel(new BorderLayout());
		mainContainer.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 5));
		mainContainer.add(queries, BorderLayout.CENTER);
		add(mainContainer, BorderLayout.CENTER);
		
		// sets up the sizings
		final Dimension d = new Dimension(1050, 480);
		setMinimumSize(d);
		setPreferredSize(d);
		
		// capture the close window event
		addWindowListener(new WindowAdapter() {
			@Override
			public void windowClosing(WindowEvent e) {
				dispose();
				GUIManager.getInstance().closeMainFrame();
			}
		});
		
		// finalise
		pack();
		setVisible(true);
	}
	
	private void initDisconnectPanel(JPanel disconnectPanel) {
		disconnectPanel.setBorder(BorderFactory.createTitledBorder("Logout of Corona"));
		disconnectPanel.setLayout(new BoxLayout(disconnectPanel, BoxLayout.X_AXIS));
		
		// ability to disconnect from the system
		final JLabel label = new JLabel("Logout:");
		final JButton button = new JButton("Logout");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
				GUIManager.getInstance().closeMainFrame();
			}
		});
		disconnectPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		disconnectPanel.add(label);
		disconnectPanel.add(Box.createRigidArea(new Dimension(5, 0)));
		disconnectPanel.add(button);
		disconnectPanel.add(Box.createHorizontalGlue());
	}
	
	private void initAdminPanel(JPanel adminPanel) {
		adminPanel.setBorder(BorderFactory.createTitledBorder("Administration"));
		adminPanel.setLayout(new SpringLayout());
		
		// ability to change password
		final JLabel pwdLabel = new JLabel("Change Password:");
		final JButton pwdButton = new JButton("Change");
		pwdButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new UserChangePasswordDialog(MainFrame.this).setVisible(true);
			}
		});
		adminPanel.add(pwdLabel);
		adminPanel.add(pwdButton);
		
		// user management for admin users
		final JLabel userManagementLabel = new JLabel("User Management:");
		final JButton userManagementButton = new JButton("View");
		userManagementButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				new UserManagementFrame().setVisible(true);
			}
		});
		boolean privilaged = false;
		try {
			privilaged = GUIManager.getInstance().getRemoteSessionInterface().getLoggedInUser().getAccessLevel() == AccessLevel.ADMIN;
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
			return;
		}
		userManagementLabel.setVisible(privilaged);
		userManagementButton.setVisible(privilaged);
		adminPanel.add(userManagementLabel);
		adminPanel.add(userManagementButton);
		
		SpringUtils.makeCompactGrid(adminPanel, 2, 2, 5, 5, 5, 5);
	}
}
