package rc3.januscompiler.syntax.visitor;

/**
 * This interface implements the visitor pattern and is to be used with {@link TreeVisitor} instances.
 */
public interface VisitableTree {

    <R> R accept(TreeVisitor<R> visitor);

}
