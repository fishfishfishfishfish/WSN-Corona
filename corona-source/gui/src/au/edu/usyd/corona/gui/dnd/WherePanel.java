package au.edu.usyd.corona.gui.dnd;


@SuppressWarnings("serial")
class WherePanel extends WorkspaceEnvironment {
	
	WherePanel(int width, int height) {
		super(width, height);
	}
	
	public String getSQLFragment() {
		String q = mainPanel.getSQLFragment();
		if (q.length() != 0)
			q = "WHERE " + q;
		return q;
	}
	
	@Override
	protected String getInstructions() {
		return INSTRUCTIONS + " Construct a tree to represent the WHERE clause in the SQL query.";
	}
	
	protected class WhereSidePanel extends SidePanel {
		public WhereSidePanel(int width, int height) {
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
	
	protected class WhereMainPanel extends MainPanel {
		public WhereMainPanel(int width, int height) {
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
				return "<Invalid WHERE clause; a forest found, not a tree>";
			}
		}
	}
	
	@Override
	protected MainPanel getMainPanel(int width, int height) {
		return new WhereMainPanel(width, height);
	}
	
	@Override
	protected SidePanel getSidePanel(int width, int height) {
		return new WhereSidePanel(width, height);
	}
}
