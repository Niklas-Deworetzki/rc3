package rc3.januscompiler.table;

import rc3.lib.parsing.Position;

/**
 * This class represents the names used to identify variables, procedures, etc.
 * <p>
 * The {@link Identifier} class contains a {@link String} that holds the characters used
 * to identify a Janus construct. This {@link String} is interned ({@link String#intern()})
 * when creating a new {@link Identifier} instance.
 * </p>
 * <p>
 * Additionally an {@link Identifier} holds the {@link Position} of where it is in the source code.
 * </p>
 */
public final class Identifier implements Comparable<Identifier> {
    /**
     * The name of this {@link Identifier}.
     */
    public final String name;
    /**
     * The {@link Position} in the source code of this {@link Identifier}.
     */
    public final Position position;

    public Identifier(String name, Position position) {
        this.name = name.intern();
        this.position = position;
    }

    public Identifier(String name) {
        this(name, null);
    }

    @Override
    public int compareTo(Identifier o) {
        return this.name.compareTo(o.name);
    }

    @Override
    public int hashCode() {
        return name.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        return obj instanceof Identifier &&
                ((Identifier) obj).name.equals(name);
    }

    @Override
    public String toString() {
        return name;
    }
}
