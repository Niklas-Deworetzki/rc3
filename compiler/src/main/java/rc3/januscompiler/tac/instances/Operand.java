package rc3.januscompiler.tac.instances;

import rc3.januscompiler.tac.visitor.VisitableTac;

/**
 * This abstract class is the root of all operands used in three-address-code instructions.
 * <p>
 * It implements the {@link VisitableTac} interface and thus can be used in the visitor pattern.
 * </p>
 */
public abstract class Operand implements VisitableTac {

}
