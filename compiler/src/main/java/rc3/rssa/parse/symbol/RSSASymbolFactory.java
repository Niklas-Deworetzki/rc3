package rc3.rssa.parse.symbol;

import java_cup.runtime.Symbol;
import java_cup.runtime.SymbolFactory;
import rc3.lib.parsing.Position;

/**
 * A simple {@link SymbolFactory} implementation. This has to be specified,
 * since {@link java_cup.runtime.DefaultSymbolFactory} is deprecated.
 *
 * <p>This implementation is mainly inspired by the {@link java_cup.runtime.DefaultSymbolFactory}. The only addition
 * is, that Position information is preserved and automatically inferred by creating new Symbols.</p>
 */
public class RSSASymbolFactory implements SymbolFactory {

    private static Position positionOf(Symbol... symbols) {
        for (Symbol symbol : symbols) {
            if (symbol instanceof RSSASymbol) {
                return ((RSSASymbol) symbol).position;
            }
        }
        return Position.NONE;
    }

    @Override
    public Symbol startSymbol(String name, int id, int state) {
        final RSSASymbol startSymbol = new RSSASymbol(Position.NONE, id, name, -1, -1);
        startSymbol.parse_state = state;
        return startSymbol;
    }

    @Override
    public Symbol newSymbol(String name, int id, Symbol left, Symbol right, Object value) {
        int lleft = (left != null) ? left.left : -1;
        int rright = (right != null) ? right.right : -1;

        return new RSSASymbol(positionOf(left, right), id, name, lleft, rright, value);
    }

    @Override
    public Symbol newSymbol(String name, int id, Symbol left, Object value) {
        return newSymbol(name, id, left, left, value);
    }

    @Override
    public Symbol newSymbol(String name, int id, Symbol left, Symbol right) {
        return newSymbol(name, id, left, right, null);
    }

    @Override
    public Symbol newSymbol(String name, int id) {
        return newSymbol(name, id, null);
    }

    @Override
    public Symbol newSymbol(String name, int id, Object value) {
        return new RSSASymbol(Position.NONE, id, name, -1, -1, value);
    }
}
