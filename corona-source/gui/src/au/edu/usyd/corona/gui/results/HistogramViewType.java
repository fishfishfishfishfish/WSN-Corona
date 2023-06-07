package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.data.category.DefaultCategoryDataset;

import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.util.DialogUtils;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.grammar.Query;

/**
 * A histogram view of the results for a query
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings({"serial", "unchecked"})
class HistogramViewType extends ViewType {
	public HistogramViewType(int queryId) {
		super("Histogram", "images/histogram.png", queryId);
	}
	
	@Override
	public AbstractResultsView createNewView() {
		return new HistogramView();
	}
	
	private class HistogramView extends AbstractResultsView {
		private final BackSide back;
		private final FrontSide front;
		private JTextField binsTextfield;
		private JCheckBox[] yAxisCheckboxes;
		private Map<Comparable, Integer>[] data;
		private String[] fields;
		private int numBins = 5;
		private int numRows = 0;
		
		HistogramView() {
			super(HistogramViewType.this.name);
			back = new MyBackSide();
			front = new MyFrontSide();
		}
		
		@Override
		protected void _dispose() {
			yAxisCheckboxes = null;
			fields = null;
			if (data != null) {
				for (Map<Comparable, Integer> m : data)
					m.clear();
				data = null;
			}
		}
		
		public BackSide getBackSide() {
			return back;
		}
		
		public FrontSide getFrontSide() {
			return front;
		}
		
		@Override
		protected void exportData(Writer out) throws IOException {
			// disabled
		}
		
		/**
		 * Class for the back side of the histogram view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyBackSide extends BackSide {
			private final String[] columnNames;
			
			MyBackSide() {
				setLayout(new BorderLayout());
				
				final Query q = getQuery();
				columnNames = CacheManager.getInstance().getResultCache().getColumnNames(q, "SELECT * FROM TABLE_" + q.getQueryID());
				
				// select the number of bins
				final JPanel binsPanel = new JPanel();
				binsPanel.setBorder(BorderFactory.createTitledBorder("Histogram Bins"));
				binsPanel.add(new JLabel("Number of bins"));
				binsPanel.add(binsTextfield = new JTextField("" + numBins, 2));
				
				// select the attributes to be plotted (y-axis for consistancy)
				final JPanel yAxisPanel = new JPanel();
				yAxisPanel.setLayout(new SpringLayout());
				yAxisPanel.add(new JLabel("Graph Attributes"));
				yAxisPanel.add(new JLabel());
				yAxisCheckboxes = new JCheckBox[columnNames.length];
				for (int i = 0; i < yAxisCheckboxes.length; i++) {
					yAxisPanel.add(new JLabel(FormattingUtils.convertAggreate(columnNames[i])));
					yAxisPanel.add(yAxisCheckboxes[i] = new JCheckBox());
				}
				SpringUtils.makeCompactGrid(yAxisPanel, 1 + yAxisCheckboxes.length, 2, 5, 5, 5, 5);
				
				// group components and add to display
				final JPanel container = new JPanel();
				container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
				container.add(binsPanel);
				final JPanel container2 = new JPanel();
				container2.setBorder(BorderFactory.createTitledBorder("Histogram Data"));
				container2.add(yAxisPanel);
				container.add(container2);
				add(container, BorderLayout.CENTER);
			}
			
			@Override
			public boolean okClicked() {
				// makes sure the number of bins is a number
				try {
					numBins = Integer.parseInt(binsTextfield.getText());
				}
				catch (NumberFormatException e) {
					DialogUtils.showMultilineWarning(this, "Histogram View Error", "The number of histogram bins '" + binsTextfield.getText() + "' is not a valid number.");
					return false;
				}
				
				// makes sure at least one y axis attribute was checked
				int numChecked = 0;
				for (JCheckBox b : yAxisCheckboxes)
					if (b.isSelected())
						numChecked++;
				if (numChecked == 0) {
					DialogUtils.showMultilineWarning(this, "Histogram View Error", "One or more attributes need to be selected to graph.");
					return false;
				}
				
				// fetch out the new data
				data = new Map[numChecked];
				fields = new String[numChecked];
				numRows = 0;
				for (int i = 0, k = 0; i < yAxisCheckboxes.length; i++) {
					if (!yAxisCheckboxes[i].isSelected())
						continue;
					
					final String columnName = columnNames[i];
					//					fields[k] = FormattingUtils.convertAggreate(columnName);
					fields[k] = columnName;
					data[k] = new HashMap<Comparable, Integer>();
					
					List<Object[]> row = CacheManager.getInstance().getResultCache().getColumnsByFields(getQuery(), columnName);
					for (Object[] o : row) {
						Comparable<?> x = (Comparable<?>) o[0];
						Integer count = data[k].get(x);
						data[k].put(x, (count == null) ? 1 : count + 1);
					}
					numRows += row.size();
					k++;
				}
				
				return true;
			}
			
			@Override
			protected void _dispose() {
				// nothing to do 
			}
		}
		
		/**
		 * Class for the front side of the histogram view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyFrontSide extends FrontSide {
			@Override
			public void update() {
				DefaultCategoryDataset dataset = new DefaultCategoryDataset();
				for (int i = 0; i != fields.length; i++) {
					// sort the values to be displayed on the X axis
					final List<Comparable> values = new ArrayList<Comparable>(data[i].keySet());
					Collections.sort(values);
					
					// split the values into the number of required bins
					final LinkedHashMap<String, Double> binnedData = new LinkedHashMap<String, Double>();
					if (numBins > values.size())
						numBins = values.size();
					final double factor = (values.size() * 1.0) / numBins;
					double x = 0, x2 = 0;
					int ix = 0, ix2 = 0;
					for (int j = 0; j != numBins; j++) {
						x2 += factor;
						ix = (int) Math.floor(x);
						ix2 = (int) Math.floor(x2);
						String key;
						if (ix2 - ix == 1)
							key = FormattingUtils.renderAttribute(fields[i], values.get(ix));
						else
							key = FormattingUtils.renderAttribute(fields[i], values.get(ix)) + " - " + FormattingUtils.renderAttribute(fields[i], values.get(ix2 - 1));
						int sum = 0;
						for (int k = ix; k != ix2; k++)
							sum += data[i].get(values.get(k));
						binnedData.put(key, (sum * 100.0) / numRows);
						x = x2;
					}
					
					// add the data to the graph
					for (Entry<String, Double> e : binnedData.entrySet())
						dataset.addValue(e.getValue(), e.getKey(), FormattingUtils.convertAggreate(fields[i]));
				}
				
				JFreeChart chart = ChartFactory.createBarChart3D("Histogram", "Values", "Frequency", dataset, PlotOrientation.VERTICAL, true, true, false);
				updateChart(chart);
			}
			
			@Override
			public boolean isDataExportable() {
				return false;
			}
			
			@Override
			protected void _dispose() {
				// nothing to do
			}
		}
	}
}
