package au.edu.usyd.corona.gui.results;


import java.awt.Image;

import javax.swing.ImageIcon;
import javax.swing.JButton;

import au.edu.usyd.corona.gui.cache.CacheManager;
import au.edu.usyd.corona.gui.util.DrawingConstants;
import au.edu.usyd.corona.server.grammar.Query;

/**
 * A class to contain the main side icons for creating "view"'s of query data
 * 
 * @author Tim Dawborn
 */
abstract class ViewType implements DrawingConstants {
	public static final int IMAGE_WIDTH = 40;
	public static final int IMAGE_HEIGHT = 40;
	
	protected final String name;
	protected final int queryId;
	private final JButton toolbarIcon;
	
	protected boolean mouseOver = false;
	
	protected ViewType(String name, String fileName, int queryId) {
		this.name = name;
		this.queryId = queryId;
		
		// the view type is represented visually just by the button on the toolbar
		toolbarIcon = new JButton();
		toolbarIcon.setIcon(new ImageIcon(new ImageIcon(fileName).getImage().getScaledInstance(IMAGE_WIDTH, IMAGE_HEIGHT, Image.SCALE_SMOOTH)));
		toolbarIcon.setToolTipText(name);
	}
	
	protected Query getQuery() {
		return CacheManager.getInstance().getQueryCache().getObject("queryID DESC", queryId);
	}
	
	public JButton getToolbarIcon() {
		return toolbarIcon;
	}
	
	/**
	 * Called when the view is asked to create a new view instance
	 * 
	 * @return the displayable view item for this conceptual view
	 */
	public abstract AbstractResultsView createNewView();
}
