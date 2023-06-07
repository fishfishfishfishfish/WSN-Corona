package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Drag and drop shape for a table attribute (sensor) in the SQL query language.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class AttributeShape extends AbstractShape {
	public static final int HEIGHT = 40;
	public static final int WIDTH = 65;
	
	public AttributeShape(int x, int y, String name, Shape parent) {
		super(x, y, HEIGHT, WIDTH, name, parent);
	}
	
	private AttributeShape(AttributeShape copy) {
		this(copy.x, copy.y, copy.value, copy.parent);
	}
	
	public Shape duplicate() {
		return new AttributeShape(this);
	}
	
	public Color getColour() {
		return LGREEN;
	}
	
	@Override
	protected Font getFont() {
		return SMALL_FONT;
	}
	
	public Shape getDrag() {
		return new AttributeShape(this) {
			@Override
			public Color getColour() {
				return LGREEN_L;
			}
		};
	}
	
	@Override
	public void _drawShape(Graphics2D g) {
		g.setColor(getColour());
		g.fillRect(this.x, this.y, this.width, this.height);
		g.setColor(getLineColour());
		g.drawRect(this.x, this.y, this.width, this.height);
	}
	
	public boolean canHaveDroppedOn(Shape shape) {
		return shape instanceof AggregateShape;
	}
	
	@Override
	public boolean _doDropOn(Shape shape) {
		if (shape instanceof AggregateShape) {
			if (modifier != null)
				return false;
			modifier = shape;
			return true;
		}
		return false;
	}
	
	public String getSQLFragment() {
		if (hasModifier())
			return modifier.getSQLFragment() + "(" + value + ")";
		return value;
	}
}
