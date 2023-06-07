package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * Drag and drop shape for a constant value in the SQL query language.
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class DataTypeShape extends AbstractShape {
	public static final int HEIGHT = 30;
	public static final int WIDTH = 65;
	
	public DataTypeShape(int x, int y, String name, Shape parent) {
		super(x, y, HEIGHT, WIDTH, name, parent);
	}
	
	private DataTypeShape(DataTypeShape copy) {
		this(copy.x, copy.y, copy.value, copy.parent);
	}
	
	public Shape duplicate() {
		return new DataTypeShape(this);
	}
	
	public Color getColour() {
		return BLUE;
	}
	
	@Override
	protected Font getFont() {
		return SMALL_FONT;
	}
	
	public Shape getDrag() {
		return new DataTypeShape(this) {
			@Override
			public Color getColour() {
				return BLUE_L;
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
