package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Graphics2D;

/**
 * Drag and drop shape for an operator in the SQL query language.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class OperatorShape extends AbstractShape {
	public static final int SIZE = 45;
	
	public OperatorShape(int x, int y, String name, Shape parent) {
		super(x, y, SIZE, SIZE, name, parent);
	}
	
	private OperatorShape(OperatorShape copy) {
		this(copy.x, copy.y, copy.value, copy.parent);
	}
	
	public Shape duplicate() {
		return new OperatorShape(this);
	}
	
	public Color getColour() {
		return RED;
	}
	
	public Shape getDrag() {
		return new OperatorShape(this) {
			@Override
			public Color getColour() {
				return RED_L;
			}
		};
	}
	
	@Override
	public void _drawShape(Graphics2D g) {
		int[] xPoints = {x + SIZE / 2, x + SIZE, x + SIZE / 2, x};
		int[] yPoints = {y, y + SIZE / 2, y + SIZE, y + SIZE / 2};
		g.setColor(getColour());
		g.fillPolygon(xPoints, yPoints, 4);
		g.setColor(getLineColour());
		g.drawPolygon(xPoints, yPoints, 4);
	}
	
	public boolean canHaveDroppedOn(Shape shape) {
		return shape instanceof AttributeShape || shape instanceof OperatorShape || shape instanceof DataTypeShape;
	}
	
	@Override
	public boolean _doDropOn(Shape shape) {
		if (canHaveDroppedOn(shape))
			return appendChild(shape);
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
