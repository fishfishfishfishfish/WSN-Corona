package au.edu.usyd.corona.srdb;


import java.util.Enumeration;

import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * Class that performs a selection on a table based on constrains expressed as a
 * {@link ConditionExpression}
 */
public class SelectionOperator extends TableOperator {
	private final ConditionExpression constraints;
	
	/**
	 * Constructor for the selection operator, where a the result of a
	 * TableOperator's execution is the table to work from
	 * 
	 * @param table the expression that returns the table to perform the
	 * selection operation on
	 * @param expr the numeric expression used to select out the wanted rows
	 */
	public SelectionOperator(TableOperator table, ConditionExpression expr) {
		children = new TableOperator[]{table};
		constraints = expr;
	}
	
	/**
	 * Evaluates the selection operation
	 */
	public Table eval(int epoch) throws InvalidOperationException {
		Table table = children[0].eval(epoch);
		
		//creates the return table
		Table t = new Table(table.getTaskID());
		ValueType[] row;
		
		//goes through the table row by row and evaluates it against the expression
		for (Enumeration it = table.elements(); it.hasMoreElements();) {
			row = (ValueType[]) it.nextElement();
			if (((BooleanType) constraints.eval(row)).getVal())
				t.addRow(row);
		}
		
		return t;
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		return b.append(T_SELECT).append(T_GROUP_OPEN).append(children[0].toTokens()).append(' ').append(constraints.toTokens()).append(T_GROUP_CLOSE);
	}
}
