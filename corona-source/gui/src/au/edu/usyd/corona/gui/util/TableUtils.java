package au.edu.usyd.corona.gui.util;


import java.awt.Component;

import javax.swing.JTable;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

public abstract class TableUtils {
	private TableUtils() {
		// hide constructor
	}
	
	/*
	 * http://www.chka.de/swing/table/cell-sizes.html Changed to only take values
	 * from the first row rather than traverse the whole table, as all our data
	 * is very simililar on a per-column level
	 */
	public static void calcColumnWidths(JTable table) {
		JTableHeader header = table.getTableHeader();
		TableCellRenderer defaultHeaderRenderer = (header == null) ? null : header.getDefaultRenderer();
		TableColumnModel columns = table.getColumnModel();
		TableModel data = table.getModel();
		
		int margin = columns.getColumnMargin(); // only JDK1.3
		int rowCount = data.getRowCount();
		int totalWidth = 0;
		
		for (int i = columns.getColumnCount() - 1; i >= 0; --i) {
			TableColumn column = columns.getColumn(i);
			int columnIndex = column.getModelIndex();
			int width = -1;
			TableCellRenderer h = column.getHeaderRenderer();
			if (h == null)
				h = defaultHeaderRenderer;
			if (h != null) {
				Component c = h.getTableCellRendererComponent(table, column.getHeaderValue(), false, false, -1, i);
				width = c.getPreferredSize().width;
			}
			
			int row = rowCount - 1;
			if (row >= 0) {
				TableCellRenderer r = table.getCellRenderer(row, i);
				Component c = r.getTableCellRendererComponent(table, data.getValueAt(row, columnIndex), false, false, row, i);
				width = Math.max(width, c.getPreferredSize().width);
			}
			
			if (width >= 0)
				column.setPreferredWidth(width + margin); // <1.3: without margin
				
			totalWidth += column.getPreferredWidth();
		}
	}
}
