package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;

/**
 * Some common methods for JDBC access. Most of these methods catch exceptions
 * for database operations that have no clear resolution. For example, if a
 * Connection cannot be properly closed, we have nothing to do except report the
 * error. This may be a rather simplistic view of the matter but is sufficient
 * for our purposes. In the future, Spring could be used to handle database
 * transactions and connections which provides more intelligent handling of
 * these cases.
 * 
 * @author Raymes Khoury
 * 
 */
public class DBUtils {
	private static final Logger logger = Logger.getLogger(DBUtils.class.getCanonicalName());
	
	public static final String DROP_TABLE_KEY = "DROP_TABLE";
	
	public static final String NAMED_PARAM_TABLE = "{table}";
	
	// hide the constructor
	private DBUtils() {
	}
	
	/**
	 * Rollback the given Connection
	 * 
	 * @param conn The Connection to rollback
	 */
	public static void rollbackConn(Connection conn) {
		try {
			if (conn != null)
				conn.rollback();
		}
		catch (SQLException e) {
			logger.severe("Could not rollback connection: " + e.getMessage());
		}
	}
	
	/**
	 * Close the given Connection
	 * 
	 * @param conn The Connection to close
	 */
	public static void closeConnection(Connection conn) {
		try {
			if (conn != null)
				conn.close();
		}
		catch (SQLException e) {
			logger.severe("Could not close connection: " + e.getMessage());
		}
	}
	
	/**
	 * Close the given Statement
	 * 
	 * @param st The Statement to close
	 */
	public static void closeStatement(Statement st) {
		try {
			if (st != null)
				st.close();
		}
		catch (SQLException e) {
			logger.severe("Could not close statement: " + e.getMessage());
		}
	}
	
	/**
	 * Close the given ResultSet
	 * 
	 * @param rs The ResultSet to close
	 */
	public static void closeResultSet(ResultSet rs) {
		try {
			if (rs != null)
				rs.close();
		}
		catch (SQLException e) {
			logger.severe("Could not close result set: " + e.getMessage());
		}
	}
	
	private static PreparedStatement countTableStatement(String sql, Connection conn, SQLLoader sqlStatements) throws DAOException {
		PreparedStatement count = null;
		try {
			Map<String, String> namedParamsCount = new HashMap<String, String>();
			namedParamsCount.put(JDBCDAOFactory.NAMED_PARAM_QUERY, sql);
			count = sqlStatements.buildSQLStatement(conn, JDBCDAOFactory.COUNT_TABLE_KEY, namedParamsCount);
			
			return count;
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not count the table.", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not count the table.", e);
		}
	}
	
	/**
	 * Return the number of rows that a query will return
	 * 
	 * @param sql The SQL query in question
	 * @param conn A connection to a datasource
	 * @param sqlStatements SQLLoader containing sql queries for the database
	 * @return The number of rows the sql query will return
	 * @throws DAOException If there is a problem connecting to the datasource
	 */
	public static int countTable(String sql, Connection conn, SQLLoader sqlStatements) throws DAOException {
		PreparedStatement count = null;
		ResultSet rs = null;
		try {
			count = countTableStatement(sql, conn, sqlStatements);
			
			rs = count.executeQuery();
			rs.next();
			return rs.getInt(1);
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not count the table.", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(count);
		}
	}
	
	private static PreparedStatement deleteTableStatement(String tableName, Connection conn, SQLLoader sqlStatements) throws DAOException {
		PreparedStatement delete = null;
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_TABLE, tableName);
			
			delete = sqlStatements.buildSQLStatement(conn, DROP_TABLE_KEY, namedParams);
			return delete;
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Unable to delete table", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Unable to delete table", e);
		}
	}
	
	/**
	 * Delete a table from the database
	 * 
	 * @param tableName The name of the table to delete
	 * @param conn A connection to a datasource
	 * @param sqlStatements SQLLoader containing sql queries for the database
	 * @throws DAOException If there is a problem connecting to the datasource
	 */
	public static void deleteTable(String tableName, Connection conn, SQLLoader sqlStatements) throws DAOException {
		PreparedStatement delete = null;
		try {
			delete = deleteTableStatement(tableName, conn, sqlStatements);
			
			delete.executeUpdate();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Unable to delete table", e);
		}
	}
	
	/**
	 * Check whether a table exists in the database
	 * 
	 * @param table The name of the table to check for existence
	 * @param conn A connection to a datasource
	 * @param sqlStatements SQLLoader containing sql queries for the database
	 * @return true if the table exists, else false
	 * @throws DAOException If there is a problem connecting to the datasource
	 */
	public static boolean tableExists(String table, Connection conn, SQLLoader sqlStatements) throws DAOException {
		if (table == null)
			throw new IllegalArgumentException("Null table name");
		
		ResultSet rs = null;
		try {
			rs = conn.getMetaData().getTables(null, null, table.trim().toUpperCase(), new String[]{"TABLE"});
			boolean res = rs.next();
			return res;
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not check the table for existence.", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
		}
	}
}
