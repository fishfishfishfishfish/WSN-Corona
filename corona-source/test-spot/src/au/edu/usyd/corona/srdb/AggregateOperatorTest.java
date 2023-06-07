package au.edu.usyd.corona.srdb;


import java.util.Enumeration;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.ValueType;

public class AggregateOperatorTest extends TableOperatorTestCase {
	
	private Table table;
	private final TableOperator tableOp = new TableOperator() {
		@Override
		public Table eval(int epoch) throws InvalidOperationException {
			return table;
		}
		
		@Override
		public StringBuffer toTokens() {
			return null;
		}
	};
	
	@Override
	public void setUp() {
		Network.initialize(Network.MODE_UNITTEST);
		
		table = new Table(new TaskID(1));
		for (int i = 0; i != 10; i++)
			table.addRow(new ValueType[]{new IntType(1), new IntType(5 * i), new IntType(20)});
		for (int i = 0; i != 7; i++)
			table.addRow(new ValueType[]{new IntType(1), new IntType(3 * i), new IntType(7)});
	}
	
	@SuppressWarnings("unchecked")
	public void testMin() throws Exception {
		Table t;
		Enumeration<ValueType[]> e;
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MIN}, new byte[]{1}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(0), e.nextElement()[1]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MIN}, new byte[]{2}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(7), e.nextElement()[2]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MIN}, new byte[]{1}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		assertEquals(new IntType(0), e.nextElement()[1]);
		assertEquals(new IntType(0), e.nextElement()[1]);
		
		// 0 3  6 9 12 15 18 21 24 27
		// X 5 10 X 20 25 30 35 40 45
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MIN}, new byte[]{2}, new byte[]{1}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(15, t.getNumRows());
		while (e.hasMoreElements()) {
			ValueType[] row = e.nextElement();
			if (row[0].equals(new IntType(0)))
				assertEquals(new IntType(7), row[2]);
			else if (row[0].equals(new IntType(15)))
				assertEquals(new IntType(7), row[2]);
		}
	}
	
	@SuppressWarnings("unchecked")
	public void testMax() throws Exception {
		Table t;
		Enumeration<ValueType[]> e;
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MAX}, new byte[]{1}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(45), e.nextElement()[1]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MAX}, new byte[]{2}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(20), e.nextElement()[2]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.MAX}, new byte[]{1}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		assertEquals(new IntType(45), e.nextElement()[1]);
		assertEquals(new IntType(18), e.nextElement()[1]);
	}
	
	@SuppressWarnings("unchecked")
	public void testAverage() throws Exception {
		Table t;
		Enumeration<ValueType[]> e;
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.AVG}, new byte[]{2}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		assertEquals(new IntType(20), e.nextElement()[2]);
		assertEquals(new IntType(7), e.nextElement()[2]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.AVG}, new byte[]{1}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		assertEquals(new IntType(22), e.nextElement()[1]);
		assertEquals(new IntType(9), e.nextElement()[1]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.AVG}, new byte[]{1}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(16), e.nextElement()[1]);
	}
	
	@SuppressWarnings("unchecked")
	public void testCount() throws Exception {
		Table t;
		Enumeration<ValueType[]> e;
		ValueType[] row;
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.COUNT}, new byte[]{1}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(17), e.nextElement()[0]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.COUNT}, new byte[]{2}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(17), e.nextElement()[0]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.COUNT}, new byte[]{1}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		assertEquals(new IntType(10), e.nextElement()[0]);
		assertEquals(new IntType(7), e.nextElement()[0]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.COUNT, AggregateOperator.COUNT}, new byte[]{1, 2}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		row = e.nextElement();
		assertEquals(new IntType(10), row[0]);
		row = e.nextElement();
		assertEquals(new IntType(7), row[0]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.COUNT, AggregateOperator.AVG}, new byte[]{1, 2}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		row = e.nextElement();
		assertEquals(new IntType(10), row[0]);
		assertEquals(new IntType(20), row[2]);
		row = e.nextElement();
		assertEquals(new IntType(7), row[0]);
		assertEquals(new IntType(7), row[2]);
	}
	
	@SuppressWarnings("unchecked")
	public void testSum() throws Exception {
		Table t;
		Enumeration<ValueType[]> e;
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.SUM}, new byte[]{1}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(288), e.nextElement()[1]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.SUM}, new byte[]{2}, new byte[]{}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(1, t.getNumRows());
		assertEquals(new IntType(249), e.nextElement()[2]);
		
		t = new AggregateOperator(tableOp, new byte[]{AggregateOperator.SUM}, new byte[]{1}, new byte[]{2}).eval(0);
		e = t.elements();
		assertEquals(3, t.getNumCols());
		assertEquals(2, t.getNumRows());
		assertEquals(new IntType(225), e.nextElement()[1]);
		assertEquals(new IntType(63), e.nextElement()[1]);
	}
}
