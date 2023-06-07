package au.edu.usyd.corona.gui.dnd;


import java.util.Collections;
import java.util.Comparator;

import au.edu.usyd.corona.gui.dnd.shapes.Shape;

@SuppressWarnings("serial")
class SelectPanel extends WorkspaceEnvironment {
	SelectPanel(int width, int height) {
		super(width, height);
	}
	
	public String getSQLFragment() {
		String q = mainPanel.getSQLFragment();
		if (q.length() != 0)
			q = "SELECT " + q;
		return q;
	}
	
	@Override
	protected String getInstructions() {
		return INSTRUCTIONS + " Drag the attributes you want into the workspace area in order to have them selected. If none are in the area then all attributes are selected.";
	}
	
	protected class SelectSidePanel extends SidePanel {
		public SelectSidePanel(int width, int height) {
			super(width, height);
			// initialises the drag and drop components
			final int PADDING = 12;
			int y = PADDING;
			y += initAttributes(y, PADDING) + 2 * PADDING;
			//y += initAggregates(y, PADDING) + 2 * PADDING;
		}
	}
	
	protected class SelectMainPanel extends MainPanel {
		public SelectMainPanel(int width, int height) {
			super(width, height);
		}
		
		@Override
		public String getSQLFragment() {
			if (shapes.size() == 0)
				return "*";
			
			Collections.sort(shapes, new Comparator<Shape>() {
				public int compare(Shape o1, Shape o2) {
					int x1 = o1.getX();
					int x2 = o2.getX();
					return (x1 == x2) ? 0 : (x1 < x2 ? -1 : 1);
				}
			});
			
			StringBuffer b = new StringBuffer();
			for (int i = 0; i < shapes.size(); ++i) {
				if (i != 0)
					b.append(", ");
				b.append(shapes.get(i).getSQLFragment());
			}
			return b.toString();
		}
	}
	
	@Override
	protected MainPanel getMainPanel(int width, int height) {
		return new SelectMainPanel(width, height);
	}
	
	@Override
	protected SidePanel getSidePanel(int width, int height) {
		return new SelectSidePanel(width, height);
	}
}
