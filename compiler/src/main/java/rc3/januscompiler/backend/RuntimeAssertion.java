package rc3.januscompiler.backend;

/**
 * This {@link Enum} lists different runtime conditions that are asserted during the execution of a
 * Janus program. If one of the assertions fail, execution is halted and the program aborted.
 * <p>
 * Some of the {@link RuntimeAssertion RuntimeAssertions} are known from non-reversible languages like
 * and ensure logical or arithmetic consistency. Other assertions however ensure that no information is destroyed
 * at the runtime in cases that can not be checked during compile time.
 * </p>
 */
public enum RuntimeAssertion {
    /**
     * Ensures that a variable declared in a <code>local</code>-block has the asserted value when exiting the block.
     * By ensuring that the value of a variable is known by entering and exiting a <code>local</code>-block, no
     * information is added or removed from the runtime.
     * <p>
     * If a variable could exit a <code>local</code>-block with any arbitrary value it would be possible to just
     * "dump" information into this variable and removing it from the program on exit.
     * </p>
     * <p>
     * An example of a program, where this assertion is violated:
     * <pre>
     *  local int var = 0
     *      var += 10
     *  delocal int var = 0
     * </pre>
     * </p>
     */
    INITIALIZER_MISMATCH,
    /**
     * Ensures that the correct branch of an if-statement can be chosen when reversing the execution, by checking
     * that the expression at the end of an if-statement evaluates to the correct boolean value.
     * <p>
     * If the boolean value at the end of an if-statement would not evaluate to the correct value (<code>true</code>
     * when coming from the <code>then</code> branch and <code>false</code> when coming from the <code>then</code>
     * branch) it would be possible to destroy information. When reversing the if-statement there would be no way
     * to determine which is the correct branch to chose, since both could be equally likely, when no value for
     * the exiting expression is required.
     * </p>
     * <p>
     * An example of a program, where this assertion is violated:
     * <pre>
     *  if empty(stk) then
     *      push(n, stk)
     *  fi empty(stk)
     * </pre>
     * </p>
     */
    IF_CONDITION_MISMATCH,
    /**
     * Ensures that the entry condition of a reversible loop evaluates to <code>true</code> when first entering the loop.
     * <p>
     * If the entry variable would not evaluate to <code>true</code> it would not be possible to correctly reverse
     * the loop, since no exit condition would be known.
     * </p>
     * <p>
     * An example of a program, where this assertion is violated:
     * <pre>
     *  local int n = 5
     *      from n = 0 do
     *          n -= 1
     *      until n = 0
     *      ...
     * </pre>
     * </p>
     */
    ENTRY_CONDITION_MUST_BE_TRUE,
    /**
     * Ensures that the entry condition of a reversible loop always evaluates to <code>false</code> upon
     * repeated evaluation.
     * <p>
     * If the entry condition would evaluate to <code>true</code> upon repeated evaluation it would not be
     * possible to determine, after how many repetitions a backwards executed loop would be un-done.
     * Allowing the entry condition of a forward executed loop to be <code>true</code> multiple times would
     * result in the exit condition of the reversed loop to evaluate to <code>true</code> prematurely.
     * </p>
     * <p>
     * An example of a program, where this assertion is violated:
     * <pre>
     *  from n != 0 do
     *      n -= 1
     *  until n = 0
     * </pre>
     * </p>
     */
    ENTRY_CONDITION_MUST_BE_FALSE,
    /**
     * Ensures that the variable used to store the value popped from a stack is zero when performing the
     * <code>pop</code> operation.
     * <p>
     * If the value of a variable would be non-zero before popping the top of a stack into it, it would be
     * possible to destroy the information contained in the variable, since it would be overwritten by
     * the value popped from the stack.
     * </p>
     * <p>
     * An example of a program, where this assertion is violated:
     * <pre>
     *  call calculationThatStoresResultIn(var)
     *  pop(var, stk)
     * </pre>
     * </p>
     */
    POP_NONZERO,
    /**
     * Ensures that the stack is non-empty when popping the top element from it.
     * <p>
     * This assertion is present in non-reversible languages as well, since it ensures that when performing a
     * <code>pop</code> operation on a stack, there actually is an element to retrieve from the stack.
     * </p>
     * <p>
     * An example program, where this assertion is violated:
     * <pre>
     *  if empty(stk) then
     *      pop(n, stk)
     *  ...
     * </pre>
     * </p>
     */
    POP_EMPTY_STACK,
    /**
     * Ensures that the stack is non-empty when fetching the top element from it.
     * <p>
     * This assertion is present in non-reversible languages as well, since it ensures that when fetching an
     * element from the top of a stack, there actually is an element on the stack that can be fetched.
     * </p>
     * <p>
     * An example program, where this assertion is violated:
     * <pre>
     *  if empty(stk) then
     *    n -= top(stk)
     *  ...
     * </pre>
     * </p>
     */
    TOP_EMPTY_STACK,
    /**
     * Ensures that an array access is within the arrays bounds.
     * <p>
     * This assertion is present in non-reversible languages as well, since it ensures that an array access
     * is always within the array bounds.
     * </p>
     * <p>
     * An example program, where this assertion is violated:
     * <pre>
     *  int array[20]
     *  push(array[45], stk)
     * </pre>
     * </p>
     */
    ARRAY_INDEX,
    /**
     * Ensures that no division by zero is performed.
     * <p>
     * This assertion is present in non-reversible languages as well. Since there is no result for a division by
     * zero it is impossible to calculate the result of an expression containing this division. Since by a division
     * using variables one variable could become zero at runtime it is not always possible to detect an illegal
     * division at compile time.
     * </p>
     * <p>
     * An example program, where this assertion is violated:
     * <pre>
     *  from n = 10 do
     *      push(100 / n, stk)
     *      n -= 1
     *  until n = 0
     * </pre>
     * </p>
     */
    DIVISION_BY_ZERO
}
