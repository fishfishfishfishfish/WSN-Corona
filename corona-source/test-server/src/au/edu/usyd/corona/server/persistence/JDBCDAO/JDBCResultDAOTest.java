/**
 * 
 */
package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.rmi.RemoteException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import junit.framework.TestCase;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteTableResultsInterface;
import au.edu.usyd.corona.server.session.ResultRetrieveException;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.types.BooleanType;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.ValueType;

/**
 * @author Edmund
 * 
 */
public class JDBCResultDAOTest extends TestCase {
	JDBCResultDAO rd;
	JDBCTaskDAO qd;
	JDBCDAOFactory factory;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		Network.initialize(Network.MODE_UNITTEST);
		factory = new JDBCDAOFactory();
		qd = (JDBCTaskDAO) factory.getTaskDAO();
		rd = (JDBCResultDAO) factory.getResultDAO();
	}
	
	@Override
	protected void tearDown() throws Exception {
		factory.clean();
		factory.close();
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCResultDAO#delete(int)}
	 * .
	 * 
	 * @throws DAOException
	 * @throws SQLException
	 */
	public void testDelete() throws DAOException, SQLException {
		int id = 0;
		
		// Normal: deleting existing table
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		rd.create(id, new String[]{"a"}, new Class<?>[]{IntType.class});
		assertTrue(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		Table t = new Table(new TaskID(id));
		t.addRow(new ValueType[]{new IntType(1)});
		rd.insert(t);
		rd.insert(t);
		
		rd.delete(id);
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		// Bad: deleting non-existing table
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		try {
			rd.delete(id);
			fail();
		}
		catch (DAOException e) {
		}
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCResultDAO#insert(au.edu.usyd.corona.srdb.Table)}
	 * .
	 * 
	 * @throws DAOException
	 * @throws SQLException
	 * @throws InterruptedException
	 * @throws RemoteException
	 * @throws ResultRetrieveException
	 */
	public void testInsert() throws DAOException, SQLException, InterruptedException, RemoteException, ResultRetrieveException {
		int id = 0;
		String allResults = "SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id;
		
		// Normal: Inserting an empty table
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		rd.create(id, new String[]{"a"}, new Class<?>[]{IntType.class});
		assertTrue(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		rd.insert(new Table(new TaskID(id)));
		assertEquals(0, rd.retrieve(allResults).getNumItems());
		
		// Normal: Inserting a populated table
		Table t = new Table(new TaskID(id));
		t.addRow(new ValueType[]{new IntType(1)});
		rd.insert(t);
		assertTrue(DBUtils.tableExists("TABLE_0", factory.getDataSource().getConnection(), factory.getSqlStatements()));
		assertEquals(1, rd.retrieve(allResults).getNumItems());
		rd.insert(t);
		assertEquals(2, rd.retrieve(allResults).getNumItems());
		
		// Bad: Inserting a table of wrong attributes
		t = new Table(new TaskID(id));
		t.addRow(new ValueType[]{new IntType(5), new BooleanType(false)});
		t.addRow(new ValueType[]{new IntType(5)});
		try {
			rd.insert(t);
			fail();
		}
		catch (DAOException e) {
			
		}
		assertEquals(2, rd.retrieve(allResults).getNumItems());
		
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCResultDAO#retrieve(java.lang.String)}
	 * .
	 * 
	 * @throws InterruptedException
	 * @throws DAOException
	 * @throws SQLException
	 * @throws ClassNotFoundException
	 * @throws IllegalAccessException
	 * @throws InstantiationException
	 * @throws RemoteException
	 * @throws ResultRetrieveException
	 */
	public void testRetrieve() throws InterruptedException, DAOException, SQLException, ClassNotFoundException, InstantiationException, IllegalAccessException, RemoteException, ResultRetrieveException {
		int id = 0;
		String allResults = "SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id + " ORDER BY a";
		
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		String[] attributeNames = new String[]{"a", "b"};
		Class<?>[] attributeTypes = new Class<?>[]{IntType.class, BooleanType.class};
		rd.create(id, attributeNames, attributeTypes);
		assertTrue(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		ArrayList<ValueType[]> rows = new ArrayList<ValueType[]>();
		rows.add(new ValueType[]{new IntType(1), new BooleanType(false)});
		rows.add(new ValueType[]{new IntType(3), new BooleanType(true)});
		rows.add(new ValueType[]{new IntType(4), new BooleanType(true)});
		rows.add(new ValueType[]{new IntType(5), new BooleanType(false)});
		rows.add(new ValueType[]{new IntType(23), new BooleanType(true)});
		rows.add(new ValueType[]{new IntType(30), new BooleanType(false)});
		
		Table t = new Table(new TaskID(id));
		for (ValueType[] row : rows)
			t.addRow(row);
		rd.insert(t);
		assertEquals(rows.size(), rd.retrieve(allResults).getNumItems());
		
		// Normal: Meta-data
		RemoteTableResultsInterface res = rd.retrieve(allResults);
		assertEquals(res.getNumCols(), attributeNames.length);
		for (int i = 0; i < attributeNames.length; i++)
			assertEquals(res.getColumnNames()[i].toLowerCase(), attributeNames[i].toLowerCase());
		
		for (int i = 0; i < attributeTypes.length; i++) {
			assertEquals(((ValueType) attributeTypes[i].newInstance()).toJDBCObject().getClass(), res.getAttributes()[i]);
			assertEquals(((ValueType) attributeTypes[i].newInstance()).toJDBCObject().getClass().getName(), res.getItems(0, 1).get(0)[i].getClass().getName());
		}
		
		// Normal: Retrieving all results
		int count = 0;
		List<Object[]> allRows = res.getItems(0, res.getNumItems());
		for (Object[] row : allRows) {
			for (int i = 0; i < attributeTypes.length; i++)
				assertEquals(rows.get(count)[i].toJDBCObject(), row[i]);
			count++;
		}
		assertEquals(rows.size(), count);
		
		// Boundary - no rows
		res = rd.retrieve(allResults);
		allRows = res.getItems(0, 0);
		assertEquals(res.getNumCols(), attributeNames.length);
		for (int i = 0; i < attributeNames.length; i++)
			assertEquals(res.getColumnNames()[i].toLowerCase(), attributeNames[i].toLowerCase());
		
		assertEquals(allRows.size(), 0);
		
		// Bad - bad bounds
		res = rd.retrieve(allResults);
		allRows = res.getItems(-6, -5);
		assertEquals(allRows.size(), 0);
		
		// Bad - bad bounds
		res = rd.retrieve(allResults);
		allRows = res.getItems(-5, -6);
		assertEquals(allRows.size(), 0);
		
		// Boundary - single row
		res = rd.retrieve(allResults);
		allRows = res.getItems(0, 1);
		count = 0;
		for (Object[] row : allRows) {
			for (int i = 0; i < attributeTypes.length; i++)
				assertEquals(rows.get(count)[i].toJDBCObject(), row[i]);
			count++;
		}
		assertEquals(1, count);
		
		// Normal - first half
		res = rd.retrieve(allResults);
		allRows = res.getItems(-5, rows.size() / 2);
		count = 0;
		for (Object[] row : allRows) {
			for (int i = 0; i < attributeTypes.length; i++)
				assertEquals(rows.get(count)[i].toJDBCObject(), row[i]);
			count++;
		}
		assertEquals(rows.size() / 2, count);
		
		// Normal - second half
		res = rd.retrieve(allResults);
		allRows = res.getItems(rows.size() / 2, 100);
		count = 0;
		for (Object[] row : allRows) {
			for (int i = 0; i < attributeTypes.length; i++)
				assertEquals(rows.get(count + rows.size() / 2)[i].toJDBCObject(), row[i]);
			count++;
		}
		assertEquals(rows.size() / 2, count);
		
		// Normal - reverse order
		res = rd.retrieve("SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id + " ORDER BY a DESC");
		allRows = res.getItems(0, 100);
		count = 0;
		for (Object[] row : allRows) {
			for (int i = 0; i < attributeTypes.length; i++)
				assertEquals(rows.get(rows.size() - count - 1)[i].toJDBCObject(), row[i]);
			count++;
		}
		assertEquals(rows.size(), count);
		
		// Normal - where clause
		res = rd.retrieve("SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id + " WHERE a >= 5 ORDER BY a");
		allRows = res.getItems(0, 100);
		count = 0;
		for (Object[] row : allRows) {
			for (int i = 0; i < attributeTypes.length; i++)
				assertEquals(rows.get(count + 3)[i].toJDBCObject(), row[i]);
			count++;
		}
		assertEquals(3, count);
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCResultDAO#create(int, String[], Class[])}
	 * 
	 * @throws DAOException
	 * @throws SQLException
	 * @throws InterruptedException
	 */
	public void testCreate() throws DAOException, SQLException, InterruptedException {
		int id;
		
		id = 100;
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		// Normal: create an empty result table
		rd.create(id, new String[]{"a"}, new Class<?>[]{IntType.class});
		assertTrue(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		// Bad: creating a result table that already exists
		try {
			rd.create(id, new String[]{"a"}, new Class<?>[]{IntType.class});
			fail("Creating a table that already exists did not throw an exception.");
		}
		catch (DAOException e) {
		}
		
		// Bad: Negative id's 
		try {
			rd.create(-1, new String[]{"a"}, new Class<?>[]{IntType.class});
			fail("Creating a table with negative ID did not throw an exception.");
		}
		catch (DAOException e) {
		}
		
		// Normal: very large ID
		id = Integer.MAX_VALUE;
		rd.create(id, new String[]{"a"}, new Class<?>[]{IntType.class});
		assertTrue(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		// Bad: Creating table with no attributes
		try {
			rd.create(0, new String[]{}, new Class<?>[]{});
			fail("Should have thrown a DAO exception since the table has no attributes.");
		}
		catch (DAOException e) {
		}
		
		//		 Bad: Mismatched number of attributes, names > attributes
		try {
			rd.create(0, new String[]{"a"}, new Class<?>[]{});
			fail("Should have thrown a DAO exception since input attributes and its names mismatch.");
		}
		catch (DAOException e) {
		}
		
		// Bad: Mismatched number of attributes, names < attributes
		try {
			rd.create(0, new String[]{"a"}, new Class<?>[]{IntType.class, IntType.class});
			//			fail("Should have thrown a DAO exception since input attributes and its names mismatch.");
		}
		catch (DAOException e) {
		}
		
		// Bad: Bad attribute types
		try {
			rd.create(0, new String[]{"a"}, new Class<?>[]{int.class});
			fail("Should have thrown a DAO exception since the table has no attributes.");
		}
		catch (DAOException e) {
		}
	}
	
	public void testNumRows() throws InterruptedException, DAOException, RemoteException, SQLException, ResultRetrieveException {
		int id = 0;
		String allResults = "SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id;
		
		// Bad: Non-existent table
		assertEquals(rd.retrieve(allResults).getNumItems(), 0);
		
		// Normal: Inserting an empty table
		assertFalse(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		rd.create(id, new String[]{"a"}, new Class<?>[]{IntType.class});
		assertTrue(DBUtils.tableExists(JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		rd.insert(new Table(new TaskID(id)));
		assertEquals(0, rd.retrieve(allResults).getNumItems());
		
		// Normal: Inserting a populated table
		Table t = new Table(new TaskID(id));
		t.addRow(new ValueType[]{new IntType(1)});
		rd.insert(t);
		assertEquals(1, rd.retrieve(allResults).getNumItems());
		rd.insert(t);
		assertEquals(2, rd.retrieve(allResults).getNumItems());
		
		// Normal: Removing a table
		rd.delete(id);
		assertEquals(0, rd.retrieve(allResults).getNumItems());
	}
}
