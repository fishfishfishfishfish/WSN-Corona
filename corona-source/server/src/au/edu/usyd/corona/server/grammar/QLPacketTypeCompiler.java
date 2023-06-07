package au.edu.usyd.corona.server.grammar;


import java.io.IOException;

import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.grammar.TokenParseException;
import au.edu.usyd.corona.scheduler.SchedulableTask;

/**
 * Abstract base class for all the compilers for different parts of the SQL
 * grammar. The AST to compiler over is passed in upon construction, and the
 * actual compiling is done when the {@link #compile()} method is called.
 * 
 * @author Tim Dawborn
 * @param <T> the type of {@link SchedulableTask} that the compiler returns
 * @see QLCompiler
 */
abstract class QLPacketTypeCompiler<T extends SchedulableTask> {
	/** the maximum length that a filename can be provided in a SQL query */
	public static final int MAX_FILENAME_LENGTH = 10;
	
	protected final Tree root;
	protected final int queryId;
	
	public QLPacketTypeCompiler(Tree root, int queryId) {
		this.root = root;
		this.queryId = queryId;
	}
	
	/**
	 * Executes the compiler on the AST provided upon construction of the
	 * instance
	 * 
	 * @return the tokenized version of the given query
	 * @throws QLCompileException
	 * @throws TokenParseException
	 * @throws IOException
	 */
	abstract T compile() throws QLCompileException, TokenParseException, IOException;
}
