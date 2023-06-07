package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Graphics2D;

/**
 * The background grouping bubbles
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public class GroupingShape extends AbstractShape {
	public static final int PADDING = 4;
	public static final double ROOT3_ON_3 = Math.sqrt(3) / 3.0;
	public static final double ROOT2_ON_2 = Math.sqrt(2) / 2.0;
	
	protected final Shape node;
	protected float radius = 0.0f;
	
	public GroupingShape(Shape node) {
		super(0, 0, 0, 0, null, null);
		this.node = node;
		updateBubble();
		this.width = (int) (2 * radius);
		this.height = (int) (2 * radius);
	}
	
	public Shape duplicate() {
		return new GroupingShape(node);
	}
	
	public Color getColour() {
		return new Color(152, 195, 255 - (node.calculateDepth() * 20));
		//return BUBBLE_COLOUR;
	}
	
	public Shape getDrag() {
		return new GroupingShape(node) {
			@Override
			public Color getColour() {
				return BUBBLE_COLOUR;
			}
		};
	}
	
	public void doMagic() {
		debug("doMagic for " + node.getValue());
		positionChildren();
		updateBubble();
		if (!node.isRoot())
			node.getParent().getGrouper().doMagic();
	}
	
	public void updateBubble() {
		debug("updateBubble for " + node.getValue());
		radius = getRadius(node);
		debug("updateBubble radius=" + radius);
		
		// sets the AbstractShape properties reflecting the change in radius
		width = (int) (2 * radius);
		height = (int) (2 * radius);
		
		// updates the position of the bubble
		updateBubblePosition();
	}
	
	public void updateBubblePosition() {
		float[] centre = node.getCentrePoint();
		debug("updateBubblePosition for " + node.getValue());
		debug("updateBubblePosition radius=" + radius + " centreX=" + centre[0] + " centreY=" + centre[1]);
		
		if (node.isLeaf()) {
			x = (int) (centre[0] - radius);
			y = (int) (centre[1] - radius);
			debug("updateBubblePosition isLeaf x=" + x + " y=" + y);
		}
		else if (node.hasModifier()) {
			System.err.println("updatePosition hasModifier STUB");
		}
		else {
			final float alpha = getRadiusLeaf(node);
			final float delta = getDelta(node);
			
			debug("updateBubblePosition children alpha=" + alpha + " delta=" + delta);
			
			double dy = Math.sqrt(alpha * alpha + 2 * alpha * delta);// - delta * ROOT3_ON_3;
			
			debug("updateBubblePosition children dy=" + dy);
			
			x = (int) (centre[0] - radius);
			y = (int) (centre[1] + dy - radius);
			y = node.getY() - PADDING - (int) (delta - alpha);
			debug("updateBubblePosition isTree x=" + x + " y=" + y);
		}
	}
	
	/**
	 * Returns the radius of a leaf shape to be used by the groupers
	 * 
	 * @param shape the leaf shape
	 * @return the grouper radius
	 */
	private static float getRadiusLeaf(Shape shape) {
		return (float) (Math.sqrt(Math.pow(shape.getWidth() / 2.0, 2) + Math.pow(shape.getHeight() / 2.0, 2)));
	}
	
	private static float getRadius(Shape shape) {
		if (shape.isLeaf())
			return getRadiusLeaf(shape) + PADDING / 2;
		
		Shape[] children = new Shape[]{shape.getChildren()[0], shape.getChildren()[1]};
		if (children[0] == null) {
			children[0] = children[1];
			children[1] = null;
		}
		
		float size = getRadiusLeaf(shape) + PADDING / 2;
		size = Math.max(size, getRadius(children[0]));
		if (children[1] != null)
			size = Math.max(size, getRadius(children[1]));
		return 2 * size;
	}
	
	private static float getDelta(Shape shape) {
		return _getDelta(shape, shape);
	}
	
	private static float _getDelta(Shape shape, Shape original) {
		if (shape.isLeaf())
			return getRadiusLeaf(shape) + PADDING / 2;
		
		Shape[] children = new Shape[]{shape.getChildren()[0], shape.getChildren()[1]};
		if (children[0] == null) {
			children[0] = children[1];
			children[1] = null;
		}
		
		float size = getRadiusLeaf(shape) + PADDING / 2;
		size = Math.max(size, _getDelta(children[0], original));
		if (children[1] != null)
			size = Math.max(size, _getDelta(children[1], original));
		return ((shape == original) ? 1 : 2) * size;
	}
	
	@Override
	public void _drawShape(Graphics2D g) {
		g.setColor(getColour());
		g.fillOval(x, y, width, height);
	}
	
	public boolean canHaveDroppedOn(Shape shape) {
		return false;
	}
	
	@Override
	public boolean _doDropOn(Shape shape) {
		return false;
	}
	
	/**
	 * Positions the children of the parent of the grouper
	 */
	private void positionChildren() {
		debug("positionChildren for " + node.getValue());
		debug("positionChildren isLeaf=" + node.isLeaf());
		if (node.isLeaf()) {
			return;
		}
		
		final float alpha = getRadiusLeaf(node);
		final float delta = getDelta(node);
		debug("positionChildren alpha=" + alpha + " delta=" + delta);
		
		float deltaX = delta;
		float deltaY = (float) Math.sqrt(alpha * alpha + 2 * alpha * delta);
		
		debug("positionChildren dx=" + deltaX + " dy=" + deltaY);
		
		float[] centre = node.getCentrePoint();
		int sign = -1;
		
		debug("positionChildren centreX=" + centre[0] + " centreY=" + centre[1]);
		
		for (Shape child : node.getChildren()) {
			if (child != null) {
				float childRadius = getDelta(child);
				if (child.isLeaf())
					childRadius -= PADDING;
				debug("positionChildren childRadius=" + childRadius);
				child.setX((int) (centre[0] + sign * deltaX - ROOT2_ON_2 * childRadius));
				child.setY((int) (centre[1] + deltaY - ROOT2_ON_2 * childRadius));
				child.getGrouper().updateBubblePosition();
				child.getGrouper().positionChildren();
			}
			sign = 1;
		}
	}
	
	public String getSQLFragment() {
		return null;
	}
	
	private static void debug(String message) {
		//		System.err.print(System.currentTimeMillis());
		//		System.err.print(": ");
		//		System.err.println(message);
	}
}
