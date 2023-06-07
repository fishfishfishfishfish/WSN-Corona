package au.edu.usyd.corona.gui.dnd.shapes;


public interface TreeNode<T> {
	/**
	 * @return weather or not the shape in question is a leaf in the tree or not
	 */
	public boolean isLeaf();
	
	/**
	 * @return weather or not the shape in question is the root node of a tree or
	 * not
	 */
	public boolean isRoot();
	
	/**
	 * @return the child nodes of the current shape
	 */
	public T[] getChildren();
	
	/**
	 * @param children the new children
	 */
	public void setChildren(T[] children);
	
	/**
	 * @return the parent of the current tree, or null if it's a root node
	 */
	public T getParent();
	
	/**
	 * @param parent the new parent of the current node
	 */
	public void setParent(T parent);
	
	/**
	 * @return the depth of the current node in the tree
	 */
	public int calculateDepth();
	
	/**
	 * @return the root of the tree
	 */
	public T getRoot();
	
	/**
	 * Removes the child from the node
	 * 
	 * @param child the child to remove
	 */
	public void removeChild(T child);
}
