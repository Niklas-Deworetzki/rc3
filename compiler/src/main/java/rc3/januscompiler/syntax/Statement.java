package rc3.januscompiler.syntax;

import rc3.lib.parsing.Position;

/**
 * This abstract class is the superclass of all statements.
 */
public abstract sealed class Statement extends Tree
        permits AssignStatement, BuiltinStatement, CallStatement, IfStatement,
        LocalBlockStatement, LoopStatement, SkipStatement, SwapStatement {
    protected Statement(Position position) {
        super(position);
    }
}
