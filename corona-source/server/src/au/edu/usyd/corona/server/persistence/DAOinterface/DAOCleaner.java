package au.edu.usyd.corona.server.persistence.DAOinterface;


/**
 * A class which simply runs the clean() method of the default DAO. The ant
 * command to execute this class is 'ant dao-clean' which will reset the DAO
 * (database) state (e.g. remove all tables).
 * 
 * @author Raymes Khoury
 * 
 */
public class DAOCleaner {
	public static void main(String[] args) throws DAOException {
		DAOFactory dao = DAOFactory.getInstance();
		dao.clean();
		dao.close();
	}
}
