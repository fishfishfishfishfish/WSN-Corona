package au.edu.usyd.corona.gui.dnd;


import java.awt.Dimension;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.ArrayList;
import java.util.Collection;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;


@SuppressWarnings("serial")
class PropertiesPanel extends JPanel implements SQLFragmentGenerator {
	private static final int DEFAULT_STARTTIME_SECONDS = 5;
	private static final int DEFAULT_STARTTIME_MINUTES = 0;
	private static final int DEFAULT_STARTTIME_HOURS = 0;
	
	private static final int DEFAULT_EPOCH_SECONDS = 0;
	private static final int DEFAULT_EPOCH_MINUTES = 0;
	private static final int DEFAULT_EPOCH_HOURS = 0;
	
	private static final int DEFAULT_RUNCOUNT = 1;
	
	private final Collection<SQLFragmentListener> listeners = new ArrayList<SQLFragmentListener>();
	
	private final JTextField tRunCount;
	private final JSpinner sEHours, sEMinutes, sESeconds;
	private final JSpinner sSTHours, sSTMinutes, sSTSeconds;
	
	PropertiesPanel(int width, int height) {
		Dimension size = new Dimension(width, height);
		setSize(size);
		setPreferredSize(size);
		setMaximumSize(size);
		setMinimumSize(size);
		
		setLayout(new BoxLayout(this, BoxLayout.Y_AXIS));
		OurListener listener = new OurListener(this);
		
		// run count
		JPanel runCountPanel = new JPanel();
		runCountPanel.setBorder(BorderFactory.createTitledBorder("Run Count"));
		
		JLabel lRunCount = new JLabel("Run Count");
		tRunCount = new JTextField(Integer.toString(DEFAULT_RUNCOUNT));
		tRunCount.addKeyListener(listener);
		tRunCount.addFocusListener(listener);
		tRunCount.setPreferredSize(new Dimension(100, 20));
		
		lRunCount.setLabelFor(tRunCount);
		runCountPanel.add(lRunCount);
		runCountPanel.add(tRunCount);
		
		add(runCountPanel);
		
		// epoch
		JPanel epochPanel = new JPanel();
		epochPanel.setBorder(BorderFactory.createTitledBorder("Epoch"));
		
		JLabel lEHours = new JLabel("Hours");
		sEHours = new JSpinner(new SpinnerNumberModel(DEFAULT_EPOCH_HOURS, 0, 100, 1));
		sEHours.addChangeListener(listener);
		lEHours.setLabelFor(sEHours);
		epochPanel.add(lEHours);
		epochPanel.add(sEHours);
		
		JLabel lEMinutes = new JLabel("Minutes");
		sEMinutes = new JSpinner(new SpinnerNumberModel(DEFAULT_EPOCH_MINUTES, 0, 60, 1));
		sEMinutes.addChangeListener(listener);
		lEMinutes.setLabelFor(sEMinutes);
		epochPanel.add(lEMinutes);
		epochPanel.add(sEMinutes);
		
		JLabel lESeconds = new JLabel("Seconds");
		sESeconds = new JSpinner(new SpinnerNumberModel(DEFAULT_EPOCH_SECONDS, 0, 60, 1));
		sESeconds.addChangeListener(listener);
		lESeconds.setLabelFor(sESeconds);
		epochPanel.add(lESeconds);
		epochPanel.add(sESeconds);
		
		add(epochPanel);
		
		// start time
		JPanel startTimePanel = new JPanel();
		startTimePanel.setBorder(BorderFactory.createTitledBorder("Start Time"));
		
		JLabel lSTHours = new JLabel("Hours");
		sSTHours = new JSpinner(new SpinnerNumberModel(DEFAULT_STARTTIME_HOURS, 0, 100, 1));
		sSTHours.addChangeListener(listener);
		lSTHours.setLabelFor(sSTHours);
		startTimePanel.add(lSTHours);
		startTimePanel.add(sSTHours);
		
		JLabel lSTMinutes = new JLabel("Minutes");
		sSTMinutes = new JSpinner(new SpinnerNumberModel(DEFAULT_STARTTIME_MINUTES, 0, 60, 1));
		sSTMinutes.addChangeListener(listener);
		lSTMinutes.setLabelFor(sSTMinutes);
		startTimePanel.add(lSTMinutes);
		startTimePanel.add(sSTMinutes);
		
		JLabel lSTSeconds = new JLabel("Seconds");
		sSTSeconds = new JSpinner(new SpinnerNumberModel(DEFAULT_STARTTIME_SECONDS, 0, 60, 1));
		sSTSeconds.addChangeListener(listener);
		lSTSeconds.setLabelFor(sSTSeconds);
		startTimePanel.add(lSTSeconds);
		startTimePanel.add(sSTSeconds);
		
		add(startTimePanel);
	}
	
	private boolean spinnerEqual(JSpinner spinner, int value) {
		return ((Integer) spinner.getValue()).intValue() == value;
	}
	
	private boolean getTimeSQL(StringBuffer b, JSpinner spinner, String singular, String plural) {
		int x = (Integer) spinner.getValue();
		if (x != 0) {
			b.append(x);
			b.append(' ');
			b.append((x == 1) ? singular : plural);
			return true;
		}
		return false;
	}
	
	private void getTimeSQL(StringBuffer b, JSpinner hours, JSpinner minutes, JSpinner seconds) {
		if (getTimeSQL(b, hours, "hour", "hours"))
			b.append(' ');
		if (getTimeSQL(b, minutes, "hour", "minutes"))
			b.append(' ');
		if (getTimeSQL(b, seconds, "second", "seconds"))
			b.append(' ');
	}
	
	public String getSQLFragment() {
		StringBuffer b = new StringBuffer();
		
		if (!(spinnerEqual(sSTHours, DEFAULT_STARTTIME_HOURS) && spinnerEqual(sSTMinutes, DEFAULT_STARTTIME_MINUTES) && spinnerEqual(sSTSeconds, DEFAULT_STARTTIME_SECONDS))) {
			b.append("START IN ");
			getTimeSQL(b, sSTHours, sSTMinutes, sSTSeconds);
			b.append('\n');
		}
		
		if (!(spinnerEqual(sEHours, DEFAULT_EPOCH_HOURS) && spinnerEqual(sEMinutes, DEFAULT_EPOCH_MINUTES) && spinnerEqual(sESeconds, DEFAULT_EPOCH_SECONDS))) {
			b.append("EPOCH ");
			getTimeSQL(b, sEHours, sEMinutes, sESeconds);
			b.append('\n');
		}
		
		if (!tRunCount.getText().equals("1")) {
			b.append("RUNCOUNT ");
			b.append(tRunCount.getText());
			b.append('\n');
		}
		
		return b.toString();
	}
	
	public void subscribeSQLFragmentGeneratorListener(SQLFragmentListener listener) {
		listeners.add(listener);
	}
	
	private class OurListener implements KeyListener, ChangeListener, FocusListener {
		private final SQLFragmentGenerator generator;
		
		public OurListener(SQLFragmentGenerator generator) {
			this.generator = generator;
		}
		
		private void updateSQL() {
			for (SQLFragmentListener x : listeners)
				x.actUponChange(generator);
		}
		
		public void keyPressed(KeyEvent e) {
		}
		
		public void keyReleased(KeyEvent e) {
			updateSQL();
		}
		
		public void keyTyped(KeyEvent e) {
		}
		
		public void stateChanged(ChangeEvent e) {
			updateSQL();
		}
		
		public void focusGained(FocusEvent e) {
		}
		
		public void focusLost(FocusEvent e) {
			updateSQL();
		}
	}
}
