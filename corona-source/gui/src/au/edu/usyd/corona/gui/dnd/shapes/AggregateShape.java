package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Drag and drop shape for an aggregate function in the SQL query language.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class AggregateShape extends AbstractShape {
	public static final int HEIGHT = 40;
	public static final int WIDTH = 65;
	
	public AggregateShape(int x, int y, String name, Shape parent) {
		super(x, y, HEIGHT, WIDTH, name, parent);
	}
	
	private AggregateShape(AggregateShape copy) {
		this(copy.x, copy.y, copy.value, copy.parent);
	}
	
	public Shape duplicate() {
		return new AggregateShape(this);
	}
	
	public Color getColour() {
		return PALE_YELLOW;
	}
	
	@Override
	protected Font getFont() {
		return MEDIUM_FONT;
	}
	
	public Shape getDrag() {
		return new AggregateShape(this) {
			@Override
			public Color getColour() {
				return PALE_YELLOW_L;
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
		return false;
	}
	
	@Override
	public boolean _doDropOn(Shape shape) {
		return false;
	}
	
	public String getSQLFragment() {
		return value;
	}
}
