package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.beans.PropertyVetoException;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.logging.Logger;

import javax.xml.parsers.ParserConfigurationException;

import org.xml.sax.SAXException;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.QueryDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.ResultDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.TaskDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.UserDAO;
import au.edu.usyd.corona.server.util.PropertiesUtil;

import com.mchange.v2.c3p0.ComboPooledDataSource;

/**
 * This class represents a specific implementation of the DAOFactory for JDBC
 * data sources. Details of the particular database, such as the database driver
 * and URL are specified in the given configuration file. SQL for particular
 * databases that are connected to is also specified externally in an XML file,
 * such that the SQL can be altered for the chosen database.
 * 
 * @author Raymes Khoury
 */
public class JDBCDAOFactory extends DAOFactory {
	public static final String CONFIG_FILE = "config/jdbc-dao.properties"; // The configuration file to load data from  
	
	public static final String PROPERTIES_SQL_FILE = "database.sql.filename";
	public static final String PROPERTIES_DB_NAME = "database.name";
	public static final String PROPERTIES_DB_DRIVER = "database.driver";
	public static final String PROPERTIES_DB_URL = "database.url";
	public static final String PROPERTIES_DB_USERNAME = "database.username";
	public static final String PROPERTIES_DB_PASSWORD = "database.password";
	
	public static final String COUNT_TABLE_KEY = "COUNT_TABLE";
	public static final String NAMED_PARAM_QUERY = "{query}";
	public static final String CHECK_TABLE_KEY = "CHECK_TABLE";
	public static final String DROP_TABLE_KEY = "DROP_TABLE";
	public static final String NAMED_PARAM_TABLE = "{table}";
	
	private static final Logger logger = Logger.getLogger(JDBCDAOFactory.class.getCanonicalName());
	
	private ComboPooledDataSource dataSource;
	private SQLLoader sqlStatements;
	
	private Map<Integer, List<String>> jdbcTypeToKeyword;
	
	/**
	 * Construct a new JDBCDAOFactory
	 * 
	 * @throws DAOException If there is a problem connecting to the database or
	 * loading configuration files.
	 */
	public JDBCDAOFactory() throws DAOException {
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(CONFIG_FILE));
			
			setupDataSource(properties);
			String file = PropertiesUtil.getProperty(PROPERTIES_SQL_FILE, properties);
			String dbName = PropertiesUtil.getProperty(PROPERTIES_DB_NAME, properties);
			
			sqlStatements = new SQLLoader(file, dbName);
			
			// Initialises all required tables (Order is important, since query references users)
			// We should not die if initialisation fails such that we can still access the DAOs
			try {
				new JDBCUserDAO(dataSource, sqlStatements).init();
				new JDBCTaskDAO(dataSource, sqlStatements).init();
				new JDBCQueryDAO(dataSource, sqlStatements).init();
			}
			catch (DAOException e) {
				logger.warning("Could not init DAOs: " + e.getMessage());
			}
			
			jdbcTypeToKeyword = new HashMap<Integer, List<String>>();
			
			loadJDBCTypes();
		}
		catch (FileNotFoundException e) {
			throw new DAOException("Could not load properties.", e);
		}
		catch (IOException e) {
			throw new DAOException("Could not load properties.", e);
		}
		catch (PropertyVetoException e) {
			throw new DAOException("Could not load JDBC driver class.", e);
		}
		catch (SAXException e) {
			throw new DAOException("Could not load SQL XML file.", e);
		}
		catch (ParserConfigurationException e) {
			throw new DAOException("Could not load SQL XML file.", e);
		}
		catch (SQLException e) {
			throw new DAOException("Could not load type info.", e);
		}
	}
	
	private void loadJDBCTypes() throws SQLException {
		Connection conn = dataSource.getConnection();
		ResultSet typeInfo = conn.getMetaData().getTypeInfo();
		while (typeInfo.next()) {
			if (!jdbcTypeToKeyword.containsKey(typeInfo.getInt(2)))
				jdbcTypeToKeyword.put(typeInfo.getInt(2), new LinkedList<String>());
			jdbcTypeToKeyword.get(typeInfo.getInt(2)).add(typeInfo.getString(1));
		}
		if (!jdbcTypeToKeyword.containsKey(java.sql.Types.BOOLEAN)) {
			jdbcTypeToKeyword.put(java.sql.Types.BOOLEAN, new LinkedList<String>());
			jdbcTypeToKeyword.get(java.sql.Types.BOOLEAN).add("BOOLEAN");
			
		}
		if (!jdbcTypeToKeyword.containsKey(java.sql.Types.BIT)) {
			jdbcTypeToKeyword.put(java.sql.Types.BIT, new LinkedList<String>());
			jdbcTypeToKeyword.get(java.sql.Types.BIT).add("BOOLEAN");
			
		}
		typeInfo.close();
		conn.close();
		
	}
	
	/**
	 * Setup the JDBC Data Source. Connection pooling is used through the c3p0
	 * library.
	 * 
	 * @param properties The properties file containing the details of the
	 * database to load
	 * @throws PropertyVetoException If there is a problem loading a property
	 * from the config file
	 * @throws IOException If there is a problem loading a property from the
	 * config file
	 */
	private synchronized void setupDataSource(Properties properties) throws PropertyVetoException, IOException {
		String driver = PropertiesUtil.getProperty(PROPERTIES_DB_DRIVER, properties);
		String url = PropertiesUtil.getProperty(PROPERTIES_DB_URL, properties);
		String username = PropertiesUtil.getProperty(PROPERTIES_DB_USERNAME, properties);
		String password = PropertiesUtil.getProperty(PROPERTIES_DB_PASSWORD, properties);
		
		dataSource = new ComboPooledDataSource();
		dataSource.setDriverClass(driver);
		dataSource.setJdbcUrl(url);
		dataSource.setUser(username);
		dataSource.setPassword(password);
	}
	
	@Override
	public synchronized TaskDAO getTaskDAO() throws DAOException {
		return new JDBCTaskDAO(dataSource, sqlStatements);
	}
	
	@Override
	public synchronized ResultDAO getResultDAO() throws DAOException {
		return new JDBCResultDAO(dataSource, sqlStatements, jdbcTypeToKeyword);
	}
	
	@Override
	public synchronized UserDAO getUserDAO() throws DAOException {
		return new JDBCUserDAO(dataSource, sqlStatements);
	}
	
	@Override
	public synchronized QueryDAO getQueryDAO() throws DAOException {
		return new JDBCQueryDAO(dataSource, sqlStatements);
	}
	
	@Override
	public synchronized void clean() throws DAOException {
		Connection conn = null;
		ResultSet rs = null;
		String table = null;
		try {
			conn = dataSource.getConnection();
			rs = conn.getMetaData().getTables(null, null, "%", new String[]{"TABLE"});
			
			while (rs.next()) {
				try {
					table = rs.getString("TABLE_NAME");
					logger.info("Deleting table: " + rs.getString("TABLE_NAME"));
					DBUtils.deleteTable(table, conn, sqlStatements);
					logger.info("Success!");
				}
				catch (DAOException e) {
					logger.warning("Could not delete table: " + rs.getString("TABLE_NAME") + ": " + e);
				}
			}
		}
		catch (SQLException e) {
			throw new DAOException("Could not delete tables.", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeConnection(conn);
		}
	}
	
	@Override
	public synchronized void close() {
		dataSource.close();
	}
	
	public ComboPooledDataSource getDataSource() {
		return dataSource;
	}
	
	public void setDataSource(ComboPooledDataSource dataSource) {
		this.dataSource = dataSource;
	}
	
	public SQLLoader getSqlStatements() {
		return sqlStatements;
	}
	
	public void setSqlStatements(SQLLoader sqlStatements) {
		this.sqlStatements = sqlStatements;
	}
	
	public int getNumConnectionsUsed() {
		try {
			return dataSource.getNumConnections();
		}
		catch (SQLException e) {
			return -1;
		}
	}
	
}
