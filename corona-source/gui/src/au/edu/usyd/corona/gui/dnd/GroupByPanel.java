package au.edu.usyd.corona.gui.dnd;


@SuppressWarnings("serial")
class GroupByPanel extends WorkspaceEnvironment {
	public GroupByPanel(int width, int height) {
		super(width, height);
	}
	
	public String getSQLFragment() {
		String q = mainPanel.getSQLFragment();
		if (q.length() != 0)
			q = "GROUP BY " + q;
		return q;
	}
	
	@Override
	protected String getInstructions() {
		return INSTRUCTIONS + "  Drag the attributes you want into the workspace area in order to have them grouped on.";
	}
	
	protected class GroupBySidePanel extends SidePanel {
		public GroupBySidePanel(int width, int height) {
			super(width, height);
			// initialises the drag and drop components
			final int PADDING = 12;
			int y = PADDING;
			y += initAttributes(y, PADDING) + 2 * PADDING;
		}
	}
	
	protected class GroupByMainPanel extends MainPanel {
		public GroupByMainPanel(int width, int height) {
			super(width, height);
		}
		
		@Override
		public String getSQLFragment() {
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
		return new GroupByMainPanel(width, height);
	}
	
	@Override
	protected SidePanel getSidePanel(int width, int height) {
		return new GroupBySidePanel(width, height);
	}
}
