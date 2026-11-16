package rc3.januscompiler.tac.visitor;

/**
 * This interface implements the visitor pattern and is to be used with {@link TacVisitor} instances.
 */
public interface VisitableTac {
    <R> R accept(TacVisitor<R> visitor);
}
