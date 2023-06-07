package au.edu.usyd.corona.gui;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.rmi.RemoteException;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.UIManager;
import javax.swing.border.EtchedBorder;
import javax.swing.plaf.FontUIResource;

import au.edu.usyd.corona.gui.dnd.QueryBuilderFrame;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.server.session.QueryExecuteException;

/**
 * This frame allows the user to enter their SQL queries
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class SQLEntryFrame extends JFrame implements DrawingConstants {
	private static final String REGEX = ".*found on line ([0-9]+) at position ([0-9]+).*";
	
	private final JTextArea sqlTextArea;
	private final JTextArea notificationArea;
	private final JButton buttonOk;
	private final JButton buttonCancel;
	private final JButton buttonQB;
	
	private final QueryBuilderFrame queryBuilder = new QueryBuilderFrame();
	
	public SQLEntryFrame() {
		// sets up frame properties
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setTitle("Enter SQL Query");
		setLayout(new BorderLayout());
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		}
		catch (Exception e) {
		}
		
		// instructions heading
		JLabel heading = new JLabel("Enter SQL Query");
		heading.setFont(MEDIUM_FONT);
		heading.setBorder(BorderFactory.createEmptyBorder(10, 10, 0, 10));
		add(heading, BorderLayout.PAGE_START);
		
		// middle contents
		final JPanel itemsPanel = new JPanel();
		itemsPanel.setLayout(new BorderLayout());
		itemsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		
		// error messages for the user
		notificationArea = new JTextArea(1, 50);
		notificationArea.setEditable(false);
		notificationArea.setForeground(RED);
		notificationArea.setFont(MONOSPACE_FONT);
		notificationArea.setLineWrap(true);
		notificationArea.setOpaque(false);
		notificationArea.setWrapStyleWord(true);
		notificationArea.setVisible(false);
		itemsPanel.add(notificationArea, BorderLayout.PAGE_START);
		
		// create the text area
		sqlTextArea = new JTextArea(15, 50);
		sqlTextArea.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		sqlTextArea.setFont(MONOSPACE_FONT);
		sqlTextArea.setLineWrap(true);
		sqlTextArea.setWrapStyleWord(true);
		itemsPanel.add(sqlTextArea, BorderLayout.CENTER);
		add(itemsPanel, BorderLayout.CENTER);
		
		// the action buttons
		final JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		buttonPanel.setBorder(BorderFactory.createEmptyBorder(0, 10, 10, 10));
		buttonPanel.add(buttonOk = new JButton("Run SQL"));
		buttonPanel.add(buttonCancel = new JButton("Cancel"));
		buttonPanel.add(buttonQB = new JButton("Query Builder"));
		add(buttonPanel, BorderLayout.PAGE_END);
		
		buttonOk.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				try {
					GUIManager.getInstance().getRemoteSessionInterface().executeQuery(sqlTextArea.getText());
					dispose();
				}
				catch (RemoteException e) {
					GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
				}
				catch (QueryExecuteException e) {
					// display the error message to the user via a JDialog, changing the font to be monospace
					final Object oldFont = UIManager.get("OptionPane.messageFont");
					UIManager.put("OptionPane.messageFont", new FontUIResource(MONOSPACE_FONT));
					JOptionPane.showMessageDialog(SQLEntryFrame.this, e.getMessage(), "SQL Error", JOptionPane.WARNING_MESSAGE);
					UIManager.put("OptionPane.messageFont", oldFont);
					final String message = e.getMessage().split("\n")[0];
					
					// display the notification in the notification error
					notificationArea.setText(message);
					notificationArea.setVisible(true);
					
					// if we can identify the location, highlight the error (found on line # at position #)
					if (message.matches(REGEX)) {
						String[] tmp = message.replaceFirst(REGEX, "$1 $2").split(" ");
						final int line = Integer.parseInt(tmp[0]) - 1;
						final int pos = Integer.parseInt(tmp[1]);
						String[] lines = sqlTextArea.getText().toLowerCase().split("\n");
						
						// find the global start position
						int start = pos, end = 0;
						for (int l = 0; l < line; l++)
							start += lines[l].length() + 1;
						
						// find the global end position
						char startChar = sqlTextArea.getText().toLowerCase().charAt(start);
						if (startChar >= 'a' && startChar <= 'z') {
							for (; pos + end < lines[line].length(); end++) {
								char c = lines[line].charAt(pos + end);
								if (c < 'a' || c > 'z')
									break;
							}
						}
						else
							end = 1;
						
						// highlight the invalid component
						sqlTextArea.requestFocus();
						sqlTextArea.select(start, start + end);
					}
				}
			}
		});
		
		buttonCancel.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				dispose();
			}
		});
		
		buttonQB.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent action) {
				queryBuilder.setVisible(true);
				queryBuilder.addWindowListener(new WindowAdapter() {
					@Override
					public void windowClosing(WindowEvent e) {
						sqlTextArea.setText(queryBuilder.getSQL());
						queryBuilder.setVisible(false);
					}
				});
			}
		});
		
		// finalise
		pack();
		GUIManager.getInstance().registerFrame(this);
	}
}
