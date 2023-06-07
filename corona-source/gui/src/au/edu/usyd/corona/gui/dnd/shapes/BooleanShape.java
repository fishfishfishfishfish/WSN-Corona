package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Drag and drop shape for a Boolean operator in the SQL query language.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class BooleanShape extends AbstractShape {
	public static final int HEIGHT = 40;
	public static final int WIDTH = 65;
	public static final int ARC_WIDTH = 10;
	public static final int ARC_HEIGHT = 10;
	
	public BooleanShape(int x, int y, String name, Shape parent) {
		super(x, y, HEIGHT, WIDTH, name, parent);
	}
	
	private BooleanShape(BooleanShape copy) {
		this(copy.x, copy.y, copy.value, copy.parent);
	}
	
	public Shape duplicate() {
		return new BooleanShape(this);
	}
	
	public Color getColour() {
		return PALE_BLUE;
	}
	
	public Shape getDrag() {
		return new BooleanShape(this) {
			@Override
			public Color getColour() {
				return PALE_BLUE_L;
			}
		};
	}
	
	@Override
	public void _drawShape(Graphics2D g) {
		g.setColor(getColour());
		g.fillRoundRect(this.x, this.y, this.width, this.height, ARC_WIDTH, ARC_HEIGHT);
		g.setColor(getLineColour());
		g.drawRoundRect(this.x, this.y, this.width, this.height, ARC_WIDTH, ARC_HEIGHT);
	}
	
	public boolean canHaveDroppedOn(Shape shape) {
		return shape instanceof BooleanShape || shape instanceof ComparatorShape;
	}
	
	@Override
	public boolean _doDropOn(Shape shape) {
		if (canHaveDroppedOn(shape)) {
			if (appendChild(shape))
				return true;
		}
		return false;
	}
	
	public String getSQLFragment() {
		String x = "";
		if (hasModifier())
			return x = modifier.getSQLFragment();
		Shape[] children = getChildren();
		if (children[0] != null)
			x += children[0].getSQLFragment();
		x += " " + value + " ";
		if (children[1] != null)
			x += children[1].getSQLFragment();
		return x;
	}
}
