package au.edu.usyd.corona.srdb;


import java.util.Enumeration;

import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * Class Operation that merges two tables together.
 * 
 * @author Raymes Khoury
 * @author Tim Dawborn
 */
public class MergeOperator extends TableOperator {
	private final TaskID taskID;
	
	/**
	 * Constructor taking two child operations which will be evaluated to tables
	 * and merged. Tables with different attributes are supported.
	 * 
	 * @param taskID
	 * @param child1 The first table to merge
	 * @param child2 The second table to merge
	 */
	public MergeOperator(TaskID taskID, TableOperator child1, TableOperator child2) {
		children = new TableOperator[]{child1, child2};
		this.taskID = taskID;
	}
	
	/**
	 * Creates a new table with the combined results of the two child operations
	 * given. Approximately O(n) time.
	 */
	public Table eval(int epoch) throws InvalidOperationException {
		//evaluates the children and stores their tables
		Table[] tables = new Table[]{children[0].eval(epoch), children[1].eval(epoch)};
		
		//checks for null tables
		if ((tables[0] == null) && (tables[1] == null))
			return new Table(taskID);
		else if (tables[0] == null)
			return tables[1];
		else if (tables[1] == null)
			return tables[0];
		
		//gets the table with the max number of columns
		final int maxIndex = (tables[0].getNumCols() < tables[1].getNumCols()) ? 1 : 0;
		final Table result = tables[maxIndex];
		final int colCount = result.getNumCols();
		ValueType[] row = null;
		
		//merges the other tables into it, padding if need be
		for (int i = 0; i < tables.length; i++) {
			if (i == maxIndex)
				continue;
			for (Enumeration it = tables[i].elements(); it.hasMoreElements();) {
				row = (ValueType[]) it.nextElement();
				ValueType[] mergedRow = new ValueType[colCount];
				System.arraycopy(row, 0, mergedRow, 0, row.length);
				result.addRow(mergedRow);
			}
		}
		return result;
	}
	
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		return b.append(T_MERGE).append(T_GROUP_OPEN).append(children[0].toTokens()).append(' ').append(children[1].toTokens()).append(T_GROUP_CLOSE);
	}
}
