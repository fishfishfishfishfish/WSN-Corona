package au.edu.usyd.corona.server.srdb;


import java.util.logging.Logger;

import au.edu.usyd.corona.server.persistence.DAOinterface.DAOException;
import au.edu.usyd.corona.server.persistence.DAOinterface.DAOFactory;
import au.edu.usyd.corona.server.persistence.DAOinterface.ResultDAO;
import au.edu.usyd.corona.middleLayer.Network;
import au.edu.usyd.corona.scheduler.ChildResultStore;
import au.edu.usyd.corona.srdb.Table;
import au.edu.usyd.corona.srdb.TableOperator;
import au.edu.usyd.corona.types.InvalidOperationException;

/**
 * This class forwards the result of table operations to its parent node
 * 
 * @author Raymes Khoury
 */
public class BaseForwardOperator extends TableOperator {
	private static final Logger logger = Logger.getLogger(BaseForwardOperator.class.getCanonicalName());
	
	private final ChildResultStore childResults;
	
	public BaseForwardOperator(TableOperator t, ChildResultStore childResults) {
		children = new TableOperator[]{t};
		this.childResults = childResults;
	}
	
	/**
	 * Forwards the result of table operations to the parent node, or writes the
	 * contents to a flash file. Returns the resultant table.
	 */
	@Override
	public Table eval(int epoch) throws InvalidOperationException {
		Table table = children[0].eval(epoch);
		
		if (Network.getInstance().getMode() == Network.MODE_UNITTEST)
			return table;
		
		try {
			ResultDAO rd = DAOFactory.getInstance().getResultDAO();
			rd.insert(table);
		}
		catch (DAOException e) {
			logger.severe("Could not persist table: " + e.getMessage());
		}
		
		// Remove old results
		childResults.removeResults(epoch);
		
		return table;
	}
	
	@Override
	public StringBuffer toTokens() {
		StringBuffer b = new StringBuffer();
		b.append(T_FORWARD).append(T_GROUP_OPEN).append(children[0].toTokens());
		return b.append(T_GROUP_CLOSE);
	}
}
