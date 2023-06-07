package au.edu.usyd.corona.server.grammar;


import junit.framework.TestCase;

import org.antlr.runtime.CommonTokenStream;
import org.antlr.runtime.RecognitionException;
import org.antlr.runtime.UnwantedTokenException;

import au.edu.usyd.corona.middleLayer.Network;

public class CoronaQLParserTest extends TestCase {
	
	@Override
	public void setUp() {
		Network.initialize(Network.MODE_UNITTEST);
	}
	
	private void run(String query, boolean shouldPass) throws Exception {
		query = query + ";";
		
		CoronaQLLexer lexer = new CoronaQLLexer(new CaseInsensitiveANTLRStringStream(query));
		CoronaQLParser parser = new CoronaQLParser(new CommonTokenStream(lexer));
		
		boolean passed = true;
		try {
			CoronaQLParser.statement_return ret = parser.statement();
			if (ret == null)
				passed = false;
			else if (lexer.getNumberOfSyntaxErrors() > 0)
				throw new UnwantedTokenException(CoronaQLLexer.EOF, parser.getTokenStream());
		}
		catch (RecognitionException e) {
			passed = false;
		}
		
		assertEquals(shouldPass, passed);
	}
	
	public void testSync() throws Exception {
		run("SYNC  ", true);
		run("SYNC me", false);
		run("sync", true);
	}
	
	public void testKill() throws Exception {
		run("KILL 1", true);
		run("KILL", false);
		run("kill 1", true);
		run("KILL " + Long.MAX_VALUE, true);
	}
	
	public void testRoute() throws Exception {
		run("ROUTE", true);
		run("route", true);
		run("ROUTE 1", false);
		run("ROUTE -1", false);
		run("ROUTE 0.34", false);
	}
	
	public void testSelect() throws Exception {
		run("SELECT *", true);
		run("SELECT node, parent", true);
		run("SELECT ", false);
		run("SELECT cpu2", true);
		run("select node", true);
		run("SELECT parent battery", false);
		run("SELECT parent\t,\nbattery", true);
		run("SELECT node, parent,light,temp, sw1, sw2, x,y,z, battery, cpu, memory", true);
		run("SELECT node, screwed, attribute", true);
	}
	
	public void testSelectAggregates() throws Exception {
		run("SELECT SUM(node)", true);
		run("SELECT SUM(*)", true);
		run("SELECT COUNT(*)", true);
		run("SELECT AVG()", false);
		run("SELECT light, MAX(temp), temp", true);
	}
	
	public void testRuncount() throws Exception {
		run("SELECT * RUNCOUNT", false);
		run("SELECT * RUNCOUNT 10", true);
		run("SELECT * RUNCOUNT FOREVER", true);
		run("SELECT * RUNCOUNT 123.45", false);
		
	}
}
