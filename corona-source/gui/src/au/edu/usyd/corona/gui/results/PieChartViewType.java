package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.SpringLayout;

import org.jfree.chart.ChartFactory;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PiePlot3D;
import org.jfree.data.general.DefaultPieDataset;

import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.cache.ResultCache;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.grammar.Query;

/**
 * A pie chart view of the results for a query
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings({"serial", "unchecked"})
class PieChartViewType extends ViewType {
	PieChartViewType(int queryId) {
		super("Pie Chart", "images/pie.png", queryId);
	}
	
	@Override
	public AbstractResultsView createNewView() {
		return new PieChartView();
	}
	
	private class PieChartView extends AbstractResultsView {
		private final BackSide back;
		private final FrontSide front;
		
		private ButtonGroup radioButtonGroup;
		private JRadioButton[] radioButtons;
		private String[] fields;
		
		private String dataField;
		private String sql;
		private final Map<Comparable, Integer> dataFrequencies = new HashMap<Comparable, Integer>();
		private Object[][] data;
		
		PieChartView() {
			super(PieChartViewType.this.name);
			back = new MyBackSide();
			front = new MyFrontSide();
		}
		
		public BackSide getBackSide() {
			return back;
		}
		
		public FrontSide getFrontSide() {
			return front;
		}
		
		@Override
		protected void _dispose() {
			radioButtonGroup = null;
			radioButtons = null;
			fields = null;
			dataField = null;
			sql = null;
			data = null;
			dataFrequencies.clear();
		}
		
		@Override
		protected void exportData(Writer out) throws IOException {
			// column header
			out.write(dataField);
			out.write('\n');
			
			// table data
			for (Object o : data) {
				out.write(o.toString());
				out.write('\n');
			}
		}
		
		/**
		 * Class for the back side of the pie chart view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyBackSide extends BackSide {
			private final ResultCache resultCache;
			
			MyBackSide() {
				setLayout(new BorderLayout());
				resultCache = CacheManager.getInstance().getResultCache();
				
				final Query query = getQuery();
				String colSql = "SELECT * FROM TABLE_" + query.getQueryID();
				int nColumns = resultCache.getNumCols(query, colSql);
				String[] columnNames = resultCache.getColumnNames(query, colSql);
				
				// select the attributes to be plotted (y-axis for consistancy)
				final JPanel yAxisPanel = new JPanel();
				final JLabel yAxisLabel = new JLabel("Graph Attribute");
				yAxisPanel.setLayout(new SpringLayout());
				yAxisPanel.add(yAxisLabel);
				yAxisPanel.add(new JLabel());
				radioButtonGroup = new ButtonGroup();
				radioButtons = new JRadioButton[nColumns];
				fields = new String[nColumns];
				for (int i = 0; i < radioButtons.length; i++) {
					radioButtons[i] = new JRadioButton();
					radioButtons[i].setSelected(i == 0);
					radioButtonGroup.add(radioButtons[i]);
					fields[i] = columnNames[i];
					yAxisPanel.add(new JLabel(FormattingUtils.convertAggreate(columnNames[i])));
					yAxisPanel.add(radioButtons[i]);
				}
				SpringUtils.makeCompactGrid(yAxisPanel, 1 + radioButtons.length, 2, 5, 5, 5, 5);
				
				// group components and add to display
				final JPanel container = new JPanel();
				container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
				container.setBorder(BorderFactory.createTitledBorder("Pie Chart Data"));
				container.add(yAxisPanel);
				add(container, BorderLayout.CENTER);
			}
			
			@Override
			public boolean okClicked() {
				// find the selected attribute
				final Query query = getQuery();
				for (int i = 0; i < radioButtons.length; i++) {
					if (radioButtons[i].isSelected()) {
						dataField = fields[i];
						sql = "SELECT " + dataField + " FROM TABLE_" + query.getQueryID();
						data = resultCache.getAllResults(query, sql).toArray(new Object[][]{});
						break;
					}
				}
				
				// generate a frequency map for that attribute's data
				dataFrequencies.clear();
				for (Object[] o : data) {
					final Comparable group = (Comparable) o[0];
					Integer count = dataFrequencies.get(group);
					dataFrequencies.put(group, (count == null ? 0 : count) + 1);
				}
				
				return true;
			}
			
			@Override
			protected void _dispose() {
				// do nothing
			}
		}
		
		/**
		 * Class for the front side of the pie chart view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyFrontSide extends FrontSide {
			@Override
			public void update() {
				DefaultPieDataset data = new DefaultPieDataset();
				
				// sorts the keys before inserting
				final List<Comparable> keys = new ArrayList(dataFrequencies.keySet());
				Collections.sort(keys);
				int i = 0;
				for (Comparable key : keys)
					data.insertValue(i++, FormattingUtils.renderAttribute(dataField, key), dataFrequencies.get(key));
				
				// creates the 3D plot and sets up some graphical options
				JFreeChart chart = ChartFactory.createPieChart3D(FormattingUtils.convertAggreate(dataField) + " Values", data, true, true, false);
				PiePlot3D x = (PiePlot3D) chart.getPlot();
				x.setCircular(false);
				x.setDarkerSides(true);
				x.setForegroundAlpha(0.75f);
				
				// updates the chart on the gui
				updateChart(chart);
			}
			
			@Override
			protected void _dispose() {
				// do nothing
			}
		}
	}
}
