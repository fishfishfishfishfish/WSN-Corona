package au.edu.usyd.corona.gui;


import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;

import javax.swing.JTable;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.TableColumnModel;
import javax.swing.table.TableModel;

/**
 * This class provides an abstract base for a {@link TableModel} whose contents
 * is provided by the caching layer provided in
 * {@link au.edu.usyd.corona.gui.cache}. It also supports sorting the table
 * contents by clicking on the column headers.
 * 
 * @author Tim Dawborn
 */
public abstract class CacheBasedSortableTableModel implements TableModel, MouseListener {
	private final Collection<WeakReference<TableModelListener>> listeners = new ArrayList<WeakReference<TableModelListener>>();
	private final TableColumnModel columnModel;
	private final String[] columnSQLNames;
	private final boolean[] columnSorting; // false => ascending, true => descending
	private int sortedColumn;
	protected String ordering;
	
	protected CacheBasedSortableTableModel(JTable table, String[] columnSQLNames, int defaultColumn, boolean sortDescending) {
		// sets up state
		this.columnModel = table.getColumnModel();
		this.columnSQLNames = columnSQLNames;
		this.columnSorting = new boolean[columnSQLNames.length];
		
		if (defaultColumn == -1) {
			sortedColumn = -1;
			ordering = "";
		}
		else if (defaultColumn < columnSQLNames.length) {
			// inits the sorting data
			sortedColumn = defaultColumn;
			columnSorting[defaultColumn] = sortDescending;
			ordering = columnSQLNames[sortedColumn] + " " + (columnSorting[sortedColumn] ? "DESC" : "ASC");
		}
		else
			return;
		
		// makes a listener the table
		table.getTableHeader().addMouseListener(this);
	}
	
	protected void updateOrdering(int newSortColumn) {
		if (newSortColumn == sortedColumn)
			columnSorting[sortedColumn] = !columnSorting[sortedColumn];
		else
			sortedColumn = newSortColumn;
		ordering = columnSQLNames[sortedColumn] + " " + (columnSorting[sortedColumn] ? "DESC" : "ASC");
		
		// update listeners
		updateListeners(new TableModelEvent(this));
	}
	
	public String getOrdering() {
		return ordering;
	}
	
	protected void updateListeners(TableModelEvent e) {
		synchronized (listeners) {
			for (WeakReference<TableModelListener> r : listeners)
				if (r.get() != null)
					r.get().tableChanged(e);
		}
	}
	
	public final void addTableModelListener(TableModelListener listener) {
		synchronized (listeners) {
			listeners.add(new WeakReference<TableModelListener>(listener));
		}
	}
	
	public final void removeTableModelListener(TableModelListener listener) {
		synchronized (listeners) {
			for (Iterator<WeakReference<TableModelListener>> it = listeners.iterator(); it.hasNext();) {
				WeakReference<TableModelListener> r = it.next();
				if (r.get() == listener) {
					it.remove();
					return;
				}
			}
		}
	}
	
	protected abstract boolean canSortOnColumn(int columnNumber);
	
	public void mouseClicked(MouseEvent e) {
		int nCols = columnModel.getColumnCount();
		int i;
		for (int width = i = 0; i != nCols; i++) {
			int cw = columnModel.getColumn(i).getWidth();
			if (width + cw > e.getX())
				break;
			width += cw;
		}
		
		// if we can sort on it, sort
		if (canSortOnColumn(i))
			updateOrdering(i);
	}
	
	public void mouseEntered(MouseEvent e) {
	}
	
	public void mouseExited(MouseEvent e) {
	}
	
	public void mousePressed(MouseEvent e) {
	}
	
	public void mouseReleased(MouseEvent e) {
	}
	
	public final int getColumnCount() {
		return columnSQLNames.length;
	}
	
	public final boolean isCellEditable(int rowIndex, int columnIndex) {
		return false;
	}
	
	public final void setValueAt(Object value, int rowIndex, int columnIndex) {
		// does nothing as model is uneditable
	}
	
	protected abstract void _dispose();
	
	public void dispose() {
		_dispose();
		listeners.clear();
		ordering = null;
	}
}
