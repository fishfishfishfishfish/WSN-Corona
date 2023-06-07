package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import javax.swing.JPanel;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;

/**
 * This provides an interface to any view of a set of results. All views must
 * have a ResultsContext which is used to uniquely identify one particular set
 * of results from the database
 * 
 * @author Tim Dawborn
 */
interface ResultsView {
	/**
	 * @return the panel to render on the front side of a view
	 */
	public FrontSide getFrontSide();
	
	/**
	 * @return the panel to render on the back side of a view
	 */
	public BackSide getBackSide();
	
	public void exportData(File file) throws IOException;
	
	/**
	 * @return the name of the type of view
	 */
	public String getName();
	
	public void dispose();
	
	/**
	 * Abstract class for the front side of a view; all views should have a
	 * subclass of this class to represent their own front side
	 * 
	 * @author Tim Dawborn
	 */
	@SuppressWarnings("serial")
	public abstract class FrontSide extends JPanel {
		protected ChartPanel chart;
		protected boolean first;
		
		protected FrontSide() {
			setLayout(new BorderLayout());
			first = true;
		}
		
		protected abstract void _dispose();
		
		public void dispose() {
			_dispose();
			chart = null;
		}
		
		protected final void updateChart(JFreeChart chart) {
			if (first) {
				this.chart = new ChartPanel(chart);
				add(this.chart, BorderLayout.CENTER);
				first = false;
			}
			else {
				this.chart.setChart(chart);
			}
		}
		
		/**
		 * Called by the view renderer after the properties have been changed. The
		 * result of this call should be that the data that the view presents
		 * should be updated to reflect the changes in the properties
		 */
		public abstract void update();
		
		/**
		 * @return if the view can export its data to a CSV dump
		 */
		public boolean isDataExportable() {
			return true;
		}
		
		/**
		 * @return if the view can export an image representation of itself
		 */
		public boolean isImageExportable() {
			return true;
		}
		
		public boolean isEditable() {
			return true;
		}
		
		public void doExportImage() {
			try {
				chart.doSaveAs();
			}
			catch (IOException e) {
				Logger.getLogger(getClass().getName()).throwing("FrontSide", "doExportImage", e);
			}
		}
	}
	
	@SuppressWarnings("serial")
	public abstract class BackSide extends JPanel {
		public abstract boolean okClicked();
		
		protected abstract void _dispose();
		
		public void dispose() {
			_dispose();
		}
	}
}
