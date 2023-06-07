package au.edu.usyd.corona.srdb;


import java.util.Enumeration;

import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

/**
 * This class calculates and returns the cartesean (cross) product of two
 * tables. i.e. it produces every combination of the two sets of data.
 * 
 * @author Tim Dawborn
 */
public class CrossProductOperator extends TableOperator {
	private final TaskID taskID;
	
	public CrossProductOperator(TaskID taskID, TableOperator child1, TableOperator child2) {
		children = new TableOperator[]{child1, child2};
		this.taskID = taskID;
	}
	
	public Table eval(int epoch) throws InvalidOperationException {
		//evaluates the children and stores their tables
		final Table[] tables = new Table[2];
		for (int i = 0; i < 2; i++)
			tables[i] = children[i].eval(epoch);
		
		//checks for null tables
		if ((tables[0] == null) && (tables[1] == null))
			return new Table(taskID);
		else if (tables[0] == null)
			return tables[1];
		else if (tables[1] == null)
			return tables[0];
		
		//performs the cross product of the two sets
		final Table result = new Table(tables[0].getTaskID());
		ValueType[] row1, row2, rowC;
		
		for (Enumeration it1 = tables[0].elements(); it1.hasMoreElements();) {
			row1 = (ValueType[]) it1.nextElement();
			for (Enumeration it2 = tables[1].elements(); it2.hasMoreElements();) {
				row2 = (ValueType[]) it2.nextElement();
				rowC = new ValueType[row1.length + row2.length];
				System.arraycopy(row1, 0, rowC, 0, row1.length);
				System.arraycopy(row2, 0, rowC, row1.length, row2.length);
				result.addRow(rowC);
			}
		}
		
		return result;
	}
	
	public StringBuffer toTokens() {
		return null;
	}
}
