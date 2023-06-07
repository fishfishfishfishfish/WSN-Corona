package au.edu.usyd.corona.gui.dnd.shapes;


import java.awt.Color;
import java.awt.Graphics2D;

import au.edu.usyd.corona.gui.dnd.SQLFragmentGenerator;
import au.edu.usyd.corona.gui.util.DrawingConstants;

/**
 * All drag and drop shape classes must implement this interface
 * 
 * @author Tim Dawborn
 * @author Glen Pink
 */
public interface Shape extends java.awt.Shape, DrawingConstants, Iterable<Shape>, TreeNode<Shape>, SQLFragmentGenerator {
	
	public void draw(Graphics2D g);
	
	public void recurseDrawGrouper(Graphics2D g);
	
	public void recurseDrawLinks(Graphics2D g);
	
	public void recurseDrawShape(Graphics2D g);
	
	public void recurseDrawValue(Graphics2D g);
	
	public Color getColour();
	
	public Color getLineColour();
	
	public void setLineColour(Color colour);
	
	public String getValue();
	
	public Shape getDrag();
	
	public void setPoint(int x, int y);
	
	public void updatePoint(int dx, int dy);
	
	public void setX(int x);
	
	public void setY(int y);
	
	public int getX();
	
	public int getY();
	
	public int getWidth();
	
	public int getHeight();
	
	public void setGrouperVisible(boolean x);
	
	public GroupingShape getGrouper();
	
	public float[] getCentrePoint();
	
	public boolean canHaveDroppedOn(Shape shape);
	
	/**
	 * Performs the drop action of the provided shape onto the current shape,
	 * return weather or not the operation was successful
	 * 
	 * @param shape the shape to drag-and-drop onto the current shape
	 * @return weather or not the operation was successful or not
	 */
	public boolean doDropOn(Shape shape);
	
	/**
	 * Returns a duplicate copy of the shape (a wrapper around Object.clone)
	 * 
	 * @return duplcicate copy of the shape
	 */
	public Shape duplicate();
	
	/**
	 * Updates the internals of the shape appropriate for the end of movement.
	 * This is to update any shapes moving with the shape.
	 */
	public void endMovement();
	
	/**
	 * @return the unary modifier attached to the shape, or null if one does not
	 * exist
	 */
	public Shape getModifier();
	
	/**
	 * @return weather or not the shape in question has a unary modifier attached
	 * to it
	 */
	public boolean hasModifier();
}
