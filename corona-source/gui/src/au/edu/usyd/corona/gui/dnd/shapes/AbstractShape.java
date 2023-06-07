package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.font.TextLayout;
import java.awt.geom.AffineTransform;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.util.Iterator;
import java.util.Stack;

import au.edu.usyd.corona.gui.dnd.SQLFragmentListener;

abstract class AbstractShape implements Shape {
	protected int x;
	protected int y;
	protected int height;
	protected int width;
	
	protected String value;
	
	protected final Shape[] children = new Shape[2];
	protected Shape parent;
	protected Shape modifier = null;
	
	protected GroupingShape grouper;
	protected boolean grouperVisible = false;
	
	protected Color lineColour;
	
	protected AbstractShape(int x, int y, int height, int width, String value, Shape parent) {
		this.x = x;
		this.y = y;
		this.height = height;
		this.width = width;
		this.value = value;
		this.parent = parent;
		this.lineColour = BLACK;
		if (this instanceof GroupingShape)
			this.grouper = null;
		else
			this.grouper = new GroupingShape(this);
	}
	
	protected Font getFont() {
		return FONT;
	}
	
	public Color getLineColour() {
		return lineColour;
	}
	
	protected abstract void _drawShape(Graphics2D g);
	
	public void recurseDrawGrouper(Graphics2D g) {
		if (grouper != null && grouperVisible)
			grouper._drawShape(g);
		if (children[0] != null)
			children[0].recurseDrawGrouper(g);
		if (children[1] != null)
			children[1].recurseDrawGrouper(g);
	}
	
	public void recurseDrawLinks(Graphics2D g) {
		_drawLinks(g);
		if (children[0] != null)
			children[0].recurseDrawLinks(g);
		if (children[1] != null)
			children[1].recurseDrawLinks(g);
	}
	
	public void recurseDrawShape(Graphics2D g) {
		_drawShape(g);
		//		
		//		float[] c = getCentrePoint();
		//		g.setColor(BLACK);
		//		g.drawLine((int) (c[0] - 50), (int) c[1], (int) (c[0] + 50), (int) c[1]);
		//		g.drawLine((int) c[0], (int) (c[1] - 50), (int) c[0], (int) (c[1] + 50));
		
		if (children[0] != null)
			children[0].recurseDrawShape(g);
		if (children[1] != null)
			children[1].recurseDrawShape(g);
	}
	
	public void recurseDrawValue(Graphics2D g) {
		_drawValue(g);
		if (children[0] != null)
			children[0].recurseDrawValue(g);
		if (children[1] != null)
			children[1].recurseDrawValue(g);
	}
	
	public final void draw(Graphics2D g) {
		recurseDrawGrouper(g);
		recurseDrawLinks(g);
		recurseDrawShape(g);
		recurseDrawValue(g);
	}
	
	protected void _drawValue(Graphics2D g) {
		Font f = getFont();
		g.setColor(BLACK);
		g.setFont(f);
		TextLayout layout = new TextLayout(value, f, g.getFontRenderContext());
		Rectangle2D bounds = layout.getBounds();
		
		// calculate panel centre
		float cx = x + width / 2;
		float cy = y + height / 2;
		
		// add the adjustment for the texts bounds
		cx += bounds.getWidth() / 2 - (bounds.getX() + bounds.getWidth());
		cy += bounds.getHeight() / 2 - (bounds.getY() + bounds.getHeight());
		
		// draw at adjusted centre
		layout.draw(g, cx, cy);
	}
	
	protected void _drawLinks(Graphics2D g) {
		g.setColor(getLineColour());
		for (Shape s : children) {
			if (s == null)
				continue;
			float[] p1 = getCentrePoint();
			float[] p2 = s.getCentrePoint();
			g.drawLine((int) p1[0], (int) p1[1], (int) p2[0], (int) p2[1]);
		}
	}
	
	public void setGrouperVisible(boolean x) {
		grouperVisible = x;
		setLineColour((grouperVisible) ? WHITE : BLACK);
	}
	
	public void setLineColour(Color colour) {
		lineColour = colour;
		if (children[0] != null)
			children[0].setLineColour(colour);
		if (children[1] != null)
			children[1].setLineColour(colour);
	}
	
	protected void updateGrouper() {
		grouper.doMagic();
	}
	
	public GroupingShape getGrouper() {
		return grouper;
	}
	
	protected boolean appendChild(Shape child) {
		if (children[0] == null)
			children[0] = child;
		else if (children[1] == null)
			children[1] = child;
		else
			return false;
		child.setParent(this);
		return true;
	}
	
	protected abstract boolean _doDropOn(Shape shape);
	
	public final boolean doDropOn(Shape shape) {
		boolean successfull = _doDropOn(shape);
		if (successfull)
			updateGrouper();
		return successfull;
	}
	
	// =========================
	// Needed by java.awt.Shape
	// =========================
	public Rectangle getBounds() {
		return new Rectangle(x, y, width, height);
	}
	
	public Rectangle2D getBounds2D() {
		return new Rectangle2D.Float(x, y, width, height);
	}
	
	public boolean contains(Point2D p) {
		return contains(p.getX(), p.getY());
	}
	
	public boolean contains(Rectangle2D r) {
		return getBounds().contains(r);
	}
	
	public boolean contains(double x, double y) {
		return (x >= this.x) && (x <= this.x + this.width) && (y >= this.y) && (y <= this.y + this.height);
	}
	
	public boolean contains(double x, double y, double w, double h) {
		return getBounds().contains(new Rectangle((int) x, (int) y, width, height));
	}
	
	public boolean intersects(Rectangle2D r) {
		return getBounds().intersects(r);
	}
	
	public boolean intersects(double x, double y, double w, double h) {
		return getBounds().intersects(new Rectangle((int) x, (int) y, width, height));
	}
	
	public PathIterator getPathIterator(AffineTransform at) {
		System.err.println("STUB: getPathIterator(AffineTransform)");
		return null;
	}
	
	public PathIterator getPathIterator(AffineTransform at, double flatness) {
		System.err.println("STUB: getPathIterator(AffineTransform, double)");
		return null;
	}
	
	// ====================
	// Getters and Setters
	// ====================
	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	
	public void setPoint(int x, int y) {
		this.x = x;
		this.y = y;
	}
	
	public String getValue() {
		return value;
	}
	
	public void setValue(String value) {
		this.value = value;
	}
	
	public int getHeight() {
		return height;
	}
	
	public void setHeight(int height) {
		this.height = height;
	}
	
	public int getWidth() {
		return width;
	}
	
	public void setWidth(int width) {
		this.width = width;
	}
	
	public float[] getCentrePoint() {
		return new float[]{x + width / 2.0f, y + height / 2.0f};
	}
	
	public void updatePoint(int dx, int dy) {
		x += dx;
		y += dy;
		if (children[0] != null)
			children[0].updatePoint(dx, dy);
		if (children[1] != null)
			children[1].updatePoint(dx, dy);
	}
	
	public void endMovement() {
		grouper.updateBubblePosition();
		if (children[0] != null)
			children[0].endMovement();
		if (children[1] != null)
			children[1].endMovement();
	}
	
	public Shape[] getChildren() {
		return children;
	}
	
	public void setChildren(Shape[] children) {
		this.children[0] = children[0];
		this.children[1] = children[1];
		updateGrouper();
	}
	
	public void removeChild(Shape child) {
		if (children[0] == child)
			children[0] = null;
		else if (children[1] == child)
			children[1] = null;
		else
			return;
		updateGrouper();
	}
	
	public Shape getParent() {
		return parent;
	}
	
	public void setParent(Shape parent) {
		this.parent = parent;
	}
	
	public Iterator<Shape> iterator() {
		return new ShapeTreeIterator(this);
	}
	
	public boolean isLeaf() {
		return children[0] == null && children[1] == null;
	}
	
	public boolean isRoot() {
		return parent == null;
	}
	
	public int calculateDepth() {
		if (isRoot())
			return 0;
		return 1 + parent.calculateDepth();
	}
	
	public Shape getRoot() {
		return (isRoot()) ? this : parent.getRoot();
	}
	
	public boolean hasModifier() {
		return modifier != null;
	}
	
	public Shape getModifier() {
		return modifier;
	}
	
	/**
	 * Provides an iterator to go through a Shape subtree
	 * 
	 * @author Tim Dawborn
	 */
	protected class ShapeTreeIterator implements Iterator<Shape> {
		private final Stack<Shape> shapes;
		
		public ShapeTreeIterator(Shape root) {
			shapes = new Stack<Shape>();
			shapes.push(root);
		}
		
		public boolean hasNext() {
			return !shapes.empty();
		}
		
		public Shape next() {
			Shape node = shapes.pop();
			for (Shape child : node.getChildren()) {
				if (child != null)
					shapes.push(child);
			}
			return node;
		}
		
		/**
		 * Not implemented
		 */
		public void remove() {
			// blank
		}
	}
	
	public void subscribeSQLFragmentGeneratorListener(SQLFragmentListener listener) {
		// not implemented
	}
}
