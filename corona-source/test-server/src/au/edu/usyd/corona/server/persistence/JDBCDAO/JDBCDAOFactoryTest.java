package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.FileInputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Properties;

import junit.framework.TestCase;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.TaskID;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.ResultDAO;
import au.edu.usyd.corona.server.util.PropertiesUtil;
import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.types.IntType;
import au.edu.usyd.corona.types.LongType;
import au.edu.usyd.corona.types.ValueType;

public class JDBCDAOFactoryTest extends TestCase {
	JDBCDAOFactory factory;
	Connection conn;
	Statement stmt;
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		
		Network.initialize(Network.MODE_UNITTEST);
		
		// Load the database config
		Properties properties = new Properties();
		properties.load(new FileInputStream(JDBCDAOFactory.CONFIG_FILE));
		String driver = PropertiesUtil.getProperty(JDBCDAOFactory.PROPERTIES_DB_DRIVER, properties);
		String url = PropertiesUtil.getProperty(JDBCDAOFactory.PROPERTIES_DB_URL, properties);
		String username = PropertiesUtil.getProperty(JDBCDAOFactory.PROPERTIES_DB_USERNAME, properties);
		String password = PropertiesUtil.getProperty(JDBCDAOFactory.PROPERTIES_DB_PASSWORD, properties);
		
		// Initialise database driver
		Class.forName(driver).newInstance();
		conn = DriverManager.getConnection(url, username, password);
		conn.setAutoCommit(false);
		stmt = conn.createStatement();
		
		factory = new JDBCDAOFactory();
	}
	
	@Override
	protected void tearDown() throws Exception {
		factory.clean();
		factory.close();
		super.tearDown();
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCDAOFactory#getTaskDAO()}
	 * .
	 */
	public void testGetQueryDAO() {
		try {
			factory.getTaskDAO();
		}
		catch (DAOException e) {
			fail("Cannot get QueryDAO. " + e);
		}
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCDAOFactory#getResultDAO()}
	 * .
	 */
	public void testGetResultDAO() {
		try {
			factory.getResultDAO();
		}
		catch (DAOException e) {
			fail("Cannot get ResultDAO. " + e);
		}
	}
	
	/**
	 * Test method for
	 * {@link au.edu.usyd.corona.server.persistence.JDBCDAO.JDBCDAOFactory#getUserDAO()}
	 * .
	 */
	public void testGetUserDAO() {
		try {
			factory.getUserDAO();
		}
		catch (DAOException e) {
			fail("Cannot get UserDAO. " + e);
		}
	}
	
	public void testGetTypeKeywords() {
		ArrayList<Integer> cases = new ArrayList<Integer>();
		cases.add(Types.ARRAY);
		cases.add(Types.BIGINT);
		cases.add(Types.BINARY);
		cases.add(Types.BIT);
		cases.add(Types.BLOB);
		cases.add(Types.BOOLEAN);
		cases.add(Types.CHAR);
		cases.add(Types.CLOB);
		cases.add(Types.DATALINK);
		cases.add(Types.DATE);
		cases.add(Types.DECIMAL);
		cases.add(Types.DISTINCT);
		cases.add(Types.DOUBLE);
		cases.add(Types.FLOAT);
		cases.add(Types.INTEGER);
		cases.add(Types.JAVA_OBJECT);
		//		cases.add(Types.LONGNVARCHAR); 1.6
		cases.add(Types.LONGVARBINARY);
		cases.add(Types.LONGVARCHAR);
		//		cases.add(Types.NCHAR); 1.6
		//		cases.add(Types.NCLOB); 1.6
		cases.add(Types.NULL);
		cases.add(Types.NUMERIC);
		//		cases.add(Types.NVARCHAR); 1.6
		cases.add(Types.OTHER);
		cases.add(Types.REAL);
		cases.add(Types.REF);
		//		cases.add(Types.ROWID); 1.6
		cases.add(Types.SMALLINT);
		//		cases.add(Types.SQLXML); 1.6
		cases.add(Types.STRUCT);
		cases.add(Types.TIME);
		cases.add(Types.TIMESTAMP);
		cases.add(Types.TINYINT);
		cases.add(Types.VARBINARY);
		cases.add(Types.VARCHAR);
		cases.add(Integer.MAX_VALUE);
		cases.add(Integer.MIN_VALUE);
		cases.add(0);
	}
	
	/**
	 * 
	 * @throws DAOException
	 * @throws SQLException
	 */
	public void testCountTable() throws DAOException, SQLException {
		int id = 0;
		// Bad - no table
		try {
			DBUtils.countTable("SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements());
			fail();
		}
		catch (DAOException e) {
			
		}
		
		ResultDAO rd = factory.getResultDAO();
		rd.create(id, new String[]{"a", "b"}, new Class[]{IntType.class, LongType.class});
		
		// Boundary - no rows
		assertEquals(0, DBUtils.countTable("SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		Table t = new Table(new TaskID(id));
		t.addRow(new ValueType[]{new IntType(5), new LongType(10)});
		t.addRow(new ValueType[]{new IntType(5), new LongType(10)});
		t.addRow(new ValueType[]{new IntType(5), new LongType(10)});
		rd.insert(t);
		
		// Normal
		assertEquals(3, DBUtils.countTable("SELECT * FROM " + JDBCResultDAO.TABLE_PREFIX + id, factory.getDataSource().getConnection(), factory.getSqlStatements()));
	}
	
	public void testTableExists() throws Exception {
		// Zero: null input
		try {
			DBUtils.tableExists(null, factory.getDataSource().getConnection(), factory.getSqlStatements());
			fail("Exception not thrown on null input");
		}
		catch (IllegalArgumentException e) {
		}
		
		// Zero: table does not exist
		assertFalse(DBUtils.tableExists("test_table", factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
		// Normal: table does exist
		stmt.executeUpdate("CREATE TABLE test_table (id INTEGER PRIMARY KEY);");
		assertTrue(DBUtils.tableExists("TEST_TABLE", factory.getDataSource().getConnection(), factory.getSqlStatements()));
		
	};
}
