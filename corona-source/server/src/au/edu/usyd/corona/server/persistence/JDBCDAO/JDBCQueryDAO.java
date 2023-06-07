package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.rmi.RemoteException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.logging.Logger;

import javax.sql.DataSource;

import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.QueryDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;

/**
 * A specific implementation of the TaskDAO for JDBC databases.
 * 
 * @author Raymes Khoury
 */
class JDBCQueryDAO implements QueryDAO {
	public static final String TABLE_NAME = "queries";
	
	// These are the keys which correspond to an SQL statement in the XML file
	public static final String INSERT_QUERY_KEY = "INSERT_QUERY";
	public static final String DELETE_QUERY_KEY = "DELETE_QUERY";
	public static final String RETRIEVE_QUERY_KEY = "RETRIEVE_QUERY";
	public static final String CREATE_QUERY_TABLE_KEY = "CREATE_QUERY_TABLE";
	public static final String GET_MAX_QUERYID_KEY = "GET_MAX_QUERYID";
	
	// These are the named parameters in the SQL which can be substituted by generated SQL
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_ORDER_BY = "{orderby}";
	public static final String NAMED_PARAM_START = "{start}";
	public static final String NAMED_PARAM_END = "{end}";
	public static final String NAMED_PARAM_NUM = "{num}";
	
	private final DataSource dataSource;
	private final SQLLoader sqlStatements;
	
	private static final Logger logger = Logger.getLogger(JDBCQueryDAO.class.getCanonicalName());
	
	/**
	 * Create a new JDBCQueryDAO object
	 * 
	 * @param dataSource The DataSource to obtain connections from
	 * @param sqlStatements An SQLLoader object to obtain SQL statements from
	 */
	public JDBCQueryDAO(DataSource dataSource, SQLLoader sqlStatements) {
		this.dataSource = dataSource;
		this.sqlStatements = sqlStatements;
	}
	
	/**
	 * Initialises the Query section of the database by attempting to create a
	 * Query table. If it cannot be created, a warning is logged.
	 * 
	 * @throws DAOException if there is a problem initialising the DAO
	 */
	public void init() throws DAOException {
		// Attempt to create the table
		Connection conn = null;
		PreparedStatement create = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			if (DBUtils.tableExists(TABLE_NAME, conn, sqlStatements)) {
				logger.warning("Table " + TABLE_NAME + " already exists.");
				return;
			}
			
			create = sqlStatements.buildSQLStatement(conn, CREATE_QUERY_TABLE_KEY);
			create.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly execute Query table creation: ", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly execute Query table creation: ", e);
		}
		finally {
			DBUtils.closeStatement(create);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void delete(int queryId) throws DAOException {
		Connection conn = null;
		PreparedStatement delete = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Object[] parameters = {queryId};
			delete = sqlStatements.buildSQLStatement(conn, DELETE_QUERY_KEY, parameters);
			delete.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly delete the Query", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly delete the Query", e);
		}
		finally {
			DBUtils.closeStatement(delete);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized int getHighestQueryID() throws DAOException {
		Connection conn = null;
		PreparedStatement queryIdStatement = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			// Check to see if the table is empty
			int numRows = DBUtils.countTable(sqlStatements.getSQLString(RETRIEVE_QUERY_KEY), conn, sqlStatements);
			if (numRows == 0) {
				return -1;
			}
			
			queryIdStatement = sqlStatements.buildSQLStatement(conn, GET_MAX_QUERYID_KEY);
			rs = queryIdStatement.executeQuery();
			
			rs.next();
			return rs.getInt(1);
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly retrieve the current QueryID", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly retrieve the current QueryID", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(queryIdStatement);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void insert(Query query) throws DAOException {
		Connection conn = null;
		PreparedStatement insert = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			Object[] parameters = {query.getQueryID(), query.getQuery(), query.getFirstExecutionTime(), query.getSubmittedTime(), query.getUser().getId(), query.getQueryID(), query.getRootTaskNodeID(), query.getRootTaskLocalID()};
			insert = sqlStatements.buildSQLStatement(conn, INSERT_QUERY_KEY, parameters);
			insert.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			e.printStackTrace();
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly insert the Query", e);
		}
		catch (IOException e) {
			e.printStackTrace();
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly insert the Query", e);
		}
		finally {
			DBUtils.closeStatement(insert);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized RemoteQueryResultsInterface retrieve(String whereClause, String orderByClause) throws DAOException, RemoteException {
		return new RemoteJDBCQueryResults(sqlStatements, dataSource, whereClause, orderByClause);
	}
	
}
