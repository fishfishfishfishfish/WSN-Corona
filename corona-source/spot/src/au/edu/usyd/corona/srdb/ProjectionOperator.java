package au.edu.usyd.corona.srdb;


import java.util.Enumeration;
import java.util.Vector;

import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * Class that performs a projection on a table based on constrains expressed as
 * an AttributeExpression. This is not strictly a relational algebra projection,
 * because it does not check for any duplicate entries (and hence the result is
 * not always a set)
 * 
 * @author Raymes Khoury
 */
public class ProjectionOperator extends TableOperator {
	private final byte[] constraints;
	
	/**
	 * Constructor for the projection operator, where the result of a
	 * TableOperator's execution is the table to work from
	 * 
	 * @param table the expression that returns the table to perform the
	 * projection operation on
	 * @param constraints list of attributes wanted
	 */
	public ProjectionOperator(TableOperator table, byte[] constraints) {
		children = new TableOperator[]{table};
		this.constraints = constraints;
	}
	
	/**
	 * Constructor for the projection operator, where the result of a
	 * TableOperator's execution is the table to work from
	 * 
	 * @param table the expression that returns the table to perform the
	 * projection operation on
	 * @param expr vector of Bytes representing the attributes wanted
	 */
	public ProjectionOperator(TableOperator table, Vector expr) {
		children = new TableOperator[]{table};
		constraints = new byte[expr.size()];
		for (int i = 0; i < expr.size(); i++)
			constraints[i] = ((Byte) expr.elementAt(i)).byteValue();
	}
	
	/**
	 * Evaluates the selection operation
	 * 
	 * @return a table with only the projected attributes
	 */
	public Table eval(int epoch) throws InvalidOperationException {
		Table table = children[0].eval(epoch);
		Table result = new Table(table.getTaskID());
		
		ValueType[] row;
		for (Enumeration it = table.elements(); it.hasMoreElements();) {
			row = (ValueType[]) it.nextElement();
			ValueType[] projectedRow = new ValueType[constraints.length];
			for (int i = 0; i < constraints.length; i++)
				projectedRow[i] = row[constraints[i]];
			result.addRow(projectedRow);
		}
		return result;
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_PROJECT).append(T_GROUP_OPEN).append(children[0].toTokens());
		for (int i = 0; i < constraints.length; i++)
			b.append(' ').append(constraints[i]);
		return b.append(T_GROUP_CLOSE);
	}
}
