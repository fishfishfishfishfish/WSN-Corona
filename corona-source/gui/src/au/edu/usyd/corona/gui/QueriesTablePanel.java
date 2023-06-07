package au.edu.usyd.corona.gui;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.rmi.RemoteException;
import java.util.Date;
import java.util.logging.Logger;

import javax.swing.*;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableColumnModel;

import au.edu.usyd.corona.gui.cache.CacheChangeListener;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.cache.QueryCache;
import au.edu.usyd.corona.gui.results.QueryResultFrame;
import au.edu.usyd.corona.gui.util.DialogUtils;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.session.QueryExecuteException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * This class is the base class for any tabular list of queries shown on the
 * GUI. This table has five columns; the query ID number, the status of the
 * query, the date the query was fired, the user/owner of the query, and the
 * query itself.
 * 
 * @author Tim Dawborn
 */
@SuppressWarnings("serial")
class QueriesTablePanel extends JPanel implements DrawingConstants {
	private static final Icon ICON_RUNNING = new ImageIcon("images/icons/running.png");
	private static final Icon ICON_KILLED = new ImageIcon("images/icons/killed.png");
	private static final Icon ICON_COMPLETE = new ImageIcon("images/icons/completed.png");
	private static final Icon[] ICONS = {ICON_RUNNING, ICON_RUNNING, ICON_KILLED, ICON_COMPLETE};
	
	private static final String[] COLUMNS = {"", "ID", "User", "Date", "Query"};
	private static final String[] COLUMNS_SQL = {"status", "queryID", "username", "submittedTime", "query"};
	private static final Class<?>[] CLASSES = {Icon.class, Integer.class, User.class, Date.class, String.class};
	
	private static final Logger logger = Logger.getLogger(QueriesTablePanel.class.getCanonicalName());
	
	private final QueryTableModel model;
	private final JTable table; // the table itself
	private final QueryCache cache;
	
	private final ActionListener viewResultsActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			showQueryResults();
		}
	};
	private final ActionListener rerunQueryActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			rerunQuery();
		}
	};
	private final ActionListener killQueryActionListener = new ActionListener() {
		public void actionPerformed(ActionEvent e) {
			killQuery();
		}
	};
	
	QueriesTablePanel() {
		setLayout(new BorderLayout());
		cache = CacheManager.getInstance().getQueryCache();
		
		// creates the table itself
		table = new JTable();
		model = new QueryTableModel(table);
		table.setModel(model);
		table.setAutoResizeMode(JTable.AUTO_RESIZE_SUBSEQUENT_COLUMNS);
		table.setComponentPopupMenu(new MyPopupMenu());
		table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseClicked(MouseEvent e) {
				if (e.getClickCount() == 2 && e.getButton() == MouseEvent.BUTTON1)
					showQueryResults();
			}
		});
		
		// set default sizings
		TableColumnModel columns = table.getColumnModel();
		columns.getColumn(0).setMinWidth(22); // the status indicator
		columns.getColumn(0).setPreferredWidth(22);
		columns.getColumn(0).setMaxWidth(22);
		
		columns.getColumn(1).setMinWidth(40); // the query id
		columns.getColumn(1).setPreferredWidth(40);
		
		columns.getColumn(2).setPreferredWidth(100); // the user
		columns.getColumn(3).setPreferredWidth(200); // the date
		columns.getColumn(4).setPreferredWidth(500); // the query
		
		// set the cell renderers
		columns.getColumn(2).setCellRenderer(FormattingUtils.USER_RENDERER);
		columns.getColumn(3).setCellRenderer(FormattingUtils.DATE_RENDERER);
		columns.getColumn(4).setCellRenderer(FormattingUtils.QUERY_RENDERER);
		
		// wraps in a scroll pane to get scrolling
		JScrollPane pane = new JScrollPane(table);
		table.getTableHeader().setPreferredSize(new Dimension(table.getRowHeight(), table.getRowHeight())); // hack for Windows
		add(pane, BorderLayout.CENTER);
		
		// adds the footer buttons
		final JPanel footer = new JPanel(new FlowLayout(FlowLayout.RIGHT));
		add(footer, BorderLayout.PAGE_END);
		
		// button to add a new query
		JButton button = new JButton("New Query");
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				SQLEntryFrame f = new SQLEntryFrame();
				f.setVisible(true);
			}
		});
		footer.add(button);
		
		// button to view the results of the currently selected query
		button = new JButton("View Results");
		button.addActionListener(viewResultsActionListener);
		footer.add(button);
		
		// rerun query button
		button = new JButton("Re-run");
		button.addActionListener(rerunQueryActionListener);
		footer.add(button);
		
		// button to kill a query
		button = new JButton("Kill");
		button.addActionListener(killQueryActionListener);
		footer.add(button);
	}
	
	private Query rerunQuery() {
		// attempts to re-run the query
		try {
			int row = table.getSelectedRow();
			if (row != -1) {
				final Query q = cache.getObjectAtIndex(model.getOrdering(), row);
				return GUIManager.getInstance().getRemoteSessionInterface().executeQuery(q.getQuery());
			}
		}
		catch (QueryExecuteException err) {
			DialogUtils.showMultilineError(this, "Query Error", "An error occured while executing your query:\n" + err.getMessage());
			logger.severe(err.getMessage());
		}
		catch (RemoteException err) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(err);
		}
		return new Query(); // temp value while handling error
	}
	
	private void killQuery() {
		try {
			int row = table.getSelectedRow();
			if (row != -1) {
				final Query killedQuery = cache.getObjectAtIndex(model.getOrdering(), row);
				final int ret = JOptionPane.showConfirmDialog(QueriesTablePanel.this, "Are you sure you want to kill query #" + killedQuery.getQueryID(), "Kill Query?", JOptionPane.OK_CANCEL_OPTION);
				if (ret == JOptionPane.YES_OPTION) {
					GUIManager.getInstance().getRemoteSessionInterface().executeQuery("KILL " + killedQuery.getQueryID());
				}
			}
		}
		catch (QueryExecuteException err) {
			DialogUtils.showMultilineError(this, "Query Error", "An error occured while executing your query:\n" + err.getMessage());
			logger.severe(err.getMessage());
		}
		catch (RemoteException err) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(err);
		}
	}
	
	private void showQueryResults(Query query) {
		QueryResultFrame viewer = new QueryResultFrame(query.getQueryID());
		viewer.setVisible(true);
	}
	
	private void showQueryResults() {
		// get the selected row (if any)
		int row = table.getSelectedRow();
		if (row != -1)
			showQueryResults(cache.getObjectAtIndex(model.getOrdering(), row));
	}
	
	private class QueryTableModel extends CacheBasedSortableTableModel implements CacheChangeListener {
		private QueryTableModel(JTable table) {
			super(table, COLUMNS_SQL, 1, true);
			cache.addChangeListener(this);
		}
		
		public final String getColumnName(int columnIndex) {
			return COLUMNS[columnIndex];
		}
		
		public final Class<?> getColumnClass(int columnIndex) {
			return CLASSES[columnIndex];
		}
		
		public int getRowCount() {
			return cache.getSize(ordering);
		}
		
		public Object getValueAt(int rowIndex, int columnIndex) {
			final Query q = cache.getObjectAtIndex(ordering, rowIndex);
			switch (columnIndex) {
			case 0:
				return ICONS[q.getStatus()];
			case 1:
				return q.getQueryID();
			case 2:
				User me = GUIManager.getInstance().getLoggedInUser();
				if (me.getAccessLevel() == AccessLevel.USER)
					return me;
				else
					return CacheManager.getInstance().getUserCache().getObject("", q.getUser().getId());
			case 3:
				return q.getSubmittedTime();
			default:
				return q.getQuery();
			}
		}
		
		@Override
		protected boolean canSortOnColumn(int columnNumber) {
			return true;
		}
		
		public void cacheChanged() {
			int row = table.getSelectedRow();
			table.repaint();
			// forward notification to the model listeners
			updateListeners(new TableModelEvent(this));
			if (row >= 0)
				table.setRowSelectionInterval(row, row);
		}
		
		@Override
		protected void _dispose() {
			// do nothing
		}
	}
	
	private class MyPopupMenu extends JPopupMenu {
		public MyPopupMenu() {
			JMenuItem item = new JMenuItem("View Results for the Query");
			item.addActionListener(viewResultsActionListener);
			add(item);
			
			item = new JMenuItem("Re-run the Query");
			item.addActionListener(rerunQueryActionListener);
			add(item);
			
			item = new JMenuItem("Kill the Query");
			item.addActionListener(killQueryActionListener);
			add(item);
		}
	}
}
