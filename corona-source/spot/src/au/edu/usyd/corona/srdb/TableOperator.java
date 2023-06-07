package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.grammar.TokenGrammarTokens;
import au.edu.usyd.corona.types.InvalidOperationException;

/**
 * This is the abstract base class for items in this expression tree. Queries in
 * the token grammar format are parsed out into an {@link TableOperator}
 * expression tree, and when it is their time to be executed in the scheduler,
 * the {@link #eval(int)} method is called, recursively evaluating the
 * expression tree.
 * 
 * @author Tim Dawborn
 */
public abstract class TableOperator implements TokenGrammarTokens {
	protected TableOperator[] children; // the children of the current node in the operator tree
	
	/**
	 * Evaluates the operation that is to be performed on the table(s), which are
	 * defined in the subclasses, and the new Table is returned
	 * 
	 * @param epoch
	 * @return the new table based on the performed operations
	 * @throws InvalidOperationException
	 */
	public abstract Table eval(int epoch) throws InvalidOperationException;
	
	/**
	 * @return the tokenized version of the current node and its subtree
	 */
	public abstract StringBuffer toTokens();
}
