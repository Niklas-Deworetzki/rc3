package rc3.januscompiler.types;

/**
 * The abstract superclass of all Janus types.
 *
 * @see PrimitiveType
 * @see ArrayType
 */
public sealed abstract class Type permits ArrayType, PrimitiveType {

}
