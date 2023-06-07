package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.TaskDAO;
import au.edu.usyd.corona.server.util.SQLExtractor;
import au.edu.usyd.corona.scheduler.TaskDetails;
import au.edu.usyd.corona.scheduler.TaskID;

/**
 * A specific implementation of the TaskDAO for JDBC databases.
 * 
 * @author Raymes Khoury
 */
class JDBCTaskDAO implements TaskDAO {
	public static final String TABLE_NAME = "tasks";
	
	// These are the keys which correspond to an SQL statement in the XML file
	public static final String INSERT_TASK_KEY = "INSERT_TASK";
	public static final String UPDATE_TASK_KEY = "UPDATE_TASK";
	public static final String DELETE_TASK_KEY = "DELETE_TASK";
	public static final String RETRIEVE_TASK_KEY = "RETRIEVE_TASK";
	public static final String RETRIEVE_TASK_KEY_LIMIT_KEY = "RETRIEVE_TASK_LIMIT";
	public static final String CREATE_TASK_TABLE_KEY = "CREATE_TASK_TABLE";
	public static final String GET_MAX_TASKID_KEY = "GET_MAX_TASKID";
	
	// These are the named parameters in the SQL which can be substituted by generated SQL
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_ORDER_BY = "{orderby}";
	public static final String NAMED_PARAM_START = "{start}";
	public static final String NAMED_PARAM_END = "{end}";
	public static final String NAMED_PARAM_NUM = "{num}";
	
	private final DataSource dataSource;
	private final SQLLoader sqlStatements;
	
	/**
	 * Create a new JDBCTaskDAO object
	 * 
	 * @param dataSource The DataSource to obtain connections from
	 * @param sqlStatements An SQLLoader object to obtain SQL statements from
	 */
	public JDBCTaskDAO(DataSource dataSource, SQLLoader sqlStatements) {
		this.dataSource = dataSource;
		this.sqlStatements = sqlStatements;
	}
	
	/**
	 * Initialises the Task section of the database by attempting to create a
	 * Task table. If it cannot be created, a warning is logged.
	 * 
	 * @throws DAOException if there is a problem initialising the DAO
	 */
	public void init() throws DAOException {
		Connection conn = null;
		PreparedStatement create = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			// Attempt to create the table
			if (DBUtils.tableExists(TABLE_NAME, conn, sqlStatements))
				return;
			
			create = sqlStatements.buildSQLStatement(conn, CREATE_TASK_TABLE_KEY);
			create.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly execute Task table creation: ", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly execute Task table creation: ", e);
		}
		finally {
			DBUtils.closeStatement(create);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void delete(TaskID taskId) throws DAOException {
		Connection conn = null;
		PreparedStatement delete = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Object[] parameters = {taskId.getQueryID(), taskId.getNodeID(), taskId.getLocalTaskID()};
			delete = sqlStatements.buildSQLStatement(conn, DELETE_TASK_KEY, parameters);
			delete.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly delete the Task", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly delete the Task", e);
		}
		finally {
			DBUtils.closeStatement(delete);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void insert(TaskDetails task) throws DAOException {
		Connection conn = null;
		PreparedStatement insert = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			Object[] parameters = {task.getTaskId().getQueryID(), task.getTaskId().getNodeID(), task.getTaskId().getLocalTaskID(), task.getExecutionTime(), task.getReschedulePeriod(), task.getRunCountTotal(), task.getRunCountLeft(), task.getStatus()};
			
			insert = sqlStatements.buildSQLStatement(conn, INSERT_TASK_KEY, parameters);
			insert.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			e.printStackTrace();
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly insert the Task", e);
		}
		catch (IOException e) {
			e.printStackTrace();
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly insert the Task", e);
		}
		finally {
			DBUtils.closeStatement(insert);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized void update(TaskDetails task) throws DAOException {
		Connection conn = null;
		PreparedStatement update = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Object[] parameters = {task.getExecutionTime(), task.getReschedulePeriod(), task.getRunCountTotal(), task.getRunCountLeft(), task.getStatus(), task.getTaskId().getQueryID(), task.getTaskId().getNodeID(), task.getTaskId().getLocalTaskID()};
			
			update = sqlStatements.buildSQLStatement(conn, UPDATE_TASK_KEY, parameters);
			update.executeUpdate();
			conn.commit();
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly update the Task", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly update the Task", e);
		}
		finally {
			DBUtils.closeStatement(update);
			DBUtils.closeConnection(conn);
		}
	}
	
	public synchronized List<TaskDetails> retrieve(String whereClause, String orderByClause, int startEntry, int endEntry) throws DAOException {
		if (startEntry < 0)
			startEntry = 0;
		
		if (endEntry < 0)
			endEntry = 0;
		
		if (endEntry <= startEntry)
			return new ArrayList<TaskDetails>();
		
		String where, orderBy;
		try {
			where = new SQLExtractor(whereClause, SQLExtractor.Type.WHERE).extractWhere();
			orderBy = new SQLExtractor(orderByClause, SQLExtractor.Type.ORDER_BY).extractOrderBy();
		}
		catch (SQLException e) {
			throw new DAOException("Could not properly retrieve the Tasks", e);
		}
		
		ArrayList<TaskDetails> results = new ArrayList<TaskDetails>();
		
		Connection conn = null;
		PreparedStatement retrieve = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_WHERE, where);
			namedParams.put(NAMED_PARAM_ORDER_BY, orderBy);
			namedParams.put(NAMED_PARAM_START, String.valueOf(startEntry));
			namedParams.put(NAMED_PARAM_END, String.valueOf(endEntry));
			namedParams.put(NAMED_PARAM_NUM, String.valueOf(endEntry - startEntry));
			
			retrieve = sqlStatements.buildSQLStatement(conn, RETRIEVE_TASK_KEY_LIMIT_KEY, namedParams);
			rs = retrieve.executeQuery();
			
			while (rs.next()) {
				TaskDetails current = new TaskDetails();
				TaskID currentID = new TaskID(rs.getInt("localTaskID"), rs.getLong("nodeID"), rs.getInt("qID"));
				current.setTaskID(currentID);
				current.setExecutionTime(rs.getLong("executionTime"));
				current.setReschedulePeriod(rs.getLong("reschedulePeriod"));
				current.setRunCountLeft(rs.getInt("runCountLeft"));
				current.setRunCountTotal(rs.getInt("runCountTotal"));
				current.setStatus(rs.getInt("status"));
				results.add(current);
			}
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly retrieve the Tasks", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly retrieve the Tasks", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(retrieve);
			DBUtils.closeConnection(conn);
		}
		
		return results;
	}
	
	public synchronized int getHighestLocalTaskID(long node) throws DAOException {
		Connection conn = null;
		PreparedStatement queryIdStatement = null;
		ResultSet rs = null;
		try {
			conn = dataSource.getConnection();
			conn.setAutoCommit(false);
			
			// Check to see if the table is empty
			int numRows = DBUtils.countTable(sqlStatements.getSQLString(RETRIEVE_TASK_KEY), conn, sqlStatements);
			if (numRows == 0) {
				return 0;
			}
			
			queryIdStatement = sqlStatements.buildSQLStatement(conn, GET_MAX_TASKID_KEY);
			queryIdStatement.setLong(1, node);
			rs = queryIdStatement.executeQuery();
			
			rs.next();
			return rs.getInt(1);
		}
		catch (SQLException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly retrieve the current TaskID", e);
		}
		catch (IOException e) {
			DBUtils.rollbackConn(conn);
			throw new DAOException("Could not properly retrieve the current TaskID", e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(queryIdStatement);
			DBUtils.closeConnection(conn);
		}
	}
	
}
