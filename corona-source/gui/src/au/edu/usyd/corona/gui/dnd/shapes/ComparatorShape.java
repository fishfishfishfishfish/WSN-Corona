package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Drag and drop shape for a comparator in the SQL query language.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class ComparatorShape extends AbstractShape {
	public static final int SIZE = 45;
	
	public ComparatorShape(int x, int y, String name, Shape parent) {
		super(x, y, SIZE, SIZE, name, parent);
	}
	
	private ComparatorShape(ComparatorShape copy) {
		this(copy.x, copy.y, copy.value, copy.parent);
	}
	
	public Shape duplicate() {
		return new ComparatorShape(this);
	}
	
	public Color getColour() {
		return PURPLE;
	}
	
	public Shape getDrag() {
		return new ComparatorShape(this) {
			@Override
			public Color getColour() {
				return PURPLE_L;
			}
		};
	}
	
	@Override
	public void _drawShape(Graphics2D g) {
		g.setColor(getColour());
		g.fillOval(this.x, this.y, this.width, this.height);
		g.setColor(getLineColour());
		g.drawOval(this.x, this.y, this.width, this.height);
	}
	
	public boolean canHaveDroppedOn(Shape shape) {
		return shape instanceof OperatorShape || shape instanceof ComparatorShape || shape instanceof AttributeShape || shape instanceof DataTypeShape;
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
		String s = "";
		if (children[0] != null)
			s += children[0].getSQLFragment();
		s += " " + value + " ";
		if (children[1] != null)
			s += children[1].getSQLFragment();
		return s;
	}
}
