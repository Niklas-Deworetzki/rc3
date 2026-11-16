package rc3.rssa.parse;

import java_cup.runtime.*;

import rc3.lib.messages.ErrorMessage;
import rc3.januscompiler.table.Identifier;

import rc3.lib.messages.ErrorMessage;
import rc3.lib.parsing.Position;
import rc3.lib.parsing.Source;

import rc3.rssa.parse.symbol.*;
import static rc3.rssa.parse.Sym.*;

%%

%class Scanner
%public

%cup
%unicode

%line
%column

%eofclose false
%eofval{
    // This needs to be specified when using a custom sym class name
    return new RSSASymbol(currentPosition(), EOF);
%eofval}

%ctorarg Source source
%init{
    this.source = source;
%init}
%{
    StringBuilder string = new StringBuilder();

    public final Source source;

    private Position currentPosition() {
        // Return the current position, counting lines and columns beginning from 1.
        return new Position(source, yyline + 1, yycolumn + 1);
    }

    private Symbol symbol(int type) {
        return new RSSASymbol(currentPosition(), type);
    }

    private Symbol symbol(int type, Object value) {
        return new RSSASymbol(currentPosition(), type, value);
    }
%}

LineTerminator = \r|\n|\r\n
OctDigit          = [0-7]
StringCharacter = [^\r\n\"\\]

%xstate STRING

%%

\/\/.*                  { /* Ignore line comments */ }
\/\*([^*]|\*[^/])*\*\/  { /* Ignore multi-line comments */ }

\s+                     { /* Ignore whitespace */ }

\(                      { return symbol(LPAR); }
\)                      { return symbol(RPAR); }
\[                      { return symbol(LBRA); }
\]                      { return symbol(RBRA); }
,                       { return symbol(COMMA); }

\<->                    { return symbol(SWAP); }
\<-                     { return symbol(ENTRY); }
\->                     { return symbol(EXIT); }

\+                      { return symbol(ADD); }
\-                      { return symbol(SUB); }
\*                      { return symbol(MUL); }
\/                      { return symbol(DIV); }
%                       { return symbol(MOD); }

&                       { return symbol(AND); }
\|                      { return symbol(OR); }
\^                      { return symbol(XOR); }
\<<                     { return symbol(LSHIFT); }
\>>                     { return symbol(RSHIFT); }

\<                      { return symbol(LST); }
\<=                     { return symbol(LSE); }
>                       { return symbol(GRT); }
>=                      { return symbol(GRE); }
=                       { return symbol(EQU); } /* maybe == ? */
\!=                     { return symbol(NEQ); }
\:=                     { return symbol(ASGN); }

call                    { return symbol(CALL); }
uncall                  { return symbol(UNCALL); }
begin                   { return symbol(BEGIN); }
end                     { return symbol(END); }
M                       { return symbol(MEM); }

@                       { return symbol(AT); }
/* string literal */
\"                      { yybegin(STRING); string.setLength(0); }


[a-zA-Z_.][a-zA-Z_0-9_.]* { return symbol(IDENT, new Identifier(yytext(), currentPosition())); }

0|-?[1-9][0-9]*  { try {
                       return symbol(INTLIT, Integer.parseInt(yytext()));
                   } catch (NumberFormatException invalidNumber) {
                       throw new ErrorMessage.InvalidLiteral("number", yytext(), currentPosition());
                   }
                 }

'.'                     { return symbol(INTLIT, (int) yytext().charAt(1)); }
'\\n'                   { return symbol(INTLIT, (int) '\n'); }
'\\r'                   { return symbol(INTLIT, (int) '\r'); }
'\\t'                   { return symbol(INTLIT, (int) '\t'); }

<STRING> {
  \"                             { yybegin(YYINITIAL); return symbol(STRING_LITERAL, string.toString()); }

  {StringCharacter}+             { string.append( yytext() ); }

  /* escape sequences */
  "\\b"                          { string.append( '\b' ); }
  "\\t"                          { string.append( '\t' ); }
  "\\n"                          { string.append( '\n' ); }
  "\\f"                          { string.append( '\f' ); }
  "\\r"                          { string.append( '\r' ); }
  "\\\""                         { string.append( '\"' ); }
  "\\'"                          { string.append( '\'' ); }
  "\\\\"                         { string.append( '\\' ); }
  \\[0-3]?{OctDigit}?{OctDigit}  { char val = (char) Integer.parseInt(yytext().substring(1),8); string.append( val ); }

  /* error cases */
  \\.                            { throw new ErrorMessage.IllegalString("Illegal escape sequence \""+yytext()+"\"", currentPosition()); }
  {LineTerminator}               { throw new ErrorMessage.IllegalString("Unterminated string at end of line", currentPosition()); }
}

[^]     { // This rule matches any previously unmatched characters.
          char offendingChar = yytext().charAt(0);
          throw new ErrorMessage.IllegalCharacter(offendingChar, currentPosition());
        }
