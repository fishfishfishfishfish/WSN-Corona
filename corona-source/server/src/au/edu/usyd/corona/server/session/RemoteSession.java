package au.edu.usyd.corona.server.session;


import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.logging.Logger;

import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.middleLayer.TimeSync;
import au.edu.usyd.corona.scheduler.KillTask;
import au.edu.usyd.corona.scheduler.QueryTask;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.sensing.SenseManager;
import au.edu.usyd.corona.server.grammar.QLCompileException;
import au.edu.usyd.corona.server.grammar.QLCompiler;
import au.edu.usyd.corona.server.grammar.QLParseException;
import au.edu.usyd.corona.server.grammar.Query;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.QueryDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteQueryResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteTableResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.RemoteUserResultsInterface;
import au.edu.usyd.corona.server.persistence.DAOinterface.ResultDAO;
import au.edu.usyd.corona.server.persistence.DAOinterface.UserDAO;
import au.edu.usyd.corona.server.scheduler.BaseScheduler;
import au.edu.usyd.corona.server.session.notifier.NotifierID;
import au.edu.usyd.corona.server.session.notifier.NotifierManager;
import au.edu.usyd.corona.server.session.notifier.RemoteNotifierInterface;
import au.edu.usyd.corona.server.user.AccessDeniedException;
import au.edu.usyd.corona.server.user.User;
import au.edu.usyd.corona.server.user.User.AccessLevel;
import au.edu.usyd.corona.server.util.Hasher;
import au.edu.usyd.corona.server.util.IDGenerator;

@SuppressWarnings("serial")
class RemoteSession extends UnicastRemoteObject implements RemoteSessionInterface {
	public static final int MAX_QUERIES_RETURNED = 1000;
	
	private static final Logger logger = Logger.getLogger(RemoteSession.class.getCanonicalName());
	
	private final User user;
	private final IDGenerator idGenerator;
	private ResultDAO resultDAO;
	private UserDAO userDAO;
	private QueryDAO queryDAO;
	
	public RemoteSession(User user, IDGenerator idGenerator) throws RemoteException {
		this.user = user;
		this.idGenerator = idGenerator;
		try {
			this.resultDAO = DAOFactory.getInstance().getResultDAO();
			this.userDAO = DAOFactory.getInstance().getUserDAO();
			this.queryDAO = DAOFactory.getInstance().getQueryDAO();
		}
		catch (DAOException e) {
			logger.severe("Could not construct RemoteSession: " + e);
			throw new RemoteException(e.toString());
		}
	}
	
	public Query executeQuery(String queryString) throws RemoteException, QueryExecuteException {
		SchedulableTask task = null;
		Query query = null;
		try {
			
			// compiles the query
			synchronized (idGenerator) {
				task = QLCompiler.getInstance().compile(queryString, idGenerator.current(), user);
				idGenerator.next();
			}
			
			// Checks permission to execute that query
			if (user.getAccessLevel() != AccessLevel.ADMIN && task instanceof KillTask) {
				int id = ((KillTask) task).getKillID();
				if (queryDAO.retrieve("user = " + user.getId() + " AND id = " + id, "").getNumItems() < 1) {
					throw new AccessDeniedException("User has no permission to Kill: " + id);
				}
			}
			
			// Only create a results table if the Task is one that returns results
			if (task instanceof QueryTask) {
				QueryTask baseTask = (QueryTask) task;
				resultDAO.create(baseTask.getTaskId().getQueryID(), baseTask.getAttributes(), baseTask.getBaseClassSchema());
			}
			
			BaseScheduler.getInstance().addTask(task);
			query = new Query(task.getTaskId().getQueryID(), queryString, TimeSync.getInstance().getTime(), task.getExecutionTime(), task.getExecutionTime(), task.getReschedulePeriod(), task.getRunCountLeft(), task.getRunCountTotal(), task.getStatus(), user, task.getTaskId().getNodeID(), task.getTaskId().getLocalTaskID());
			queryDAO.insert(query);
			
		}
		catch (QLParseException e) {
			throw new QueryExecuteException("Could not compile query: " + e.getMessage());
		}
		catch (QLCompileException e) {
			throw new QueryExecuteException("Could not compile query: " + e.getMessage());
		}
		catch (DAOException e) {
			e.printStackTrace();
			throw new QueryExecuteException(e.getMessage());
		}
		catch (Exception e) {
			e.printStackTrace();
		}
		
		NotifierManager.getInstance().updateAll(new NotifierID(NotifierID.NotifierType.QUERIES_TABLE_NOTIFIER));
		return query;
		
	}
	
	private String createPrivilagedWhereClause(String whereClause) {
		String where;
		if (user.getAccessLevel() != AccessLevel.ADMIN) { // limit visibility if user not admin
			if (whereClause == null || whereClause.trim().length() == 0)
				where = "user = " + user.getId();
			else
				where = "user = " + user.getId() + " AND (" + whereClause + ")";
		}
		else
			where = whereClause;
		return where;
	}
	
	public RemoteQueryResultsInterface retrieveQueries(String whereClause, String orderBy) throws RemoteException, QueryRetrieveException {
		String where = createPrivilagedWhereClause(whereClause);
		try {
			return DAOFactory.getInstance().getQueryDAO().retrieve(where, orderBy);
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new QueryRetrieveException(e.getMessage());
		}
	}
	
	public boolean deleteQuery(int queryId) throws RemoteException, QueryRetrieveException {
		if (user.getAccessLevel() != AccessLevel.ADMIN) {
			logger.warning(user.getUsername() + " tried to call addUser()");
			throw new AccessDeniedException("User not administrator");
		}
		
		try {
			logger.info("Deleting query (" + queryId + ")");
			QueryDAO qd = DAOFactory.getInstance().getQueryDAO();
			
			if (qd.retrieve("id = " + queryId, "").getNumItems() != 1) // Query does not exist
				return false;
			
			synchronized (idGenerator) {
				qd.delete(queryId);
			}
			NotifierManager.getInstance().updateAll(new NotifierID(NotifierID.NotifierType.QUERIES_TABLE_NOTIFIER));
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new QueryRetrieveException(e.getMessage());
		}
		
		return true;
	}
	
	public RemoteTableResultsInterface retrieveRows(String sql) throws RemoteException, ResultRetrieveException {
		logger.fine("Retrieving rows for: " + sql);
		try {
			return resultDAO.retrieve(sql);
		}
		catch (DAOException e) {
			throw new ResultRetrieveException("Could not retrieve results: " + e.getMessage());
		}
	}
	
	public RemoteUserResultsInterface getUsers(String whereClause, String orderByClause) throws UserAccessException, RemoteException {
		if (user.getAccessLevel() != AccessLevel.ADMIN) {
			logger.warning(user.getUsername() + " tried to call getUsers()");
			throw new AccessDeniedException("User not administrator");
		}
		
		try {
			logger.fine("Getting users in system");
			return userDAO.retrieveUsers(whereClause, orderByClause);
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new UserAccessException(e.getMessage());
		}
	}
	
	public void addUser(String username, String password, AccessLevel accessLevel) throws UserAccessException, RemoteException {
		if (user.getAccessLevel() != AccessLevel.ADMIN) {
			logger.warning(user.getUsername() + " tried to call addUser()");
			throw new AccessDeniedException("User not administrator");
		}
		
		try {
			logger.info("Adding a user (" + username + ", " + accessLevel + ")");
			userDAO.addUser(username, Hasher.hash(password), accessLevel);
			NotifierManager.getInstance().updateAll(new NotifierID(NotifierID.NotifierType.USERS_TABLE_NOTIFIER));
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new UserAccessException(e.getMessage());
		}
	}
	
	public void updateUser(int userId, String username, String password, AccessLevel accessLevel) throws UserAccessException, RemoteException {
		if (user.getId() != userId && user.getAccessLevel() != AccessLevel.ADMIN) {
			logger.warning(user.getUsername() + " tried to call updateUser()");
			throw new AccessDeniedException("User not administrator");
		}
		
		try {
			logger.info("Updating user (" + userId + ") to username (" + username + "), password and access level (" + accessLevel + ")");
			userDAO.updateUser(userId, username, Hasher.hash(password), accessLevel);
			NotifierManager.getInstance().updateAll(new NotifierID(NotifierID.NotifierType.USERS_TABLE_NOTIFIER));
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new UserAccessException(e.getMessage());
		}
	}
	
	public void deleteUser(String username) throws UserAccessException, RemoteException {
		if (user.getAccessLevel() != AccessLevel.ADMIN) {
			logger.warning(user.getUsername() + " tried to call addUser()");
			throw new AccessDeniedException("User not administrator");
		}
		
		try {
			logger.fine("Deleting user (" + username + ")");
			userDAO.deleteUser(username);
			NotifierManager.getInstance().updateAll(new NotifierID(NotifierID.NotifierType.USERS_TABLE_NOTIFIER));
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new UserAccessException(e.getMessage());
		}
	}
	
	public User getLoggedInUser() throws RemoteException {
		return user;
	}
	
	public void addNotifier(NotifierID notifierID, RemoteNotifierInterface notifier) throws RemoteException {
		NotifierManager.getInstance().add(notifierID, notifier);
	}
	
	public void removeNotifier(NotifierID notifierID, RemoteNotifierInterface notifier) throws RemoteException {
		NotifierManager.getInstance().remove(notifierID, notifier);
		
	}
	
	public void updatePassword(String oldPassword, String newPassword) throws UserAccessException, RemoteException {
		String passwordHash = Hasher.hash(oldPassword);
		
		try {
			User u = userDAO.checkPassword(user.getUsername(), passwordHash);
			if (u != null) {
				userDAO.updateUser(u.getId(), u.getUsername(), Hasher.hash(newPassword), u.getAccessLevel());
				NotifierManager.getInstance().updateAll(new NotifierID(NotifierID.NotifierType.USERS_TABLE_NOTIFIER));
			}
			else {
				throw new UserAccessException("Incorrect password.");
			}
		}
		catch (DAOException e) {
			logger.severe(e.getMessage());
			throw new UserAccessException(e.getMessage());
		}
	}
	
	public long getBasestationIEEEAddress() throws RemoteException {
		return Network.getInstance().getMyAddress();
	}
	
	public String[] getAttributeNames() throws RemoteException {
		// Return attributes minus the count column
		String[] src = SenseManager.getInstance().getColumnNames();
		String[] dest = new String[src.length - 1];
		System.arraycopy(src, 1, dest, 0, dest.length);
		return dest;
	}
}
