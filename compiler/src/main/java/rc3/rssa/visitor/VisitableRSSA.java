package rc3.rssa.visitor;

public interface VisitableRSSA {
    <R> R accept(RSSAVisitor<R> visitor);
   // void accept(VariableVisitor visitor);

}
