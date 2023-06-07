package au.edu.usyd.corona.gui;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.TableModelEvent;

import au.edu.usyd.corona.gui.cache.CacheChangeListener;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.cache.UserCache;
import au.edu.usyd.corona.gui.util.DialogUtils;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.session.UserAccessException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * This frame allows admin users to adminstrate the users on the server. This
 * includes adding users, removing users, and changing other users passwords or
 * adminsitration privilages.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class UserManagementFrame extends JFrame implements DrawingConstants {
	private static final Logger logger = Logger.getLogger(UserManagementFrame.class.getCanonicalName());
	private static final Class<?>[] CLASSES = {User.class, AccessLevel.class};
	private static final String[] COLUMNS = {"User", "Access Level"};
	private static final String[] COLUMNS_SQL = {"username", "access_level"};
	
	private final JTable usersTable;
	private final MyTableModel tableModel;
	
	private final JPanel userPanel;
	private final JButton userPanelButton;
	private final JTextField userUsername;
	private final JComboBox userAccess;
	private final JPasswordField userPasswd1;
	private final JPasswordField userPasswd2;
	private User editUser;
	private final JButton buttonDeleteUser;
	
	public UserManagementFrame() {
		// sets up frame properties
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("User Management");
		setLayout(new BorderLayout());
		
		// instructions heading
		JLabel heading = new JLabel("User Management");
		heading.setFont(MEDIUM_FONT);
		heading.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		add(heading, BorderLayout.PAGE_START);
		
		final JPanel centrePanel = new JPanel();
		centrePanel.setLayout(new GridLayout(1, 2, 5, 5));
		centrePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
		
		// the left current users panel
		final JPanel leftPanel = new JPanel(new BorderLayout());
		leftPanel.setBorder(BorderFactory.createTitledBorder("Current Users"));
		
		// the users table
		usersTable = new JTable();
		tableModel = new MyTableModel(usersTable);
		usersTable.setModel(tableModel);
		usersTable.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				int row = usersTable.getSelectedRow();
				if (row != -1) {
					editUser = tableModel.cache.getObjectAtIndex(tableModel.ordering, row);
					updateUserPanel();
				}
			}
		});
		//		usersTable.setAutoCreateRowSorter(true); FOOBAR
		usersTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane pane = new JScrollPane(usersTable);
		pane.setOpaque(false);
		leftPanel.add(pane, BorderLayout.CENTER);
		centrePanel.add(leftPanel);
		
		// the right editing panel
		final JPanel rightPanel = new JPanel();
		rightPanel.setBorder(BorderFactory.createTitledBorder("Add/Edit User"));
		centrePanel.add(rightPanel);
		add(centrePanel, BorderLayout.CENTER);
		
		// the bottom button
		final JPanel buttonPanel = new JPanel();
		final JButton buttonAddUser = new JButton("New User");
		buttonDeleteUser = new JButton("Delete User");
		final JButton buttonOk = new JButton("Close");
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttonPanel.add(buttonAddUser);
		buttonPanel.add(buttonDeleteUser);
		buttonPanel.add(buttonOk);
		
		add(buttonPanel, BorderLayout.PAGE_END);
		
		buttonOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				dispose();
			}
		});
		buttonAddUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				editUser = null;
				updateUserPanel();
			}
		});
		buttonDeleteUser.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				try {
					int deleteCheck = JOptionPane.showConfirmDialog(UserManagementFrame.this, "Are you sure you want to delete user \"" + editUser.getUsername() + "\"?", "Confirm Delete User", JOptionPane.YES_NO_OPTION);
					if (deleteCheck == JOptionPane.YES_OPTION) {
						doDeleteUser();
						DialogUtils.showMultilineInformation(UserManagementFrame.this, "Delete User", "The user has been successfully deleted");
						editUser = null;
					}
					updateUserPanel();
				}
				catch (UserAccessException err) {
					handleUserAccessException(err);
				}
				catch (RemoteException err) {
					GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(err);
				}
			}
		});
		
		// the user edit/add panel
		userPanel = new JPanel(new SpringLayout());
		userPanel.add(new JLabel("Username"));
		userPanel.add(userUsername = new JTextField(20));
		userPanel.add(new JLabel("Access Level"));
		userPanel.add(userAccess = new JComboBox(AccessLevel.values()));
		userPanel.add(new JLabel("Password"));
		userPanel.add(userPasswd1 = new JPasswordField(20));
		userPanel.add(new JLabel("Confirm Password"));
		userPanel.add(userPasswd2 = new JPasswordField(20));
		userPanel.add(new JLabel());
		userPanel.add(userPanelButton = new JButton("Save"));
		userUsername.setEnabled(false);
		userAccess.setEnabled(false);
		userPasswd1.setEnabled(false);
		userPasswd2.setEnabled(false);
		userPanelButton.setEnabled(false);
		userPanelButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				// checks that the passwords match
				final String pwd1 = new String(userPasswd1.getPassword());
				final String pwd2 = new String(userPasswd2.getPassword());
				if (!pwd1.equals(pwd2)) {
					DialogUtils.showMultilineWarning(UserManagementFrame.this, "Error", "Your passwords do not match. Try again.");
					userPasswd1.setText("");
					userPasswd2.setText("");
					userPasswd1.requestFocus();
					return;
				}
				
				// invokes the appropreate action
				try {
					if (editUser == null) {
						doAddUser();
						DialogUtils.showMultilineInformation(UserManagementFrame.this, "User Added", "The user has been successfully added.");
					}
					else {
						doEditUser();
						DialogUtils.showMultilineInformation(UserManagementFrame.this, "User Edited", "The user has been successfully edited.");
					}
				}
				catch (UserAccessException err) {
					handleUserAccessException(err);
				}
				catch (RemoteException err) {
					GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(err);
				}
			}
		});
		SpringUtils.makeCompactGrid(userPanel, 5, 2, 0, 0, 0, 0);
		rightPanel.add(userPanel);
		
		buttonDeleteUser.setEnabled(false);
		
		// finalise
		pack();
		GUIManager.getInstance().registerFrame(this);
	}
	
	@Override
	public void dispose() {
		tableModel.dispose();
		editUser = null;
		super.dispose();
	}
	
	private void updateUserPanel() {
		boolean edit = editUser != null;
		
		// updates the data in the fields accordingly
		userPanelButton.setText(edit ? "Save Changes" : "Add User");
		userUsername.setText(edit ? editUser.getUsername() : "");
		userAccess.setSelectedItem(edit ? editUser.getAccessLevel() : AccessLevel.USER);
		userPasswd1.setText(edit ? editUser.getPassword() : "");
		userPasswd2.setText(edit ? editUser.getPassword() : "");
		
		if (editUser != null) {
			buttonDeleteUser.setEnabled(true);
		}
		else {
			buttonDeleteUser.setEnabled(false);
		}
		
		// makes them all enabled
		userUsername.setEnabled(true);
		userAccess.setEnabled(true);
		userPasswd1.setEnabled(true);
		userPasswd2.setEnabled(true);
		userPanelButton.setEnabled(true);
	}
	
	private void handleUserAccessException(UserAccessException e) {
		logger.severe(e.getMessage());
		DialogUtils.showMultilineWarning(this, "Useer Access Error", "A User Access has occured:\n" + e.getMessage());
	}
	
	private void doEditUser() throws RemoteException, UserAccessException {
		String userName = userUsername.getText();
		String passwd = new String(userPasswd1.getPassword());
		AccessLevel level = (AccessLevel) userAccess.getSelectedItem();
		GUIManager.getInstance().getRemoteSessionInterface().updateUser(editUser.getId(), userName, passwd, level);
	}
	
	private void doAddUser() throws RemoteException, UserAccessException {
		String userName = userUsername.getText();
		String passwd = new String(userPasswd1.getPassword());
		AccessLevel level = (AccessLevel) userAccess.getSelectedItem();
		GUIManager.getInstance().getRemoteSessionInterface().addUser(userName, passwd, level);
	}
	
	private void doDeleteUser() throws RemoteException, UserAccessException {
		String userName = userUsername.getText();
		GUIManager.getInstance().getRemoteSessionInterface().deleteUser(userName);
	}
	
	private static class MyTableModel extends CacheBasedSortableTableModel implements CacheChangeListener {
		private final UserCache cache;
		
		public MyTableModel(JTable table) {
			super(table, COLUMNS_SQL, 0, false);
			cache = CacheManager.getInstance().getUserCache();
			cache.addChangeListener(this);
		}
		
		public Class<?> getColumnClass(int columnIndex) {
			return CLASSES[columnIndex];
		}
		
		public String getColumnName(int columnIndex) {
			return COLUMNS[columnIndex];
		}
		
		public int getRowCount() {
			return cache.getSize(ordering);
		}
		
		public Object getValueAt(int rowIndex, int columnIndex) {
			final User u = cache.getObjectAtIndex(ordering, rowIndex);
			if (columnIndex == 0)
				return u.getUsername();
			else if (columnIndex == 1)
				return u.getAccessLevel();
			else
				return null;
		}
		
		@Override
		protected boolean canSortOnColumn(int columnNumber) {
			return true;
		}
		
		public void cacheChanged() {
			updateListeners(new TableModelEvent(this));
		}
		
		@Override
		protected void _dispose() {
			cache.removeChangeListener(this);
		}
	}
}
