package rc3.lib.parsing;

import java_cup.runtime.Symbol;
import rc3.januscompiler.parse.Sym;

/**
 * A custom {@link Symbol} class used by Scanner and Parser.
 * <p>
 * This class extends the default {@link Symbol} class by also providing
 * a {@link Position} value, representing the original position in the source
 * code, where this {@link Symbol} originates from.
 * <p>
 * Additionally this class uses {@link RichSymbol#terminalNames()} to
 * find the names of terminal {@link Symbol Symbols}. In contrast to
 * non-terminals, where this name is provided by the Parser, a terminal's
 * name must be found on another way. This is achieved by implementing the
 * {@link RichSymbol#terminalNames()} method.
 * <p>
 * An implementing class must override all constructors with delegates to
 * this class's constructors. To obtain the names of terminal symbols via
 * {@link RichSymbol#terminalNames()}, an implementation can use the generated
 * <code>Sym</code> class, if the Scanner is generated using <i>JFlex</i>.
 */
public abstract class RichSymbol extends Symbol {
    public final Position position;

    /**
     * Is this a terminal symbol? This decision is made depending on the invoked constructor.
     */
    private final boolean isTerminal;
    private final String name;

    public RichSymbol(Position position, int terminalId, Object value) {
        super(terminalId, position.line, position.column, value);
        this.position = position;
        this.isTerminal = true;
        this.name = terminalNames()[terminalId];
    }

    public RichSymbol(Position position, int terminalId) {
        this(position, terminalId, null);
    }

    public RichSymbol(Position position, int nonterminalId, String nonterminalName, int left, int right, Object value) {
        super(nonterminalId, left, right, value);
        this.position = position;
        this.isTerminal = false;
        this.name = nonterminalName;
    }

    public RichSymbol(Position position, int nonterminalId, String nonterminalName, int left, int right) {
        this(position, nonterminalId, nonterminalName, left, right, null);
    }

    /**
     * Implementations of this method should return an array of {@link String} values,
     * representing the names of different terminals. These values are indexed by
     * the terminals unique id.
     *
     * @implNote If the Scanner producing terminal symbols is generated using
     * <i>JFlex</i>, it is valid to return <code>Sym.terminalNames</code> from
     * the generated <code>Sym</code> class.
     */
    public abstract String[] terminalNames();

    /**
     * The name of this Symbol. It is determined depending on whether this symbol represents a
     * terminal symbol or a nonterminal symbol.
     * <p>
     * For terminal symbols the symbol's name is obtained by looking it up in {@link Sym#terminalNames} with the
     * symbol's terminal id. For nonterminal symbols the name must be provided when invoking the constructor.
     * </p>
     *
     * @return This symbol's name.
     */
    public String getName() {
        return name;
    }

    @Override
    public String toString() {
        if (isTerminal && value != null) {
            return String.format("%s (%s)", name, value);
        } else {
            return name;
        }
    }
}
