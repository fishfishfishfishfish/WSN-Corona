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

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteUserResultsInterface;
import au.edu.usyd.corona.server.session.UserRetrieveException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;
import au.edu.usyd.corona.server.util.SQLExtractor;

/**
 * Implementation of RemoteUserResultsInterface for JDBC databases
 * 
 * @author Raymes Khoury
 * 
 */
@SuppressWarnings("serial")
public class RemoteJDBCUserResults extends UnicastRemoteObject implements RemoteUserResultsInterface {
	protected static final String GET_USER_KEY = "GET_USER";
	protected static final String GET_USER_LIMIT_KEY = "GET_USER_LIMIT";
	public static final String NAMED_PARAM_WHERE = "{where}";
	public static final String NAMED_PARAM_ORDER_BY = "{orderby}";
	public static final String NAMED_PARAM_START = "{start}";
	public static final String NAMED_PARAM_END = "{end}";
	public static final String NAMED_PARAM_NUM = "{num}";
	
	private final DataSource datasource;
	private String countUsers;
	private String retrieveUsers;
	
	private final SQLLoader sqlStatements;
	private final SQLExtractor sqlExtractorWhere;
	private final SQLExtractor sqlExtractorOrderBy;
	
	public RemoteJDBCUserResults(SQLLoader sqlStatements, DataSource datasource, String whereClause, String orderByClause) throws DAOException, RemoteException {
		this.sqlExtractorWhere = new SQLExtractor(whereClause, SQLExtractor.Type.WHERE);
		this.sqlExtractorOrderBy = new SQLExtractor(orderByClause, SQLExtractor.Type.ORDER_BY);
		this.datasource = datasource;
		this.sqlStatements = sqlStatements;
		
		setupCountUsersStatement();
		setupRetrieveQueriesStatement();
		
	}
	
	private void setupRetrieveQueriesStatement() throws DAOException {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_WHERE, sqlExtractorWhere.extractWhere());
			namedParams.put(NAMED_PARAM_ORDER_BY, sqlExtractorOrderBy.extractOrderBy());
			
			retrieveUsers = sqlStatements.getSQLString(GET_USER_LIMIT_KEY, namedParams);
			
		}
		catch (SQLException e) {
			throw new DAOException("Could not properly retrieve the Users", e);
		}
		catch (IOException e) {
			throw new DAOException("Could not properly retrieve the Users", e);
		}
	}
	
	public synchronized List<User> getItems(int startEntry, int endEntry) throws UserRetrieveException {
		if (startEntry < 0)
			startEntry = 0;
		
		if (endEntry < 0)
			endEntry = 0;
		
		if (endEntry <= startEntry)
			return new ArrayList<User>();
		
		ArrayList<User> results = new ArrayList<User>();
		
		Connection conn = null;
		PreparedStatement retrieve = null;
		ResultSet rs = null;
		try {
			conn = datasource.getConnection();
			retrieve = conn.prepareStatement(retrieveUsers);
			retrieve.setInt(1, startEntry);
			retrieve.setInt(2, endEntry - startEntry);
			rs = retrieve.executeQuery();
			
			while (rs.next()) {
				int id = rs.getInt(1);
				String username = rs.getString(2);
				AccessLevel accessLevel;
				
				try {
					accessLevel = AccessLevel.valueOf(rs.getString(3));
				}
				catch (IllegalArgumentException e) {
					accessLevel = User.DEFAULT_ACCESS_LEVEL;
				}
				
				results.add(new User(id, username, null, accessLevel));
			}
		}
		catch (SQLException e) {
			throw new UserRetrieveException("Could not properly retrieve the Users: " + e);
		}
		finally {
			DBUtils.closeResultSet(rs);
			DBUtils.closeStatement(retrieve);
			DBUtils.closeConnection(conn);
		}
		
		return results;
	}
	
	private void setupCountUsersStatement() throws DAOException {
		try {
			Map<String, String> namedParams = new HashMap<String, String>();
			namedParams.put(NAMED_PARAM_WHERE, sqlExtractorWhere.extractWhere());
			
			countUsers = sqlStatements.getSQLString(GET_USER_KEY, namedParams);
		}
		catch (IOException e) {
			throw new DAOException("Could not properly construct Query count SQL", e);
		}
		catch (SQLException e) {
			throw new DAOException("Could not properly count the Querys", e);
		}
	}
	
	public synchronized int getNumItems() throws UserRetrieveException {
		Connection conn = null;
		int res = 0;
		try {
			conn = datasource.getConnection();
			try {
				res = DBUtils.countTable(countUsers, conn, sqlStatements);
			}
			catch (DAOException e) {
				throw new UserRetrieveException("Could not properly count the Queries: " + e);
			}
		}
		catch (SQLException e) {
			throw new UserRetrieveException("Could not properly count the Queries: " + e);
		}
		finally {
			DBUtils.closeConnection(conn);
		}
		return res;
	}
	
}
