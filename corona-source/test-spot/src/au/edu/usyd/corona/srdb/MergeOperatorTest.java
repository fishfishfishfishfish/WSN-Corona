package au.edu.usyd.corona.srdb;


import java.util.Enumeration;

import au.edu.usyd.corona.grammar.TokenGrammarTokens;
import au.edu.usyd.corona.grammar.TokenParseException;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.InvalidOperationException;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

public class MergeOperatorTest extends TableOperatorTestCase implements TokenGrammarTokens {
	
	@Override
	public void setUp() {
		Network.initialize(Network.MODE_UNITTEST);
	}
	
	@SuppressWarnings("unchecked")
	private static void assertTableEquals(final Table expected, final Table t1, final Table t2) throws InvalidOperationException, TokenParseException {
		final Table actual = new MergeOperator(new TaskID(2), new ReadOperator(t1), new ReadOperator(t2)).eval(0);
		
		// assert table dimensions
		assertEquals(expected.getNumRows(), actual.getNumRows());
		assertEquals(expected.getNumCols(), actual.getNumCols());
		
		// assert table contents
		for (Enumeration<ValueType[]> ee = expected.elements(), ea = actual.elements(); ee.hasMoreElements();)
			assertTrue(rowsEqual(ee.nextElement(), ea.nextElement()));
	}
	
	public void testNormal() throws InvalidOperationException, TokenParseException {
		final Table t1 = new Table(new TaskID(0));
		t1.addRow(new ValueType[]{new IntType(1), new IntType(2)});
		t1.addRow(new ValueType[]{new IntType(3), new IntType(4)});
		t1.addRow(new ValueType[]{new IntType(5), new IntType(6)});
		
		final Table t2 = new Table(new TaskID(1));
		t2.addRow(new ValueType[]{new IntType(7), new IntType(8)});
		t2.addRow(new ValueType[]{new IntType(9), new IntType(10)});
		
		final Table te = new Table(new TaskID(0));
		te.addRow(new ValueType[]{new IntType(1), new IntType(2)});
		te.addRow(new ValueType[]{new IntType(3), new IntType(4)});
		te.addRow(new ValueType[]{new IntType(5), new IntType(6)});
		te.addRow(new ValueType[]{new IntType(7), new IntType(8)});
		te.addRow(new ValueType[]{new IntType(9), new IntType(10)});
		
		assertTableEquals(te, t1, t2);
	}
	
	public void testOneEmpty() throws InvalidOperationException, TokenParseException {
		final Table t1 = new Table(new TaskID(0));
		t1.addRow(new ValueType[]{new IntType(1), new IntType(2)});
		t1.addRow(new ValueType[]{new IntType(3), new IntType(4)});
		t1.addRow(new ValueType[]{new IntType(5), new IntType(6)});
		
		final Table t2 = new Table(new TaskID(1));
		
		final Table te = new Table(new TaskID(0));
		te.addRow(new ValueType[]{new IntType(1), new IntType(2)});
		te.addRow(new ValueType[]{new IntType(3), new IntType(4)});
		te.addRow(new ValueType[]{new IntType(5), new IntType(6)});
		
		assertTableEquals(te, t1, t2);
	}
	
	public void testBothEmpty() throws InvalidOperationException, TokenParseException {
		final Table t1 = new Table(new TaskID(0));
		final Table t2 = new Table(new TaskID(1));
		final Table te = new Table(new TaskID(0));
		assertTableEquals(te, t1, t2);
	}
	
	public void testDifferentColumnCounts() throws InvalidOperationException, TokenParseException {
		final Table t1 = new Table(new TaskID(0));
		t1.addRow(new ValueType[]{new IntType(1), new BooleanType(true), new LongType(123)});
		t1.addRow(new ValueType[]{new IntType(4), new BooleanType(false), new LongType(456)});
		t1.addRow(new ValueType[]{new IntType(7), new BooleanType(true), new LongType(789)});
		
		final Table t2 = new Table(new TaskID(0));
		t2.addRow(new ValueType[]{new IntType(11), new BooleanType(true)});
		t2.addRow(new ValueType[]{new IntType(21), new BooleanType(false)});
		
		final Table te = new Table(new TaskID(0));
		te.addRow(new ValueType[]{new IntType(1), new BooleanType(true), new LongType(123)});
		te.addRow(new ValueType[]{new IntType(4), new BooleanType(false), new LongType(456)});
		te.addRow(new ValueType[]{new IntType(7), new BooleanType(true), new LongType(789)});
		te.addRow(new ValueType[]{new IntType(11), new BooleanType(true), null});
		te.addRow(new ValueType[]{new IntType(21), new BooleanType(false), null});
		
		assertTableEquals(te, t1, t2);
	}
	
}
