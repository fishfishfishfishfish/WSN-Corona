package au.edu.usyd.corona.server.persistence.DAOinterface;


import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * This class represents an abstract Factory for DAO objects. DAO provide an
 * interface for access to data source without exposing the internal
 * implementation of that data source and its access code. The DAO Factory
 * constructs and provides access to these DAO objects. This class loads a
 * particular DAO Factory implementation on initialisation at runtime based on a
 * configuration file. The DAO Factory implementation can then be used to access
 * the data in the particular data source.
 * 
 * @author Raymes Khoury
 */
public abstract class DAOFactory {
	public static final String CONFIG_FILE = "config/dao.properties";
	public static final String PROPERTY_FACTORY_CLASS_FILE = "dao.factory.class";
	
	private static DAOFactory instance;
	
	protected DAOFactory() {
	}
	
	/**
	 * Return the singleton instance of this Factory
	 * 
	 * @return The singleton instance of this Factory
	 * @throws DAOException If the Factory implementation could not be
	 * initialised
	 */
	public synchronized static final DAOFactory getInstance() throws DAOException {
		init();
		return instance;
	}
	
	/**
	 * Initialises the implementation of the Factory, if it has not already been
	 * initialised.
	 * 
	 * @throws DAOException If the Factory implementation could not be
	 * initialised
	 */
	private static final void init() throws DAOException {
		if (instance != null)
			return;
		try {
			Properties properties = new Properties();
			properties.load(new FileInputStream(CONFIG_FILE));
			String className = properties.getProperty(PROPERTY_FACTORY_CLASS_FILE);
			if (className == null)
				throw new IOException("Invalid " + PROPERTY_FACTORY_CLASS_FILE + " property");
			instance = (DAOFactory) Class.forName(className).newInstance();
		}
		catch (IOException e) {
			throw new DAOException("Unable to parse configuration	file", e);
		}
		catch (InstantiationException e2) {
			throw new DAOException("Cannot construct factory class.", e2);
		}
		catch (IllegalAccessException e3) {
			throw new DAOException("Cannot construct factory class.", e3);
		}
		catch (ClassNotFoundException e4) {
			throw new DAOException("Cannot construct factory class.", e4);
		}
		
	}
	
	/**
	 * Clean the DAOs by removing all existing data and starting from scratch.
	 * This should be seldom called.
	 * 
	 * @throws DAOException If there is a problem during cleanup
	 */
	public abstract void clean() throws DAOException;
	
	/**
	 * Close the DAOFactory performing any cleanup necessary. The DAOFactory and
	 * its associated DAOs should not be used again without calling init()
	 * 
	 * @throws DAOException If there is a problem closing the DAOFactory
	 */
	public abstract void close() throws DAOException;
	
	/**
	 * Return a DAO for Task object access
	 * 
	 * @return A DAO for Task object access
	 * @throws DAOException If there is a problem creating a DAO object
	 */
	public abstract TaskDAO getTaskDAO() throws DAOException;
	
	/**
	 * Return a DAO for Query object access
	 * 
	 * @return A DAO for Query object access
	 * @throws DAOException If there is a problem creating a DAO object
	 */
	public abstract QueryDAO getQueryDAO() throws DAOException;
	
	/**
	 * Return a DAO for Result access
	 * 
	 * @return A DAO for Result access
	 * @throws DAOException If there is a problem creating a DAO object
	 */
	public abstract ResultDAO getResultDAO() throws DAOException;
	
	/**
	 * Return a DAO for User object access
	 * 
	 * @return A DAO for User object access
	 * @throws DAOException If there is a problem creating a DAO object
	 */
	public abstract UserDAO getUserDAO() throws DAOException;
}
