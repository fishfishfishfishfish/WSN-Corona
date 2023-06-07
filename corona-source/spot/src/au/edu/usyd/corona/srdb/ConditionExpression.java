package au.edu.usyd.corona.srdb;


import au.edu.usyd.corona.grammar.TokenGrammarTokens;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class represents any numeric expression that can exist in a query, such
 * as a greater than comparison, a multiplication of two numbers, a value in a
 * row of data, etc.
 */
public abstract class ConditionExpression implements TokenGrammarTokens {
	protected final ConditionExpression[] children;
	
	protected ConditionExpression(ConditionExpression child1, ConditionExpression child2) {
		children = new ConditionExpression[]{child1, child2};
	}
	
	/**
	 * Evaluates the numeric expression and returns the numeric result
	 * 
	 * @param row the row to use if table attributes are required
	 * @return the evaluated numeric expression
	 * @throws InvalidOperationException
	 */
	public abstract ValueType eval(ValueType[] row) throws InvalidOperationException;
	
	/**
	 * @return the tokenized version of the current node and its subtree
	 */
	public abstract StringBuffer toTokens();
}
