package au.edu.usyd.corona.middleLayer;


import java.util.Enumeration;
import java.util.Hashtable;

/**
 * This class stores information about the routing tree, including this nodes
 * height, its parent and children in the tree. This information is used by
 * RoutingManager to maintain the tree. We try to establish a minimum height
 * routing tree, so the only information stored about a parent is its depth in
 * the tree (which is simply 1 less than our depth). If we were to construct a
 * tree based on other factors (such as the degree of the parent node) we would
 * also need to store this information.
 * 
 * In order to know the height of the subtree rooted at our node (which is a
 * useful value), we must know the heights of all our children and take maximum
 * of these (and add 1) to find our height. As such, each nodes height is
 * updated periodically (in a heartbeat message) by RoutingManager, and also
 * upon re-routing the tree. Whenever we alter a childs height in this class, we
 * also re-calculate our height.
 * 
 * @author Khaled Almi'ani
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
class Routing {
	private final Hashtable children; // Long => Byte (IEEE address => child height)
	
	private byte depth; // The depth of this node in the routing tree
	private byte height; // The height of the subtree rooted at this node
	private long parent; // The parent of this nodes
	
	Routing() {
		children = new Hashtable();
		depth = 0;
		height = 0;
		parent = -1;
	}
	
	/**
	 * Reset the state concerning a parent node
	 */
	void lostParent() {
		parent = -1;
		depth = Byte.MAX_VALUE;
	}
	
	/**
	 * Return the parent of this node
	 * 
	 * @return the address of the parent of this node
	 */
	long getRoutingParent() {
		return parent;
	}
	
	/**
	 * Return the number of routing children this node has
	 * 
	 * @return the number of routing children this node has
	 */
	int getNumberChildren() {
		return children.size();
	}
	
	/**
	 * Return the depth of this node in the routing tree
	 * 
	 * @return The depth of this node in the routing tree
	 */
	byte getDepth() {
		return depth;
	}
	
	/**
	 * Return the height of the sub-tree rooted at this node
	 * 
	 * @return the height of the sub-tree rooted at this node
	 */
	byte getHeight() {
		return height;
	}
	
	/**
	 * Return whether the given node is a child of this node
	 * 
	 * @param nodeId the potential child node
	 * @return true if the node is a child, false otherwise
	 */
	boolean hasChild(Long nodeId) {
		return children.containsKey(nodeId);
	}
	
	/**
	 * Return true if the node has a routing parent, false otherwise
	 * 
	 * @return true if the node has a routing parent, false otherwise
	 */
	boolean hasParent() {
		return parent != -1;
	}
	
	/**
	 * Set a new parent
	 * 
	 * @param node
	 * @param depth
	 */
	void newParent(long node, byte depth) {
		this.depth = (byte) (depth + 1);
		parent = node;
	}
	
	/**
	 * Return an array of the IDs of routing children of this node
	 * 
	 * @return an array of the IDs of routing children of this node
	 */
	Long[] getChildren() {
		final Long[] x = new Long[children.size()];
		int i = 0;
		for (Enumeration e = children.keys(); e.hasMoreElements(); i++)
			x[i] = (Long) e.nextElement();
		return x;
	}
	
	private byte maxValueInHashtable(Hashtable map) {
		Byte value = new Byte((byte) -1);
		for (Enumeration e = map.elements(); e.hasMoreElements();) {
			Byte v = (Byte) e.nextElement();
			if ((value == null) || (v.byteValue() > value.byteValue()))
				value = v;
		}
		return value.byteValue();
	}
	
	/**
	 * Remove all child nodes
	 */
	void clearChildren() {
		children.clear();
		updateMyHeight();
	}
	
	/**
	 * Remove a given child node
	 * 
	 * @param node The child to remove
	 */
	void removeChild(long node) {
		children.remove(new Long(node));
		updateMyHeight();
	}
	
	/**
	 * Add a child node without knowing its height value
	 * 
	 * @param node the routing child to add
	 */
	void addChild(long node) {
		addChild(node, (byte) 0);
	}
	
	/**
	 * Add (or update) a routing child whose height is known
	 * 
	 * @param node the node to add
	 * @param height the height of the subtree rooted at that child
	 */
	void addChild(long node, byte height) {
		children.put(new Long(node), new Byte(height));
		updateMyHeight();
	}
	
	private void updateMyHeight() {
		// update my height based on my childrens height
		if (children.isEmpty())
			height = 0;
		else
			height = (byte) (maxValueInHashtable(children) + 1);
	}
	
	/**
	 * Return the number of children this node has
	 * 
	 * @return the number of children this node has
	 */
	int numChildren() {
		return children.size();
	}
	
	/**
	 * Return whether this node has any routing children
	 * 
	 * @return true if the node has routing children, false otherwise
	 */
	boolean hasChildren() {
		return !children.isEmpty();
	}
	
}
