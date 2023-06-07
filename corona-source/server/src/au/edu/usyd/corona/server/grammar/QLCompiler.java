package au.edu.usyd.corona.server.grammar;


import java.io.IOException;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.tree.Tree;

import au.edu.usyd.corona.grammar.TokenParseException;
import au.edu.usyd.corona.scheduler.SchedulableTask;
import au.edu.usyd.corona.server.user.User;

/**
 * Singleton class which is used to compile queries in our SQL grammar into
 * executable instances. This class provides the public interfaces to all the
 * compilers in this package that are not publicly accessible.
 * 
 * @author Tim Dawborn
 * @author Raymes Khoury
 */
public class QLCompiler {
	private static final QLCompiler instance = new QLCompiler();
	
	public static QLCompiler getInstance() {
		return instance;
	}
	
	private QLCompiler() {
		// hidden constructor
	}
	
	/**
	 * Compiles the given SQL query into an executable {@link SchedulableTask}
	 * instance
	 * 
	 * @param query the query to compile
	 * @param queryID the ID of the task to be generated
	 * @param user the user executing the query
	 * @return the task to be executed within the system
	 * @throws QLParseException if an error occurs while parsing
	 * @throws QLCompileException if an error occurs while compiling
	 */
	public SchedulableTask compile(String query, int queryID, User user) throws QLParseException, QLCompileException {
		// sanity check
		if (query == null || query.length() == 0)
			throw new QLParseException("Query is null", -1, query);
		
		// checks for a trailing semi-colon
		query = query.trim();
		if (query.charAt(query.length() - 1) != ';')
			query = query + ";";
		
		// creates the lexer for the given SQL
		CoronaQLLexer lexer = new CoronaQLLexer(new CaseInsensitiveANTLRStringStream(query));
		
		// creates the parser using the lexer, and attempts to parse the given sql
		CoronaQLParser parser = new CoronaQLParser(new CommonTokenStream(lexer));
		CoronaQLParser.statement_return ret = null;
		try {
			ret = parser.statement();
			if (ret == null)
				throw new QLParseException("Query was empty", -1, query);
		}
		catch (RecognitionException e) {
			StringBuffer msg = new StringBuffer();
			msg.append(parser.getErrorMessage(e, parser.getTokenNames())).append(":\n");
			for (int i = 0; i <= e.charPositionInLine; i++)
				msg.append(i == e.charPositionInLine ? "v" : "-");
			msg.append('\n');
			msg.append(query.replace('\t', ' ').split("\n")[e.line - 1]).append("\n");
			for (int i = 0; i <= e.charPositionInLine; i++)
				msg.append(i == e.charPositionInLine ? "^" : "-");
			msg.append('\n');
			throw new QLParseException(msg.toString(), e.charPositionInLine, query);
		}
		
		// extracts the AST
		Tree root = (Tree) ret.getTree();
		
		// works out which type of query this is
		QLPacketTypeCompiler<?> compiler = createCompiler(root, queryID, user);
		
		// attempts to compile
		try {
			return compiler.compile();
		}
		catch (IOException e) {
			throw new QLCompileException("IOException occured while attempting to compile the query: " + e.getMessage());
		}
		catch (TokenParseException e) {
			throw new QLCompileException("TokenParseException occured while attempting to compile the query: " + e.getMessage());
		}
	}
	
	private QLPacketTypeCompiler<?> createCompiler(Tree root, int queryId, User user) throws QLCompileException {
		switch (root.getType()) {
		case CoronaQLLexer.TYPE_QUERY:
			return new QueryCompiler(root, queryId);
		case CoronaQLLexer.TYPE_ROUTE:
			return new RouteCompiler(root, queryId);
		case CoronaQLLexer.TYPE_SYNC:
			return new SyncCompiler(root, queryId);
		case CoronaQLLexer.TYPE_KILL:
			return new KillCompiler(root, queryId);
		case CoronaQLLexer.TYPE_SET:
			return new SetPropertyCompiler(root, queryId, user);
		default:
			throw new QLCompileException("Unknown root token type '" + root.getType() + "' with text '" + root.getText() + "'");
		}
	}
}
