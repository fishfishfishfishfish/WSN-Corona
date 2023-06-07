package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.rmi.RemoteException;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JToolBar;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.session.notifier.NotifierID;
import au.edu.usyd.corona.server.session.notifier.RemoteNotifier;

/**
 * A frame for viewing the results of a query
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
public class QueryResultFrame extends JFrame implements DrawingConstants {
	public static final int MIN_HEIGHT = 700;
	public static final int MIN_WIDTH = 900;
	
	private final QueryResultPanel resultsPanel;
	private final ViewType[] viewTypes;
	private final int queryId;
	
	// variables for adding and removing result notifications
	private RemoteNotifier contextRemoteNotifier;
	private NotifierID contextNotifierId;
	
	public QueryResultFrame(int queryId) {
		GUIManager.getInstance().registerFrame(this);
		
		this.queryId = queryId;
		
		// frame properties
		Dimension d = new Dimension(MIN_WIDTH, MIN_HEIGHT);
		setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		setLayout(new BorderLayout());
		setMinimumSize(d);
		setPreferredSize(d);
		setTitle("Results for Query #" + queryId);
		
		// close window button
		final JButton closeWindowButton = new JButton("Close Window");
		closeWindowButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				dispose();
			}
		});
		
		// setup the view types
		viewTypes = new ViewType[6];
		viewTypes[0] = new HistogramViewType(queryId);
		viewTypes[1] = new LineViewType(queryId);
		viewTypes[2] = new PieChartViewType(queryId);
		viewTypes[3] = new TabularViewType(queryId);
		viewTypes[4] = new SQLViewType(queryId);
		viewTypes[5] = new NetworkTreeViewType(queryId);
		
		// add the toolbar and the close button
		final JPanel header = new JPanel(new BorderLayout());
		header.add(new ViewsToolBar(4), BorderLayout.WEST);
		
		final JPanel header2 = new JPanel(new BorderLayout());
		header2.add(closeWindowButton, BorderLayout.PAGE_END);
		header.add(header2, BorderLayout.EAST);
		add(header, BorderLayout.PAGE_START);
		
		// add main window component
		add(resultsPanel = new QueryResultPanel(), BorderLayout.CENTER);
		
		// add the default view
		resultsPanel.addNewView(new SQLViewType(queryId).createNewView());
		
		// finalize
		pack();
	}
	
	@Override
	public void dispose() {
		Query q = CacheManager.getInstance().getQueryCache().getObject("queryID DESC", queryId);
		CacheManager.getInstance().getResultCache().removeCachesForQuery(q);
		try {
			GUIManager.getInstance().getRemoteSessionInterface().removeNotifier(contextNotifierId, contextRemoteNotifier);
		}
		catch (RemoteException err) {
			GUIManager.getInstance().getRemoteExceptionNotifier().notifyHandler(err);
		}
		
		resultsPanel.dispose();
		contextNotifierId = null;
		contextRemoteNotifier = null;
		System.gc();
		
		super.dispose();
	}
	
	private class ViewsToolBar extends JToolBar {
		ViewsToolBar(int... seperatorIndicies) {
			super(HORIZONTAL);
			setLayout(new FlowLayout(FlowLayout.LEFT));
			setFloatable(false);
			
			for (int i = 0; i != viewTypes.length; i++) {
				// adds in a separator if need be
				for (int sep : seperatorIndicies)
					if (i == sep)
						addSeparator();
				// adds the toolbar icon to the toolbar
				final JButton toolbarIcon = viewTypes[i].getToolbarIcon();
				toolbarIcon.addActionListener(new ToolbarButtonListener(viewTypes[i]));
				add(toolbarIcon);
			}
		}
	}
	
	private class ToolbarButtonListener implements ActionListener {
		private final ViewType viewType;
		
		public ToolbarButtonListener(ViewType viewType) {
			this.viewType = viewType;
		}
		
		public void actionPerformed(ActionEvent e) {
			resultsPanel.addNewView(viewType.createNewView());
		}
	}
}
