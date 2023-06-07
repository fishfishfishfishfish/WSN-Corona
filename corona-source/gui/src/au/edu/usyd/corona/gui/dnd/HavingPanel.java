package au.edu.usyd.corona.gui.dnd;


@SuppressWarnings("serial")
class HavingPanel extends WorkspaceEnvironment {
	HavingPanel(int width, int height) {
		super(width, height);
	}
	
	public String getSQLFragment() {
		String q = mainPanel.getSQLFragment();
		if (q.length() != 0)
			q = "HAVING " + q;
		return q;
	}
	
	@Override
	protected String getInstructions() {
		return INSTRUCTIONS + " Construct a tree to represent the WHERE clause in the SQL query.";
	}
	
	protected class HavingSidePanel extends SidePanel {
		public HavingSidePanel(int width, int height) {
			super(width, height);
			// initialises the drag and drop components
			final int PADDING = 12;
			int y = PADDING;
			y += initBooleans(y, PADDING) + 2 * PADDING;
			y += initComparators(y, PADDING) + 2 * PADDING;
			y += initOperators(y, PADDING) + 2 * PADDING;
			y += initAttributes(y, PADDING) + 2 * PADDING;
			//y += initAggregates(y, PADDING) + 2 * PADDING;
			y += initDataType(y, PADDING);
		}
	}
	
	protected class HavingMainPanel extends MainPanel {
		public HavingMainPanel(int width, int height) {
			super(width, height);
		}
		
		@Override
		public String getSQLFragment() {
			switch (shapes.size()) {
			case 0:
				return "";
			case 1:
				return shapes.iterator().next().getSQLFragment();
			default:
				return "<Invalid HAVING clause; a forest found, not a tree>";
			}
		}
	}
	
	@Override
	protected MainPanel getMainPanel(int width, int height) {
		return new HavingMainPanel(width, height);
	}
	
	@Override
	protected SidePanel getSidePanel(int width, int height) {
		return new HavingSidePanel(width, height);
	}
}
