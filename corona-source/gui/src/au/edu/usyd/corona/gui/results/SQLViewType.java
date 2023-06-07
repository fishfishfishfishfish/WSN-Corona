package au.edu.usyd.corona.gui.results;


import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.IOException;
import java.io.Writer;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.SpringLayout;
import javax.swing.border.EtchedBorder;

import au.edu.usyd.corona.gui.GUIManager;
import au.edu.usyd.corona.gui.cache.CacheChangeListener;
import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.util.FormattingUtils;
import au.edu.usyd.corona.gui.util.SpringUtils;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;

/**
 * An information view of the results for a query providing information about
 * the query itself rather than the results it produced
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
@SuppressWarnings("serial")
class SQLViewType extends ViewType {
	public SQLViewType(int queryId) {
		super("Query Information", "images/sql.png", queryId);
	}
	
	@Override
	public AbstractResultsView createNewView() {
		return new SQLView();
	}
	
	private class SQLView extends AbstractResultsView {
		private final FrontSide front;
		
		SQLView() {
			super(SQLViewType.this.name);
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
			// do nothing
		}
		
		@Override
		protected void exportData(Writer out) throws IOException {
			// not implemented
		}
		
		/**
		 * Class for the front side of the histogram view
		 * 
		 * @author Tim Dawborn
		 */
		private class MyFrontSide extends FrontSide implements CacheChangeListener {
			private final JLabel runcountLabel;
			private final JLabel nextExecutionLabel;
			
			MyFrontSide() {
				final JPanel container = new JPanel(new SpringLayout());
				final Query query = getQuery();
				
				container.add(new JLabel("Query ID:"));
				container.add(new JLabel("" + query.getQueryID()));
				
				container.add(new JLabel("Query String:"));
				JTextArea sql = new JTextArea(query.getQuery());
				sql.setBorder(BorderFactory.createEtchedBorder(EtchedBorder.LOWERED));
				sql.setEditable(false);
				sql.setFont(MONOSPACE_FONT);
				sql.setLineWrap(true);
				sql.setMaximumSize(new Dimension(400, 200));
				sql.setWrapStyleWord(true);
				container.add(sql);
				
				container.add(new JLabel("User:"));
				User user = GUIManager.getInstance().getLoggedInUser();
				if (user.getAccessLevel() == AccessLevel.ADMIN)
					user = CacheManager.getInstance().getUserCache().getObject("", query.getUser().getId());
				container.add(new JLabel(user.getUsername()));
				
				container.add(new JLabel("Time of Submission:"));
				container.add(new JLabel(FormattingUtils.DATE_RENDERER.convert(query.getSubmittedTime())));
				
				container.add(new JLabel("Time of First Execution:"));
				container.add(new JLabel(FormattingUtils.DATE_RENDERER.convert(query.getFirstExecutionTime())));
				
				container.add(new JLabel("Time of Next Execution:"));
				container.add(nextExecutionLabel = new JLabel(FormattingUtils.DATE_RENDERER.convert(query.getExecutionTime())));
				
				container.add(new JLabel("Run Count Left:"));
				container.add(runcountLabel = new JLabel());
				updateRuncountLabel(query);
				
				SpringUtils.makeCompactGrid(container, 7, 2, 5, 5, 10, 5);
				final JPanel wrapper = new JPanel();
				wrapper.setLayout(new BoxLayout(wrapper, BoxLayout.Y_AXIS));
				wrapper.add(container);
				add(wrapper, BorderLayout.CENTER);
				
				CacheManager.getInstance().getQueryCache().addChangeListener(this);
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
			public boolean isDataExportable() {
				return false;
			}
			
			public void cacheChanged() {
				final Query query = getQuery();
				nextExecutionLabel.setText(FormattingUtils.DATE_RENDERER.convert(query.getExecutionTime()));
				updateRuncountLabel(query);
				repaint();
			}
			
			@Override
			protected void _dispose() {
				CacheManager.getInstance().getQueryCache().removeChangeListener(this);
			}
			
			private void updateRuncountLabel(Query query) {
				if (query.getRunCountTotal() == Integer.MIN_VALUE)
					runcountLabel.setText("\u221E");
				else
					runcountLabel.setText(query.getRunCountLeft() + " of " + query.getRunCountTotal());
			}
		}
		
	}
}
