package rc3.rssa.parse.symbol;

import rc3.lib.parsing.Position;
import rc3.lib.parsing.RichSymbol;
import rc3.rssa.parse.Sym;

/**
 * A custom Symbol class used by Scanner and Parser.
 * This class holds a {@link Position} value, representing the original position in the source code.
 * Additionally this class uses the generated {@link Sym} class, to lookup names for terminal symbols.
 */
public final class RSSASymbol extends RichSymbol {
    public RSSASymbol(Position position, int terminalId, Object value) {
        super(position, terminalId, value);
    }

    public RSSASymbol(Position position, int terminalId) {
        super(position, terminalId);
    }

    public RSSASymbol(Position position, int nonterminalId, String nonterminalName, int left, int right, Object value) {
        super(position, nonterminalId, nonterminalName, left, right, value);
    }

    public RSSASymbol(Position position, int nonterminalId, String nonterminalName, int left, int right) {
        super(position, nonterminalId, nonterminalName, left, right);
    }

    @Override
    public String[] terminalNames() {
        return Sym.terminalNames;
    }
}
