package au.edu.usyd.corona.gui;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.SpringLayout;

import au.edu.usyd.corona.gui.util.DialogUtils;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.session.UserAccessException;

/**
 * This class allows users to change their own passwords
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class UserChangePasswordDialog extends JDialog implements DrawingConstants {
	public UserChangePasswordDialog(Frame owner) {
		super(owner);
		
		// sets up frame properties
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Change Password");
		setLayout(new BorderLayout());
		setModal(true);
		
		// instructions heading
		JLabel heading = new JLabel("Change Password");
		heading.setFont(MEDIUM_FONT);
		heading.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(heading, BorderLayout.PAGE_START);
		
		final JLabel oldLabel = new JLabel("Old password");
		final JPasswordField oldPassword = new JPasswordField(20);
		final JLabel newLabel = new JLabel("New password");
		final JPasswordField newPassword = new JPasswordField(20);
		final JLabel newConfirmLabel = new JLabel("Confirm new");
		final JPasswordField newConfirmPassword = new JPasswordField(20);
		
		// the main content
		final JPanel content = new JPanel(new SpringLayout());
		add(content, BorderLayout.CENTER);
		content.add(oldLabel);
		content.add(oldPassword);
		content.add(newLabel);
		content.add(newPassword);
		content.add(newConfirmLabel);
		content.add(newConfirmPassword);
		SpringUtils.makeCompactGrid(content, 3, 2, 5, 5, 5, 5);
		
		// the action buttons
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(footer, BorderLayout.PAGE_END);
		
		// add a change button
		final JButton changeButton = new JButton("Change");
		changeButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// grabs the two passwords
				String newPwd = new String(newPassword.getPassword());
				String newCPwd = new String(newConfirmPassword.getPassword());
				
				// makes sure they are equal
				if (!newPwd.equals(newCPwd)) {
					DialogUtils.showMultilineWarning(UserChangePasswordDialog.this, "Error", "Your passwords do not match. Try again.");
					newPassword.setText("");
					newConfirmPassword.setText("");
					newPassword.requestFocus();
				}
				else if (newPwd.trim().length() == 0) {
					DialogUtils.showMultilineWarning(UserChangePasswordDialog.this, "Error", "Passwords can not be empty or only whitespace. Try again.");
					newPassword.setText("");
					newConfirmPassword.setText("");
					newPassword.requestFocus();
				}
				else {
					// attempts to update the user
					try {
						GUIManager.getInstance().getRemoteSessionInterface().updatePassword(new String(oldPassword.getPassword()), newPwd);
						DialogUtils.showMultilineInformation(UserChangePasswordDialog.this, "Password Changed", "Your password was successfully changed.");
						dispose();
					}
					catch (UserAccessException err) {
						DialogUtils.showMultilineError(UserChangePasswordDialog.this, "UserAccessException", err.getMessage());
					}
					catch (RemoteException err) {
						dispose();
						GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(err);
					}
				}
			}
		});
		footer.add(changeButton);
		
		// add a cancel button
		final JButton cancelButton = new JButton("Cancel");
		cancelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		footer.add(cancelButton);
		
		pack();
	}
}
