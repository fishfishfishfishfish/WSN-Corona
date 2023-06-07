package au.edu.usyd.corona.srdb;


import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import junit.framework.TestCase;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

abstract class TableOperatorTestCase extends TestCase {
	protected Table testTable;
	protected List<ValueType[]> rows;
	
	private void addRow(Table table, long a, int b, int c, boolean d) {
		final ValueType[] row = new ValueType[]{new LongType(a), new IntType(b), new IntType(c), new BooleanType(d)};
		table.addRow(row);
		rows.add(row);
	}
	
	@Override
	public void setUp() {
		rows = new ArrayList<ValueType[]>();
		testTable = new Table(new TaskID(1));
		addRow(testTable, 123, 42, 20, false);
		addRow(testTable, 124, 11, 21, false);
		addRow(testTable, 122, 90, 24, true);
		addRow(testTable, 120, 127, 20, false);
		addRow(testTable, 120, 120, 19, false);
		addRow(testTable, 122, 90, 20, true);
		addRow(testTable, 123, 45, 20, false);
		addRow(testTable, 124, 10, 21, false);
		addRow(testTable, 124, 15, 21, false);
		addRow(testTable, 124, 10, 22, false);
		
		assertEquals(1, testTable.getTaskID().getQueryID());
		assertEquals(4, testTable.getNumCols());
		assertEquals(10, testTable.getNumRows());
	}
	
	@Override
	public void tearDown() {
		testTable = null;
		rows = null;
	}
	
	protected boolean rowsEqual(int rowNum, ValueType[] actual) throws InvalidOperationException {
		return rowsEqual(rows.get(rowNum), actual);
	}
	
	protected static boolean rowsEqual(ValueType[] expected, ValueType[] actual) throws InvalidOperationException {
		for (int i = 0; i < actual.length; i++)
			if (!(expected[i] == null && actual[i] == null) && !expected[i].equals(actual[i]))
				return false;
		return true;
	}
	
	@SuppressWarnings("unchecked")
	protected void assertTableEquals(int numCols, Table table, int... rowNumbers) throws InvalidOperationException {
		// ensure the same number of rows, num columns, and same task id 
		assertEquals(1, table.getTaskID().getQueryID());
		assertEquals(numCols, table.getNumCols());
		assertEquals(rowNumbers.length, table.getNumRows());
		
		// mark which of the rows we have seen
		boolean[] seen = new boolean[rowNumbers.length];
		for (Enumeration<ValueType[]> e = table.elements(); e.hasMoreElements();) {
			final ValueType[] row = e.nextElement();
			for (int i = 0; i < rowNumbers.length; i++) {
				if (rowsEqual(rowNumbers[i], row)) {
					seen[i] = true;
					break;
				}
			}
		}
		
		// checks that every row was found
		for (int i = 0; i < rowNumbers.length; i++)
			if (!seen[i])
				fail("Failed to see row number " + rowNumbers[i]);
	}
}
