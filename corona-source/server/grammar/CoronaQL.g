grammar CoronaQL;

options {
	output=AST;
	ASTLabelType=CommonTree;
}
tokens {
	ALL_ATTRIBS;
	AGGREGATION;
	TYPE_KILL; TYPE_SYNC; TYPE_ROUTE; TYPE_QUERY; TYPE_SET;
}

@lexer::header {
	package au.edu.usyd.corona.server.grammar;
}

@header {
	package au.edu.usyd.corona.server.grammar;
}

@members {
	@Override
	protected void mismatch(IntStream input, int ttype, BitSet follow) throws RecognitionException {
		throw new MismatchedTokenException(ttype, input);
	}
	
	@Override
	public Object recoverFromMismatchedSet(IntStream input, RecognitionException e, BitSet follow) throws RecognitionException {
		throw e;
	}
	
	@Override
	protected Object recoverFromMismatchedToken(IntStream input, int ttype, BitSet follow) throws RecognitionException {
		RecognitionException e = null;
		if (mismatchIsUnwantedToken(input, ttype)) 
			e = new UnwantedTokenException(ttype, input);
		else if (mismatchIsMissingToken(input, follow)) {
			Object inserted = getMissingSymbol(input, e, ttype, follow);
			e = new MissingTokenException(ttype, input, inserted);
		}
		else
			e = new MismatchedTokenException(ttype, input);
		throw e;
	}
	
	public String getErrorMessage(RecognitionException e, String[] tokenNames) {
	   String msg = e.getMessage();
	   Token t = e.token;
	   if ( e instanceof UnwantedTokenException ) {
	      UnwantedTokenException ute = (UnwantedTokenException)e;
	      String tokenName="<unknown>";
	      if ( ute.expecting== Token.EOF ) {
	         tokenName = "'end of query'";
	      }
	      else {
	         tokenName = tokenNames[ute.expecting];
	      }
	      msg = "Unexpected symbol '"+t.getText()+"' found on line "+t.getLine()+" at position "+t.getCharPositionInLine()+"; expected "+tokenName;
	   }
	   else if ( e instanceof MissingTokenException ) {
	      MissingTokenException mte = (MissingTokenException)e;
	      String tokenName="<unknown>";
	      if ( mte.expecting== Token.EOF ) {
	         tokenName = "'end of query'";
	      }
	      else {
	         tokenName = tokenNames[mte.expecting];
	      }
	      msg = "Missing symbol found on line "+t.getLine()+" at position "+t.getCharPositionInLine()+"; expected "+tokenName;
	   }
	   else if ( e instanceof MismatchedTokenException ) {
	      MismatchedTokenException mte = (MismatchedTokenException)e;
	      String tokenName="<unknown>";
	      if ( mte.expecting== Token.EOF ) {
	         tokenName = "'end of query'";
	      }
	      else {
	         tokenName = tokenNames[mte.expecting];
	      }
	      msg = "Unexpected symbol '"+t.getText()+"' found on line "+t.getLine()+" at position "+t.getCharPositionInLine()+"; expected "+tokenName;
	   }
	   else if ( e instanceof NoViableAltException ) {
	      NoViableAltException nvae = (NoViableAltException) e;
	      msg = "Unexpected symbol '"+t.getText()+"' found on line "+t.getLine()+" at position "+t.getCharPositionInLine();
	   }
	   else
	   	msg = super.getErrorMessage(e, tokenNames);
	   return msg;
	} 
	
	public String getTokenErrorDisplay(Token t) { 
		return t.toString(); 
	}
}

@lexer::members { 
	
	@Override
	public void displayRecognitionError(String[] tokenNames, RecognitionException e) {
	}

	
	@Override
	public Token nextToken() {
		boolean broken = false;
		int brokenStart = 0;
		int brokenLine = 0;
		int brokenStartLine = 0;
		
       while (true) {
          state.token = null;
          state.channel = Token.DEFAULT_CHANNEL;
          state.tokenStartCharIndex = input.index();
          state.tokenStartCharPositionInLine = input.getCharPositionInLine();
          state.tokenStartLine = input.getLine();
          state.text = null;
          if (broken) {
               Token t = new CommonToken(input, Token.INVALID_TOKEN_TYPE, state.channel, brokenStart, getCharIndex()-1);
					t.setLine(brokenLine);
					t.setCharPositionInLine(brokenStartLine);
					return t;
             } 
          if ( input.LA(1)==CharStream.EOF ) { 
             return Token.EOF_TOKEN;
          }  
          
          try {
             mTokens();
             if ( state.token==null ) {
                emit();
             }   
             else if ( state.token==Token.SKIP_TOKEN ) { 
                continue;
             }
             
             return state.token;
          }   
          catch (NoViableAltException nva) {
             reportError(nva);
             broken = true;
             brokenStart = state.tokenStartCharIndex;
             brokenLine = state.tokenStartLine;
             brokenStartLine = state.tokenStartCharPositionInLine;
             recover(nva); // throw out current char and try again
          }   
          catch (RecognitionException re) {
             reportError(re);
             // match() routine has already called recover()
             broken = true;
             brokenStart = state.tokenStartCharIndex;
             brokenLine = state.tokenStartLine;
             brokenStartLine = state.tokenStartCharPositionInLine;
          }   
       }   
    } 
	
	
}

@rulecatch {
	catch (RecognitionException e) {
		throw e;
	}
}




ASTRIX : '*';

COMPARATOR : '<=' | '<' | '!=' | '==' | '>=' | '>' ;
BINARYFUNC2: '/' | '*' ;
BINARYFUNC1: '+' | '-' ;
HOUR : 'HOURS' | 'HOUR' | 'H' ;
MINUTE : 'MINUTES' | 'MINUTE' | 'M' ;
SECOND : 'SECONDS' | 'SECOND' | 'S' ;

KILL   : 'KILL' ;
SYNC   : 'SYNC' ;
ROUTE  : 'ROUTE' ;
SELECT : 'SELECT' ;
SET    : 'SET' ;

FROM    : 'FROM' ;
WHERE   : 'WHERE' ;
GROUPBY : 'GROUP BY' ;
HAVING  : 'HAVING' ;
START   : 'START' ;
EPOCH   : 'EPOCH' ;
RUNCOUNT: 'RUNCOUNT' ;

BOOL_AND: 'AND' ;
BOOL_OR : 'OR' ;
BOOL_NOT: 'NOT' ;
AT      : 'AT' ;
IN      : 'IN' ;
ON_DATE : 'ON DATE' ;
FOREVER : 'FOREVER' ;
SENSORS : 'SENSORS' ;

NUMBER_FLOAT : '-'? ('0'..'9')+ '.' ('0'..'9')+ ;
NUMBER_INT   : '-'? ('0'..'9')+ ;
STRING       : '"' (~('\n'|'\r'|'\t'|' '|'/'|'\\'|'"'|'\''))+ '"' ;
fragment HEX : '0'..'9' | 'A'..'F' ;
ADDRESS      :  HEX HEX HEX HEX '.' HEX HEX HEX HEX '.' HEX HEX HEX HEX '.' HEX HEX HEX HEX ;
WHITE_SPACE  : ('\n'|'\r'|'\t'|' ')+ ;
WORD         : ('a'..'z'|'A'..'Z'|'0'..'9'|'_')+ ;

statement 
	: WHITE_SPACE? statement2 ';' EOF -> statement2
	;
statement2 
	: kill
	| sync
	| route
	| set 
	| data_query ;

kill  
	: KILL WHITE_SPACE NUMBER_INT WHITE_SPACE? -> ^(TYPE_KILL NUMBER_INT)
	;
sync  
	: SYNC WHITE_SPACE? -> ^(TYPE_SYNC)
	;
route 
	: ROUTE WHITE_SPACE? -> ^(TYPE_ROUTE)
	;
set 
	: SET WHITE_SPACE WORD WHITE_SPACE? '=' WHITE_SPACE? NUMBER_INT -> ^(TYPE_SET WORD NUMBER_INT)
	;


/* =====================
 *  QUERY related rules
 * =====================*/
data_query 
	: qselect (WHITE_SPACE qfrom)? (WHITE_SPACE qwhere)? (WHITE_SPACE qgroupby)? (WHITE_SPACE qhaving)? (WHITE_SPACE qstart)? (WHITE_SPACE qepoch)? (WHITE_SPACE qruncount)? WHITE_SPACE?
		-> ^(TYPE_QUERY qselect qfrom? qwhere? qgroupby? qhaving? qstart? qepoch? qruncount?)
	;
qselect 
	: SELECT^ WHITE_SPACE! fields 
	;
qfrom
	: FROM^ WHITE_SPACE! SENSORS
	;
qwhere 
	: WHERE^ WHITE_SPACE! orexpression
	;
qgroupby
	: GROUPBY^ WHITE_SPACE! gbfield
	;
qhaving
	: HAVING^ WHITE_SPACE! orexpression
	;
qstart
	: START^ WHITE_SPACE! starttime
	;
qepoch 
	: EPOCH^ WHITE_SPACE! verbosetime
	;
qruncount
	: RUNCOUNT^ WHITE_SPACE! (NUMBER_INT | FOREVER)
	;



/* =====================
 *  field related rules
 * =====================*/
fields 
	: ASTRIX -> ALL_ATTRIBS 
	| fields2 
	;
fields2 
	: fields3 (WHITE_SPACE? ',' WHITE_SPACE? fields3)* -> fields3+ 
	;
fields3 
	: WORD
	| aggregation
	;
aggregation 
	: WORD '(' WORD ')' -> ^(AGGREGATION WORD WORD)
	| WORD '(' ASTRIX ')' -> ^(AGGREGATION WORD ASTRIX)
	;
gbfield
	: WORD (WHITE_SPACE? ',' WHITE_SPACE? WORD)* -> WORD+
	;



/* =====================
 *  comparison related rules 
 * =====================*/
orexpression
	: andexpression (WHITE_SPACE! BOOL_OR^ WHITE_SPACE! orexpression)?
	;
andexpression 
	: notexpression (WHITE_SPACE! BOOL_AND^ WHITE_SPACE! andexpression)? 
	;
notexpression 
	: (BOOL_NOT^ WHITE_SPACE!)? comparison
	;
comparison 
	: numericexpr1 (WHITE_SPACE! COMPARATOR^ WHITE_SPACE! numericexpr1)?
	;
numericexpr1 
	: numericexpr2 (WHITE_SPACE! BINARYFUNC1^ WHITE_SPACE! numericexpr2)? 
	;
numericexpr2 
	: numeric (WHITE_SPACE! (BINARYFUNC2|ASTRIX)^ WHITE_SPACE! numericexpr2)? 
	;
numeric  
	: WORD 
	| NUMBER_INT
	| NUMBER_FLOAT
	| ADDRESS
	| aggregation  
	| '(' WHITE_SPACE? orexpression WHITE_SPACE? ')' -> orexpression
	;



/* =====================
 *  Time related rules
 * =====================*/
starttime 
	: fixedtime 
	| relativetime 
	;
fixedtime 
	: AT^ WHITE_SPACE! statictime 
	;
relativetime 
	: IN^ WHITE_SPACE! verbosetime 
	;
verbosetime 
	: vthours 
	| vtminutes 
	| vtseconds 
	;
vthours 
	: NUMBER_INT WHITE_SPACE! HOUR (WHITE_SPACE! (vtminutes | vtseconds))? 
	;
vtminutes 
	: NUMBER_INT WHITE_SPACE! MINUTE (WHITE_SPACE! vtseconds)? 
	;
vtseconds 
	: NUMBER_INT WHITE_SPACE! SECOND 
	;
statictime 
	: NUMBER_INT ':'! NUMBER_INT ':'! NUMBER_INT date?
	;
date 
	: ON_DATE WHITE_SPACE NUMBER_INT '.' NUMBER_INT '.' NUMBER_INT -> ^(ON_DATE NUMBER_INT NUMBER_INT NUMBER_INT)
	; 


