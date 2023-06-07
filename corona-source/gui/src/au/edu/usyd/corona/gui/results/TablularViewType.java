package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.io.IOException;
import java.io.Writer;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;

import au.edu.usyd.corona.gui.CacheBasedSortableTableModel;
import au.edu.usyd.corona.gui.cache.CacheChangeListener;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.cache.ResultCache;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.gui.util.TableUtils;
import au.edu.usyd.corona.server.grammar.Query;

/**
 * A tabular view of the results for a query, presenting the data in the most
 * basic tabular raw form
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class TabularViewType extends ViewType {
	private final ResultCache cache;
	
	TabularViewType(int queryId) {
		super("Tablular", "images/table.png", queryId);
		cache = CacheManager.getInstance().getResultCache();
	}
	
	@Override
	public AbstractResultsView createNewView() {
		return new TabularView();
	}
	
	private class TabularView extends AbstractResultsView {
		private final FrontSide front;
		private final String sql;
		private MyTableModel model;
		private JTable table;
		private JLabel rowCountLabel;
		
		TabularView() {
			super(TabularViewType.this.name);
			sql = "SELECT * FROM TABLE_" + getQuery().getQueryID();
			front = new MyFrontSide();
		}
		
		public BackSide getBackSide() {
			return null;
		}
		
		public FrontSide getFrontSide() {
			return front;
		}
		
		@Override
		protected void _dispose() {
			model.dispose();
			model = null;
			table = null;
			rowCountLabel = null;
		}
		
		@Override
		protected void exportData(Writer out) throws IOException {
			final int nCols = model.getColumnCount();
			
			// column headers
			for (int c = 0; c < nCols; c++) {
				if (c != 0)
					out.write(',');
				out.write(model.getColumnName(c));
			}
			out.write('\n');
			
			// table data
			final int nRows = model.getRowCount();
			for (int r = 0; r < nRows; r++) {
				for (int c = 0; c < nCols; c++) {
					if (c != 0)
						out.write(',');
					out.write(model.getValueAt(r, c).toString());
				}
				out.write('\n');
			}
		}
		
		/**
		 * Class for the front side of the histogram view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyFrontSide extends FrontSide {
			MyFrontSide() {
				// sets up the table
				table = new JTable();
				model = new MyTableModel(table, getQuery());
				table.setModel(model);
				table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
				TableUtils.calcColumnWidths(table);
				
				// sets the cell renderers
				TableColumnModel columns = table.getColumnModel();
				for (int i = 0; i < model.getColumnCount(); i++) {
					String columnName = model.getColumnName(i);
					if (FormattingUtils.hasRenderer(columnName))
						columns.getColumn(i).setCellRenderer(FormattingUtils.getRenderer(columnName));
				}
				
				// allows for scroll bars
				JScrollPane pane = new JScrollPane(table);
				
				// adds a label showing the number of rows in the table
				rowCountLabel = new JLabel(model.getRowCount() + " Rows");
				final JPanel bottomContainer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
				bottomContainer.add(rowCountLabel);
				
				final JPanel container = new JPanel(new BorderLayout());
				container.add(pane, BorderLayout.CENTER);
				container.add(bottomContainer, BorderLayout.SOUTH);
				add(container, BorderLayout.CENTER);
			}
			
			@Override
			public void update() {
			}
			
			@Override
			public boolean isImageExportable() {
				return false;
			}
			
			@Override
			public boolean isEditable() {
				return false;
			}
			
			@Override
			protected void _dispose() {
				// do nothing
			}
		}
		
		private class MyTableModel extends CacheBasedSortableTableModel implements CacheChangeListener {
			private final String[] columnNames;
			private final Class<?>[] columnClasses;
			private int rowCount;
			
			public MyTableModel(JTable table, Query query) {
				super(table, cache.getColumnNames(query, sql), -1, false);
				
				// keep local copy of cache items
				columnNames = cache.getColumnNames(query, sql);
				columnClasses = cache.getAttributes(query, sql);
				rowCount = cache.getNumRows(query, sql);
				
				// makes aggregates display nicely
				for (int i = 0; i != columnNames.length; i++)
					columnNames[i] = FormattingUtils.convertAggreate(columnNames[i]);
				
				// subscribe to cache changes so we update automatically
				cache.addChangeListener(this);
			}
			
			public Class<?> getColumnClass(int columnIndex) {
				return columnClasses[columnIndex];
			}
			
			public String getColumnName(int columnIndex) {
				return columnNames[columnIndex];
			}
			
			public int getRowCount() {
				return rowCount;
			}
			
			public Object getValueAt(int rowIndex, int columnIndex) {
				return cache.getResult(getQuery(), sql + (ordering.length() == 0 ? "" : " ORDER BY " + ordering), rowIndex)[columnIndex];
			}
			
			@Override
			protected boolean canSortOnColumn(int columnNumber) {
				return true;
			}
			
			public void cacheChanged() {
				rowCount = cache.getNumRows(getQuery(), sql);
				rowCountLabel.setText(rowCount + " Rows");
				updateListeners(new TableModelEvent(this));
			}
			
			@Override
			protected void _dispose() {
				cache.removeChangeListener(this);
			}
		}
	}
}
