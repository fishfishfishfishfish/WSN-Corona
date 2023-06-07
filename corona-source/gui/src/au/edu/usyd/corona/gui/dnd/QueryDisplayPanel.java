package au.edu.usyd.corona.gui.dnd;


import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.border.EtchedBorder;

import au.edu.usyd.corona.gui.util.DrawingConstants;

@SuppressWarnings("serial")
class QueryDisplayPanel extends JPanel implements DrawingConstants {
	private final JTextArea textArea;
	private final JButton closeButton;
	
	private String query = "";
	
	QueryDisplayPanel(ActionListener closeButtonActionListener) {
		setLayout(new BoxLayout(this, BoxLayout.X_AXIS));
		setBorder(BorderFactory.createEmptyBorder(0, 7, 7, 7));
		
		// setup the text area to display the generated query
		textArea = new JTextArea(3, 80);
		textArea.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
		textArea.setEditable(false);
		textArea.setFont(MONOSPACE_FONT);
		textArea.setLineWrap(true);
		textArea.setWrapStyleWord(true);
		add(textArea);
		
		// add a close button to close down the query builder
		closeButton = new JButton("Done");
		closeButton.addActionListener(closeButtonActionListener);
		add(closeButton);
	}
	
	public void setQuery(String query) {
		this.query = query;
		textArea.setText(query);
	}
	
	public String getQuery() {
		return query;
	}
}
