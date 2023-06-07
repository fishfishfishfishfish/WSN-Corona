package au.edu.usyd.corona.server.persistence.JDBCDAO;


import java.io.IOException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.sql.DataSource;

import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;
import au.edu.usyd.corona.server.session.QueryRetrieveException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;
import au.edu.usyd.corona.server.util.SQLExtractor;

/**
 * Implementation of RemoteQueryResultsInterface for JDBC databases
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class RemoteJDBCQueryResults extends UnicastRemoteObject implements RemoteQueryResultsInterface {
	public static final String RETRIEVE_QUERY_KEY = "RETRIEVE_QUERY";
	public static final String RETRIEVE_QUERY_LIMIT_KEY = "RETRIEVE_QUERY_LIMIT";
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_ORDER_BY = "{orderby}";
	public static final String NAMED_PARAM_START = "{start}";
	public static final String NAMED_PARAM_END = "{end}";
	public static final String NAMED_PARAM_NUM = "{num}";
	
	private final DataSource datasource;
	private String countQueries;
	private String retrieveQueries;
	
	private final SQLLoader sqlStatements;
	private final SQLExtractor sqlExtractorWhere;
	private final SQLExtractor sqlExtractorOrderBy;
	
	public RemoteJDBCQueryResults(SQLLoader sqlStatements, DataSource datasource, String whereClause, String orderByClause) throws DAOException, RemoteException {
		this.sqlExtractorWhere = new SQLExtractor(whereClause, SQLExtractor.Type.WHERE);
		this.sqlExtractorOrderBy = new SQLExtractor(orderByClause, SQLExtractor.Type.ORDER_BY);
		this.datasource = datasource;
		this.sqlStatements = sqlStatements;
		
		setupCountQueriesStatement();
		setupRetrieveQueriesStatement();
		
	}
	
	private void setupRetrieveQueriesStatement() throws DAOException {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_WHERE, sqlExtractorWhere.extractWhere());
			namedParams.put(NAMED_PARAM_ORDER_BY, sqlExtractorOrderBy.extractOrderBy());
			
			retrieveQueries = sqlStatements.getSQLString(RETRIEVE_QUERY_LIMIT_KEY, namedParams);
			
		}
		catch (SQLException e) {
			throw new DAOException("Could not properly retrieve the Querys", e);
		}
		catch (IOException e) {
			throw new DAOException("Could not properly retrieve the Querys", e);
		}
	}
	
	public synchronized List<Query> getItems(int startEntry, int endEntry) throws QueryRetrieveException {
		if (startEntry < 0)
			startEntry = 0;
		
		if (endEntry < 0)
			endEntry = 0;
		
		if (endEntry <= startEntry)
			return new ArrayList<Query>();
		
		ArrayList<Query> results = new ArrayList<Query>();
		
		ResultSet rs = null;
		Connection conn = null;
		PreparedStatement retrieve = null;
		try {
			conn = datasource.getConnection();
			retrieve = conn.prepareStatement(retrieveQueries);
			retrieve.setInt(1, startEntry);
			retrieve.setInt(2, endEntry - startEntry);
			rs = retrieve.executeQuery();
			
			while (rs.next()) {
				Query current = new Query();
				current.setQueryID(rs.getInt("queryID"));
				current.setQuery(rs.getString("query"));
				current.setFirstExecutionTime(rs.getLong("firstExecutionTime"));
				current.setSubmittedTime(rs.getLong("submittedTime"));
				
				User.AccessLevel accessLevel;
				try {
					accessLevel = AccessLevel.valueOf(rs.getString("access_level"));
				}
				catch (IllegalArgumentException e) {
					accessLevel = User.DEFAULT_ACCESS_LEVEL;
				}
				User u = new User(rs.getInt("user"), rs.getString("username"), rs.getString("password"), accessLevel);
				current.setUser(u);
				current.setRootTaskLocalID(rs.getInt("localTaskID"));
				current.setRootTaskNodeID(rs.getLong("nodeID"));
				current.setExecutionTime(rs.getLong("executionTime"));
				current.setReschedulePeriod(rs.getLong("reschedulePeriod"));
				current.setRunCountTotal(rs.getInt("runCountTotal"));
				current.setRunCountLeft(rs.getInt("runCountLeft"));
				current.setStatus(rs.getInt("status"));
				
				results.add(current);
			}
		}
		catch (SQLException e) {
			throw new QueryRetrieveException("Could not properly retrieve the Queries: " + e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(retrieve);
			DBUtils.closeConnection(conn);
		}
		
		return results;
	}
	
	private void setupCountQueriesStatement() throws DAOException {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_WHERE, sqlExtractorWhere.extractWhere());
			
			countQueries = sqlStatements.getSQLString(RETRIEVE_QUERY_KEY, namedParams);
		}
		catch (IOException e) {
			throw new DAOException("Could not properly construct Query count SQL", e);
		}
		catch (SQLException e) {
			throw new DAOException("Could not properly count the Querys", e);
		}
	}
	
	public synchronized int getNumItems() throws QueryRetrieveException {
		Connection conn = null;
		int res = 0;
		try {
			conn = datasource.getConnection();
			try {
				res = DBUtils.countTable(countQueries, conn, sqlStatements);
			}
			catch (DAOException e) {
				throw new QueryRetrieveException("Could not properly count the Queries: " + e);
			}
		}
		catch (SQLException e) {
			throw new QueryRetrieveException("Could not properly count the Queries: " + e);
		}
		finally {
			DBUtils.closeConnection(conn);
		}
		return res;
	}
}
