package rs.ac.bg.etf.pp1;

import java_cup.runtime.Symbol;

%%

%{

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type) {
		return new Symbol(type, yyline+1, yycolumn + 1);
	}

	// ukljucivanje informacije o poziciji tokena
	private Symbol new_symbol(int type, Object value) {
		return new Symbol(type, yyline+1, yycolumn + 1, value);
	}

%}

%cup
%line
%column

%xstate COMMENT

%eofval{
	return new_symbol(sym.EOF);
%eofval}

%%

" " 	{ }
"\b" 	{ }
"\t" 	{ }
"\r\n" 	{ }
"\f" 	{ }

"program"   { return new_symbol(sym.PROG, yytext());}
"void" 		{ return new_symbol(sym.VOID, yytext()); }
"print" 	{ return new_symbol(sym.PRINT, yytext()); }
"read" 		{ return new_symbol(sym.READ, yytext()); }
"return" 	{ return new_symbol(sym.RETURN, yytext()); }
"new" 		{ return new_symbol(sym.NEW, yytext()); }
"const"     { return new_symbol(sym.CONST, yytext()); }
"true"      { return new_symbol(sym.BOOLEANCONST, Boolean.valueOf(yytext())); }
"false"     { return new_symbol(sym.BOOLEANCONST, Boolean.valueOf(yytext())); }
"findAny"	{ return new_symbol(sym.FINDANY, yytext()); }
"+" 		{ return new_symbol(sym.PLUS, yytext()); }
"++" 		{ return new_symbol(sym.PLUSPLUS, yytext()); }
"-" 		{ return new_symbol(sym.MINUS, yytext()); }
"--" 		{ return new_symbol(sym.MINUSMINUS, yytext()); }
"=" 		{ return new_symbol(sym.EQUAL, yytext()); }
"*" 		{ return new_symbol(sym.MUL, yytext()); }
"/" 		{ return new_symbol(sym.DIV, yytext()); }
"%" 		{ return new_symbol(sym.MOD, yytext()); }
"." 		{ return new_symbol(sym.DOT, yytext()); }
";" 		{ return new_symbol(sym.SEMI, yytext()); }
"," 		{ return new_symbol(sym.COMMA, yytext()); }
"(" 		{ return new_symbol(sym.LPAREN, yytext()); }
")" 		{ return new_symbol(sym.RPAREN, yytext()); }
"{" 		{ return new_symbol(sym.LBRACE, yytext()); }
"}"			{ return new_symbol(sym.RBRACE, yytext()); }
"["			{ return new_symbol(sym.LSQUARE, yytext()); }
"]"			{ return new_symbol(sym.RSQUARE, yytext()); }

"//" 				{yybegin(COMMENT);}
<COMMENT> . 		{yybegin(COMMENT);}
<COMMENT> "\r\n" 	{ yybegin(YYINITIAL); }

[0-9]+  						{ return new_symbol(sym.NUMBER, Integer.parseInt(yytext())); }
([a-z]|[A-Z])[a-z|A-Z|0-9|_]* 	{ return new_symbol(sym.IDENT, yytext()); }
\'([0-9a-zA-Z])\'   			{ return new_symbol(sym.CHARCONST, yytext().charAt(1)); }

. { System.err.println("Leksicka greska ("+yytext()+") u liniji "+(yyline+1)); }