package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.io.IOException;
import java.io.Writer;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SpringLayout;

import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.CategoryLabelPositions;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.labels.StandardCategoryToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.renderer.category.LineAndShapeRenderer;
import org.jfree.data.category.DefaultCategoryDataset;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.cache.ResultCache;
import au.edu.usyd.corona.gui.util.DialogUtils;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.grammar.Query;

/**
 * A line view of the results for a query
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class LineViewType extends ViewType {
	private final Set<String> numericAttributes;
	private static final Set<String> NON_NUMERIC_ATTRIBS;
	
	static {
		NON_NUMERIC_ATTRIBS = new HashSet<String>();
		NON_NUMERIC_ATTRIBS.add("NODE");
		NON_NUMERIC_ATTRIBS.add("PARENT");
		NON_NUMERIC_ATTRIBS.add("TIME");
	}
	
	LineViewType(int queryId) {
		super("Line Graph", "images/line.png", queryId);
		
		// Get the column names
		numericAttributes = new HashSet<String>();
		String[] columnNames = new String[0];
		try {
			columnNames = GUIManager.getInstance().getRemoteSessionInterface().getAttributeNames();
		}
		catch (RemoteException e) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(e);
			return;
		}
		
		for (String column : columnNames) {
			if (!NON_NUMERIC_ATTRIBS.contains(column.toUpperCase()))
				numericAttributes.add(column.toUpperCase());
		}
	}
	
	@Override
	public AbstractResultsView createNewView() {
		return new LineView();
	}
	
	private class LineView extends AbstractResultsView {
		private final BackSide back;
		private final FrontSide front;
		private String xItem;
		private String groupItem;
		private JComboBox xAxis;
		private JCheckBox[] yAxisCheckboxes;
		private JComboBox grouping;
		private JCheckBox drawLine;
		private Map<String, Map<String, Double>> data; // { x axis value => { attribute => y axis value } }
		
		LineView() {
			super(LineViewType.this.name);
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
			xItem = null;
			groupItem = null;
			xAxis = null;
			yAxisCheckboxes = null;
			grouping = null;
			drawLine = null;
			if (data != null) {
				for (Map<String, Double> m : data.values())
					m.clear();
				data.clear();
				data = null;
			}
		}
		
		@Override
		protected void exportData(Writer out) throws IOException {
			// data export disabled
		}
		
		/**
		 * Class for the back side of the histogram view
		 * 
		 * @author Tim Dawborn
		 * @author Glen Pink
		 */
		private class MyBackSide extends BackSide {
			private final ResultCache resultCache;
			private final String[] columnNames;
			private final String[] prettyNames;
			
			MyBackSide() {
				setLayout(new BorderLayout());
				resultCache = CacheManager.getInstance().getResultCache();
				
				final Query query = getQuery();
				final String sql = "SELECT * FROM TABLE_" + query.getQueryID();
				columnNames = resultCache.getColumnNames(query, sql);
				prettyNames = new String[columnNames.length];
				for (int i = 0; i != columnNames.length; i++)
					prettyNames[i] = FormattingUtils.convertAggreate(columnNames[i]);
				
				// select the x-axis
				final JPanel xAxisPanel = new JPanel();
				xAxis = new JComboBox(prettyNames);
				xAxisPanel.setBorder(BorderFactory.createTitledBorder("Data for X axis"));
				xAxisPanel.add(new JLabel("X axis"));
				xAxisPanel.add(xAxis);
				
				// select the attributes to be plotted (y-axis for consistancy)
				final JPanel yAxisPanel = new JPanel();
				yAxisPanel.setLayout(new SpringLayout());
				yAxisPanel.add(new JLabel("Select data to plot on Y axis"));
				yAxisPanel.add(new JLabel());
				yAxisCheckboxes = new JCheckBox[columnNames.length];
				for (int i = 0; i != yAxisCheckboxes.length; i++) {
					yAxisPanel.add(new JLabel(prettyNames[i]));
					yAxisPanel.add(yAxisCheckboxes[i] = new JCheckBox());
					yAxisCheckboxes[i].setEnabled(numericAttributes.contains(columnNames[i]) || columnNames[i].contains("_"));
				}
				SpringUtils.makeCompactGrid(yAxisPanel, 1 + yAxisCheckboxes.length, 2, 5, 5, 5, 5);
				
				// select the grouping
				final JPanel groupingPanel = new JPanel();
				grouping = new JComboBox();
				grouping.addItem("No grouping");
				for (String s : prettyNames)
					grouping.addItem(s);
				groupingPanel.setBorder(BorderFactory.createTitledBorder("Grouping data"));
				groupingPanel.add(new JLabel("Group on:"));
				groupingPanel.add(grouping);
				
				// select weather or not to draw a line
				final JPanel linePanel = new JPanel();
				drawLine = new JCheckBox();
				drawLine.setSelected(true);
				linePanel.setBorder(BorderFactory.createTitledBorder("Draw Line"));
				linePanel.add(new JLabel("Connect points? "));
				linePanel.add(drawLine);
				
				// group components and add to display
				final JPanel container = new JPanel();
				container.setLayout(new BoxLayout(container, BoxLayout.Y_AXIS));
				container.add(xAxisPanel);
				
				final JPanel container3 = new JPanel();
				container3.setLayout(new BoxLayout(container3, BoxLayout.Y_AXIS));
				container3.add(groupingPanel);
				container3.add(linePanel);
				container3.add(Box.createVerticalGlue());
				
				final JPanel yAxisContainer = new JPanel();
				yAxisContainer.setBorder(BorderFactory.createTitledBorder("Data for Y axis"));
				yAxisContainer.add(yAxisPanel);
				
				final JPanel container2 = new JPanel(new GridLayout(1, 2));
				container2.add(yAxisContainer);
				container2.add(container3);
				container.add(container2);
				
				add(container, BorderLayout.CENTER);
			}
			
			@Override
			public boolean okClicked() {
				xItem = columnNames[xAxis.getSelectedIndex()];
				groupItem = grouping.getSelectedIndex() == 0 ? "No grouping" : columnNames[grouping.getSelectedIndex() - 1];
				
				// makes sure at least one y axis attribute was checked
				int numChecked = 0;
				for (JCheckBox b : yAxisCheckboxes)
					if (b.isSelected())
						numChecked++;
				if (numChecked == 0) {
					DialogUtils.showMultilineWarning(this, "Line View Error", "One or more Y-axis attributes need to be selected to graph");
					return false;
				}
				
				data = new HashMap<String, Map<String, Double>>();
				final Query query = getQuery();
				
				// fetch X axis data
				List<Object[]> _xAxisData = resultCache.getColumnsByFields(query, xItem);
				final List<String> xAxisData = new ArrayList<String>(_xAxisData.size());
				for (Object[] o : _xAxisData) {
					String xValue = FormattingUtils.renderAttribute(xItem, o[0]);
					if (!data.containsKey(xValue)) {
						xAxisData.add(xValue);
						data.put(xValue, new HashMap<String, Double>());
					}
				}
				
				//need to either put all the data into coodinates
				if (groupItem.equals("No grouping")) {
					addData(xAxisData, "", "");
				}
				else { // Or do it by grouping.
					// Get the groups.
					List<Object[]> beforeGroups = resultCache.getColumnsByFields(query, groupItem);
					Set<Object> groups = new HashSet<Object>();
					for (Object[] r : beforeGroups)
						groups.add(r[0]);
					
					// get the data for each group
					for (Object group : groups) {
						String brackets = " [" + FormattingUtils.convertAggreate(groupItem) + ": " + FormattingUtils.renderAttribute(groupItem, group) + "]";
						addData(xAxisData, "WHERE " + groupItem + " = " + group, brackets);
					}
				}
				
				return true;
			}
			
			private void addData(List<String> xAxisData, String whereClause, String attributeSuffix) {
				for (int i = 0; i != yAxisCheckboxes.length; i++) {
					if (!yAxisCheckboxes[i].isSelected())
						continue;
					
					final Query query = getQuery();
					final String sql = String.format("SELECT %s FROM TABLE_%d %s", columnNames[i], query.getQueryID(), whereClause);
					List<Object[]> yAxisData = resultCache.getAllResults(query, sql);
					for (int j = 0; j < xAxisData.size() && j < yAxisData.size(); j++) {
						Object o = yAxisData.get(j)[0];
						double value;
						if (o instanceof Boolean)
							value = ((Boolean) o).booleanValue() ? 1 : 0;
						else
							value = Double.parseDouble(o.toString());
						data.get(xAxisData.get(j)).put(prettyNames[i] + attributeSuffix, value);
					}
				}
			}
			
			@Override
			protected void _dispose() {
				// do nothing
			}
		}
		
		/**
		 * Class for the front side of the histogram view
		 * 
		 * @author Tim Dawborn
		 * @author Glen Pink
		 */
		private class MyFrontSide extends FrontSide {
			@Override
			public void update() {
				DefaultCategoryDataset dataset = new DefaultCategoryDataset();
				for (Entry<String, Map<String, Double>> xDataEntry : data.entrySet()) {
					for (Entry<String, Double> e : xDataEntry.getValue().entrySet()) {
						dataset.addValue(e.getValue(), e.getKey(), xDataEntry.getKey());
					}
				}
				
				CategoryAxis categoryAxis = new CategoryAxis();
				categoryAxis.setCategoryLabelPositions(CategoryLabelPositions.UP_45);
				
				ValueAxis valueAxis = new NumberAxis("Value");
				
				LineAndShapeRenderer renderer = new LineAndShapeRenderer(drawLine.isSelected(), true);
				renderer.setBaseToolTipGenerator(new StandardCategoryToolTipGenerator());
				
				CategoryPlot plot = new CategoryPlot(dataset, categoryAxis, valueAxis, renderer);
				plot.setOrientation(PlotOrientation.VERTICAL);
				JFreeChart chart = new JFreeChart("Line Chart", JFreeChart.DEFAULT_TITLE_FONT, plot, true);
				
				updateChart(chart);
			}
			
			@Override
			public boolean isDataExportable() {
				return false;
			}
			
			@Override
			protected void _dispose() {
				// do nothing
			}
		}
	}
}
