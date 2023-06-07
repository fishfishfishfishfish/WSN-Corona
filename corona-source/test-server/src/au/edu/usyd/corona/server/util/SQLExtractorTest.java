package au.edu.usyd.corona.server.util;


import java.sql.SQLException;
import java.util.ArrayList;

import junit.framework.TestCase;

/**
 * @author Edmund
 * 
 */
public class SQLExtractorTest extends TestCase {
	public void testSQLExtractorWhere() throws SQLException {
		SQLExtractor extractor;
		
		// Empty input - null
		extractor = new SQLExtractor(null, SQLExtractor.Type.WHERE);
		assertEquals("", extractor.extractWhere());
		assertEquals("", extractor.extractOrderBy());
		assertEquals("", extractor.extractSelect());
		assertEquals("", extractor.extractFrom());
		assertEquals("", extractor.extractGroupBy());
		assertEquals("", extractor.extractHaving());
		
		// Empty input - ""
		extractor = new SQLExtractor("", SQLExtractor.Type.WHERE);
		assertEquals("", extractor.extractWhere());
		assertEquals("", extractor.extractOrderBy());
		assertEquals("", extractor.extractSelect());
		assertEquals("", extractor.extractFrom());
		assertEquals("", extractor.extractGroupBy());
		assertEquals("", extractor.extractHaving());
		
		// Valid inputs:
		ArrayList<String> normalCases = new ArrayList<String>();
		normalCases.add("");
		normalCases.add("  ");
		normalCases.add("\t");
		normalCases.add(null);
		normalCases.add("a");
		normalCases.add("+1");
		normalCases.add("(a)");
		normalCases.add("(((((((((((1)))))))))))");
		normalCases.add("''");
		normalCases.add("1 = 1");
		normalCases.add("a = 1");
		normalCases.add("1 = a");
		normalCases.add("a = b");
		normalCases.add("a > b");
		normalCases.add("a < b");
		normalCases.add("a AND b");
		normalCases.add("a LIKE '%'");
		normalCases.add("a LIKE b");
		normalCases.add("a NOT LIKE b");
		normalCases.add("a IN (a,1,b,2,'c')");
		normalCases.add("a BETWEEN a AND b");
		normalCases.add("a BETWEEN 'a' AND b");
		normalCases.add("a BETWEEN 1 AND 10");
		normalCases.add("SUM(a)");
		normalCases.add("SUM(a) = 1");
		normalCases.add("true OR false");
		normalCases.add("MAX(a)");
		normalCases.add("MIN(a)");
		normalCases.add("COUNT(a)");
		normalCases.add("AVG(a)");
		normalCases.add("NULL");
		normalCases.add("EXISTS (SELECT * FROM queries)");
		
		ArrayList<String> normalExpected = new ArrayList<String>();
		normalExpected.add("");
		normalExpected.add("");
		normalExpected.add("");
		normalExpected.add("");
		normalExpected.add("a");
		normalExpected.add("(+ 1)");
		normalExpected.add("a");
		normalExpected.add("1");
		normalExpected.add("''");
		normalExpected.add("(1 = 1)");
		normalExpected.add("(a = 1)");
		normalExpected.add("(1 = a)");
		normalExpected.add("(a = b)");
		normalExpected.add("(a > b)");
		normalExpected.add("(a < b)");
		normalExpected.add("(a AND b)");
		normalExpected.add("(a LIKE '%')");
		normalExpected.add("(a LIKE b)");
		normalExpected.add("(a NOT LIKE b)");
		normalExpected.add("(a IN (a, 1, b, 2, 'c'))");
		normalExpected.add("(a BETWEEN a AND b)");
		normalExpected.add("(a BETWEEN 'a' AND b)");
		normalExpected.add("(a BETWEEN 1 AND 10)");
		normalExpected.add("SUM(a)");
		normalExpected.add("(SUM(a) = 1)");
		normalExpected.add("(true OR false)");
		normalExpected.add("MAX(a)");
		normalExpected.add("MIN(a)");
		normalExpected.add("COUNT(a)");
		normalExpected.add("AVG(a)");
		normalExpected.add("NULL");
		normalExpected.add("(EXISTS (select * from queries))");
		
		for (int i = 0; i < normalCases.size(); i++) {
			extractor = new SQLExtractor(normalCases.get(i), SQLExtractor.Type.WHERE);
			//			assertTrue(normalExpected.get(i).equalsIgnoreCase(extractor.extractWhere()));
			assertEquals(normalExpected.get(i), extractor.extractWhere());
			assertEquals("", extractor.extractOrderBy());
			assertEquals("", extractor.extractSelect());
			assertEquals("", extractor.extractFrom());
			assertEquals("", extractor.extractGroupBy());
			assertEquals("", extractor.extractHaving());
		}
		
		// Erroneous inputs:
		ArrayList<String> errorCases = new ArrayList<String>();
		errorCases.add("a LIKE %");
		errorCases.add("LIKE");
		errorCases.add("*");
		errorCases.add("true false");
		errorCases.add("a b c");
		errorCases.add("AVG a");
		errorCases.add("SUM(SUM)");
		errorCases.add("AVG(+)");
		
		for (int i = 0; i < errorCases.size(); i++) {
			extractor = new SQLExtractor(errorCases.get(i), SQLExtractor.Type.WHERE);
			try {
				extractor.extractWhere();
				fail("The input of \"" + errorCases.get(i) + "\" did not throw an exception.");
			}
			catch (SQLException e) {
				// Expect each case to reach here: an SQLException is thrown
			}
		}
	}
	
	public void testSQLExtractorOrderBy() throws SQLException {
		SQLExtractor extractor;
		
		// Empty input - null
		extractor = new SQLExtractor(null, SQLExtractor.Type.ORDER_BY);
		assertEquals("", extractor.extractOrderBy());
		assertEquals("", extractor.extractWhere());
		assertEquals("", extractor.extractSelect());
		assertEquals("", extractor.extractFrom());
		assertEquals("", extractor.extractGroupBy());
		assertEquals("", extractor.extractHaving());
		
		// Empty input - ""
		extractor = new SQLExtractor("", SQLExtractor.Type.ORDER_BY);
		assertEquals("", extractor.extractOrderBy());
		assertEquals("", extractor.extractWhere());
		assertEquals("", extractor.extractSelect());
		assertEquals("", extractor.extractFrom());
		assertEquals("", extractor.extractGroupBy());
		assertEquals("", extractor.extractHaving());
		
		// Valid inputs:
		ArrayList<String> normalCases = new ArrayList<String>();
		normalCases.add("a");
		normalCases.add("a ASC");
		normalCases.add("a DESC");
		normalCases.add("1");
		
		ArrayList<String> normalExpected = new ArrayList<String>();
		normalExpected.add("a ASC");
		normalExpected.add("a ASC");
		normalExpected.add("a DESC");
		normalExpected.add("1 ASC");
		
		for (int i = 0; i < normalCases.size(); i++) {
			extractor = new SQLExtractor(normalCases.get(i), SQLExtractor.Type.ORDER_BY);
			assertEquals(normalExpected.get(i), extractor.extractOrderBy());
			assertEquals("", extractor.extractWhere());
			assertEquals("", extractor.extractSelect());
			assertEquals("", extractor.extractFrom());
			assertEquals("", extractor.extractGroupBy());
			assertEquals("", extractor.extractHaving());
		}
		
		// Erroneous inputs:
		ArrayList<String> errorCases = new ArrayList<String>();
		errorCases.add("a b c");
		errorCases.add("ASC");
		errorCases.add("DESC");
		errorCases.add("a ASC DESC");
		errorCases.add("a ASC b DESC");
		errorCases.add("%");
		
		for (int i = 0; i < errorCases.size(); i++) {
			extractor = new SQLExtractor(errorCases.get(i), SQLExtractor.Type.ORDER_BY);
			try {
				extractor.extractOrderBy();
				fail("The input of \"" + errorCases.get(i) + "\" did not throw an exception.");
			}
			catch (SQLException e) {
				//Expect each case to reach here: an SQLException is thrown
			}
		}
	}
	
	public void testSQLExtractorFullQuery() throws SQLException {
		SQLExtractor extractor;
		
		// Empty input - null
		extractor = new SQLExtractor(null, SQLExtractor.Type.FULL_QUERY);
		assertEquals("", extractor.extractWhere());
		assertEquals("", extractor.extractOrderBy());
		assertEquals("", extractor.extractSelect());
		assertEquals("", extractor.extractFrom());
		assertEquals("", extractor.extractGroupBy());
		assertEquals("", extractor.extractHaving());
		
		// Empty input - ""
		extractor = new SQLExtractor("", SQLExtractor.Type.FULL_QUERY);
		assertEquals("", extractor.extractWhere());
		assertEquals("", extractor.extractOrderBy());
		assertEquals("", extractor.extractSelect());
		assertEquals("", extractor.extractFrom());
		assertEquals("", extractor.extractGroupBy());
		assertEquals("", extractor.extractHaving());
		
		// Valid inputs:
		ArrayList<String> normalCases = new ArrayList<String>();
		normalCases.add("");
		normalCases.add("  ");
		normalCases.add("\t");
		normalCases.add(null);
		normalCases.add("SELECT * FROM p");
		normalCases.add("SELECT a, b, c FROM p");
		normalCases.add("SELECT a, b, c FROM p;");
		normalCases.add("SELECT a, b, c FROM p; DROP TABLE x");
		normalCases.add("SELECT a FROM p WHERE (MAX(x) > y + z) = false");
		normalCases.add("SELECT a FROM p, q WHERE x GROUP BY y HAVING z ORDER BY a");
		
		ArrayList<String[]> normalExpected = new ArrayList<String[]>();
		normalExpected.add(new String[]{"", "", "", "", "", ""});
		normalExpected.add(new String[]{"", "", "", "", "", ""});
		normalExpected.add(new String[]{"", "", "", "", "", ""});
		normalExpected.add(new String[]{"", "", "", "", "", ""});
		normalExpected.add(new String[]{"*", "p", "", "", "", ""});
		normalExpected.add(new String[]{"a, b, c", "p", "", "", "", ""});
		normalExpected.add(new String[]{"a, b, c", "p", "", "", "", ""});
		normalExpected.add(new String[]{"a, b, c", "p", "", "", "", ""});
		normalExpected.add(new String[]{"a", "p", "((MAX(x) > (y + z)) = false)", "", "", ""});
		normalExpected.add(new String[]{"a", "p, q", "x", "y", "z", "a ASC"});
		
		for (int i = 0; i < normalCases.size(); i++) {
			extractor = new SQLExtractor(normalCases.get(i), SQLExtractor.Type.FULL_QUERY);
			assertEquals(normalExpected.get(i)[0], extractor.extractSelect());
			assertEquals(normalExpected.get(i)[1], extractor.extractFrom());
			assertEquals(normalExpected.get(i)[2], extractor.extractWhere());
			assertEquals(normalExpected.get(i)[3], extractor.extractGroupBy());
			assertEquals(normalExpected.get(i)[4], extractor.extractHaving());
			assertEquals(normalExpected.get(i)[5], extractor.extractOrderBy());
		}
		
		// Erroneous inputs:
		ArrayList<String> errorCases = new ArrayList<String>();
		errorCases.add(";");
		errorCases.add("a");
		errorCases.add("SELECT");
		errorCases.add("SELECT *");
		errorCases.add("FROM p");
		errorCases.add("1 = 1");
		errorCases.add("MAX(a)");
		errorCases.add("a LIKE %");
		errorCases.add("%");
		
		for (int i = 0; i < errorCases.size(); i++) {
			extractor = new SQLExtractor(errorCases.get(i), SQLExtractor.Type.FULL_QUERY);
			try {
				extractor.extractSelect();
				fail("The input of \"" + errorCases.get(i) + "\" did not throw an exception.");
			}
			catch (SQLException e) {
				// Expect each case to reach here: an SQLException is thrown
			}
		}
	}
}
