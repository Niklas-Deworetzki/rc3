package rc3.rssa.cse;

import rc3.lib.utils.CollectionUtils;
import rc3.lib.utils.GraphViz;
import rc3.lib.utils.RestrictedOperations;
import rc3.lib.utils.SetOperations;
import rc3.rssa.annotations.Annotation;
import rc3.rssa.annotations.views.InBoundsView;
import rc3.rssa.blocks.BasicBlock;
import rc3.rssa.instances.*;
import rc3.rssa.visitor.DoNothingVoidRSSAVisitor;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.Objects.requireNonNull;

/**
 * This class implements Common Subexpression Elimination (CSE) using a Directed Acyclic Graph (DAG).
 * The goal of CSE is to transform expression trees into a DAG by re-using common subtrees in the expression tree.
 * <p>
 * The graph is constructed from the {@link BasicBlock} passed to the constructor during instance creation.
 * <p>
 * This class extends the technique described in "Compilerbau, Tl. 2" by Alfred V. Aho, Ravi Sethi and
 * Jeffrey D. Ullman (1988), p.668ff. by additional data structures and tricks to manage the reversible code.
 * <p>
 * This class manages a {@link CSEGraph#nodes set of nodes} that represents all nodes contained in the graph.
 * Since in the sense of CSE the memory changing nodes contain side effects, the set of all nodes may contain
 * multiple structurally equal nodes. These nodes either produce a side effect and thus have to be included multiple
 * times. Or they depend on memory that might be changed via a side effect and thus has to be evaluated multiple times.
 * <p>
 * An additional {@link CSEGraph#liveNodes set of live nodes} is managed, holding all nodes that are available
 * for elimination. This set should only contain structurally distinct nodes, since nodes (or expression trees) can
 * be eliminated based on structural equality.
 * <p>
 * To account for memory changes, a {@link CSEGraph#readingMemoryAccesses set of reading memory accesses} is managed
 * with the live nodes. Whenever a memory changing node is introduced, the method {@link CSEGraph#addMemoryBarrier(DagNode)}
 * is called. This method has the following three tasks:
 * <ul>
 *  <li>It adds the reading memory accesses as sequential children of the memory changing node, since the reads have to
 *  occur before the change is made.</li>
 *  <li>It removes all nodes from the set of live nodes, that depend on any of the reading memory accesses, since those
 *  values are not valid anymore after the memory has been changed.</li>
 *  <li>It clears the set of memory reading nodes to provide a fresh state for further memory operations.</li>
 * </ul>
 * <p>
 * Nodes are added to this graph via the {@link CSEGraph#findValueNodeOrInsert(RValue)} or respectively the
 * {@link CSEGraph#findArithmeticNodeOrInsert(Atom, ArithmeticDagNode.Operator, RValue, BinaryOperand.BinaryOperator, RValue)}
 * method. These methods call the internal {@link CSEGraph#findNode(DagNode)} method to search for structurally
 * equal nodes and return those, if present. If no structurally equal nodes are found, a newly created node is
 * inserted and returned by the method for further processing. This new node is then available as a live node
 * until removed by a memory barrier.
 * <p>
 * This graph also has to handle destruction and creation of variables, as the RSSA language allows it. For this
 * purpose {@link CSEGraph#creatingNodes} and {@link CSEGraph#destroyingNodes} is managed, which adds an entry
 * for every node that either creates or destroys a variable. These maps are than used to insert sequential children
 * after all nodes have been added to the graph. The method {@link CSEGraph#insertAllUseBeforeDestroyDependencies()}
 * adds all parents of a node as sequential children to the destroying parent of a node. This way, all occurrences
 * of a variable are resolved, before it is destroyed.
 * <p>
 * For {@link ProcedureDagNode} as well as {@link EntryExitDagNode} instances the original CSE scheme does not
 * work, since one node usually creates a single value. These cases, however, would create multiple values that
 * are introduced by entry or call instructions. In these cases, the introduced <i>variables</i> are inserted
 * as a fresh node into the graph with a sequential dependency to the creating node. This way the call or entry
 * has to be evaluated before its results are accessible. Constants do not need to have this dependency, since they
 * are always available.
 * <p>
 * An additional set of sequential dependencies is inserted into the graph to ensure all instructions are generated
 * in the correct order from it. Two special nodes {@link CSEGraph#entry} and {@link CSEGraph#exit} represent the
 * entry and exit point of a {@link BasicBlock}. The first of these two has to be generated as the very first
 * instruction while the second has to be generated as the very last instruction. To ensure this behavior, a sequential
 * dependency from every {@link DagNode#isLeaf() leaf node} is added to the entry point, so no values can be used
 * before it has been created. Consequently a sequential dependency from the exit points is added to every
 * {@link DagNode#isRoot() root node}, so all values have to be generated before the exit point.
 */
public class CSEGraph extends DoNothingVoidRSSAVisitor implements GraphViz, RestrictedOperations {
    // Public members, the underlying BasicBlock and the Set holding all nodes.
    private final Map<String, Boolean> memoryManipulatingCalls;
    public final BasicBlock basicBlock;
    public final Set<DagNode> nodes = new HashSet<>();

    // Management of creation/finalizer dependencies.
    private final Map<Variable, DagNode> creatingNodes = new HashMap<>();
    private final Map<DagNode, DagNode> destroyingNodes = new HashMap<>();

    // Management of memory barriers.
    protected final Set<DagNode> liveNodes = new HashSet<>();
    protected final Set<DagNode> readingMemoryAccesses = new HashSet<>();
    protected DagNode memoryChange = null;

    // Reservation for special entry and exit point.
    protected DagNode entry, exit;

    public CSEGraph(BasicBlock basicBlock, Map<String, Boolean> memoryManipulatingCalls) {
        this.memoryManipulatingCalls = (memoryManipulatingCalls != null)
                ? memoryManipulatingCalls : Collections.emptyMap();
        this.basicBlock = requireNonNull(basicBlock, "BasicBlock for local CSE cannot be null!");

        // TODO: It is possible to assert a valid state after every added node.
        basicBlock.forEach(this);

        assert entry != null : "Incomplete graph: Missing begin!";
        assert exit != null : "Incomplete graph: Missing exit!";

        insertAllUseBeforeDestroyDependencies();
        addEntryExitDependencies();
    }

    /**
     * Returns the underlying {@link BasicBlock} used to generate this graph.
     */
    public BasicBlock getBasicBlock() {
        return basicBlock;
    }

    /**
     * Checks whether or not the given node is a memory reading node.
     *
     * @return <code>true</code> if the node is an {@link ArithmeticDagNode} and its operator is
     * {@link ArithmeticDagNode.Operator#MEM_READ}
     */
    private boolean isMemoryRead(DagNode node) {
        return node instanceof ArithmeticDagNode &&
                ((ArithmeticDagNode) node).operator == ArithmeticDagNode.Operator.MEM_READ;
    }

    /**
     * Checks whether or not the first parameter destroys the second.
     *
     * @param destroyingNode The node which should destroy the other.
     * @param destroyedNode  The node which is destroyed by the other.
     * @return <code>true</code> if one of the following cases occurs:
     * <ol>
     *  <li>The destroying node is an {@link ArithmeticDagNode} with a
     *  {@link ArithmeticDagNode.Operator#isDestructive() destructive operator} and the {@link ArithmeticDagNode#source}
     *  is the destroyed node.</li>
     *
     *  <li>The destroying node is a {@link ProcedureDagNode} and the destroyed node is one of its children.</li>
     *
     *  <li>The destroying node is an {@link EntryExitDagNode} with an
     *  {@link EntryExitDagNode.Variant#isExit() exit variant} and the parameter list contains the destroyed node.</li>
     * </ol>
     */
    private boolean isDestroyingNode(DagNode destroyingNode, DagNode destroyedNode) {
        if (destroyingNode instanceof ArithmeticDagNode arithmeticNode) {
            return arithmeticNode.operator.isDestructive() &&
                    arithmeticNode.source.equals(destroyedNode);

        } else if (destroyingNode instanceof ProcedureDagNode procedureNode) {
            return procedureNode.getDirectChildren().contains(destroyedNode);

        } else if (destroyingNode instanceof EntryExitDagNode entryExitNode) {
            return entryExitNode.variant.isExit() &&
                    entryExitNode.parameterList.contains(destroyedNode);
        }

        return false;
    }

    /**
     * Tries to find a node in {@link CSEGraph#liveNodes} that is structurally equal to the given node.
     */
    private <N extends DagNode> Optional<N> findNode(N node) {
        for (DagNode liveNode : liveNodes) {
            if (node.structurallyEquals(liveNode)) {
                @SuppressWarnings("unchecked") /* should never fail, since equals returned true. */
                        N returnValue = (N) liveNode;
                return Optional.of(returnValue);
            }
        }

        return Optional.empty();
    }

    /**
     * Inserts a node into this graph.
     * <p>
     * The given node is added to the set of all nodes, the set of live nodes and the set of reading memory
     * accesses (if applicable). Additionally this method checks whether the node is an entry or exit point
     * and stores them accordingly.
     * <p>
     * This method calls {@link DagNode#registerChildren()} on the inserted node, so its children are notified
     * that a new parent exists.
     */
    private <N extends DagNode> N insertNode(N node) {
        nodes.add(node);
        liveNodes.add(node);

        if (isMemoryRead(node)) {
            readingMemoryAccesses.add(node);
        }

        if (node instanceof EntryExitDagNode) {
            if (((EntryExitDagNode) node).variant.isExit()) {
                exit = node;
            } else {
                entry = node;
            }
        }

        node.registerChildren();
        return node;
    }

    /**
     * Tries to find a structurally equal node and returns it, if found. Otherwise the given node is inserted
     * into the graph and returned.
     */
    private <N extends DagNode> N findOrInsertNode(N node) {
        return findNode(node)
                .orElseGet(() -> insertNode(node));
    }

    /**
     * Inserts a node for a given {@link RValue}. This method handles:
     * <ul>
     *  <li>Aliasing for variables.</li>
     *  <li>Management of creation for variable nodes.</li>
     *  <li>Management of memory changes for memory reading nodes.</li>
     * </ul>
     */
    public DagNode findValueNodeOrInsert(RValue value) {
        if (value instanceof Constant constant) {
            return findOrInsertNode(new AtomicDagNode(constant));

        } else if (value instanceof Variable variable) {

            DagNode potentialResult = creatingNodes.get(variable);
            if (potentialResult != null) {
                return potentialResult;
            }

            AtomicDagNode result = insertNode(new AtomicDagNode(variable));
            creatingNodes.put(variable, result);
            return result;

        } else {
            MemoryAccess memoryAccess = (MemoryAccess) value;
            Optional<InBoundsView> bounds = memoryAccess.getBoundsCheck().map(Annotation::getView);

            DagNode index = findValueNodeOrInsert(memoryAccess.index);
            DagNode memoryAreaBase = bounds.map(bound -> findValueNodeOrInsert(bound.getBaseAddress())).orElse(null);
            DagNode memoryAreaSize = bounds.map(bound -> findValueNodeOrInsert(bound.getSize())).orElse(null);

            ArithmeticDagNode memoryAccessNode = ArithmeticDagNode.memoryAccess(index, memoryAreaBase, memoryAreaSize);
            if (memoryChange != null) {
                memoryAccessNode.addSequentialChildren(memoryChange);
            }

            return findOrInsertNode(memoryAccessNode);
        }
    }

    /**
     * Inserts a node for a given arithmetic computation. This method handles management of destroying nodes.
     */
    public ArithmeticDagNode findArithmeticNodeOrInsert(Atom source, ArithmeticDagNode.Operator operator,
                                                        RValue left, BinaryOperand.BinaryOperator auxiliaryOperator, RValue right) {
        DagNode nodeSource = findValueNodeOrInsert(source);
        DagNode nodeLeft = findValueNodeOrInsert(left);
        DagNode nodeRight = findValueNodeOrInsert(right);

        ArithmeticDagNode result = new ArithmeticDagNode(nodeSource, operator, nodeLeft, auxiliaryOperator, nodeRight);
        Optional<ArithmeticDagNode> eliminatedSubexpression = findNode(result);


        if (eliminatedSubexpression.isEmpty() && isAlreadyDestroyed(nodeSource)
                && nodeRepresentsVariable(nodeSource)) {
            // We are inserting a new node that ALSO destroys a destroyed source.

            nodeSource = explicitCopy(nodeSource);
            result = new ArithmeticDagNode(nodeSource, operator, nodeLeft, auxiliaryOperator, nodeRight);
            insertNode(result);

        } else if (eliminatedSubexpression.isPresent()) {
            result = eliminatedSubexpression.get();
        } else {
            insertNode(result);
        }

        markDestroying(nodeSource, result);
        return result;
    }

    public DagNode explicitCopy(DagNode node) {
        Atom target;
        if (node.hasMarks()) {
            target = node.getBestMark();
        } else if (node instanceof AtomicDagNode) {
            target = ((AtomicDagNode) node).getValue();
        } else {
            throw new IllegalStateException("The graph has a weird structure, where values are needed but not present!");
        }

        ArithmeticDagNode explicitCopy = new ArithmeticDagNode(
                findValueNodeOrInsert(new Constant(0)), ArithmeticDagNode.Operator.XOR,
                findValueNodeOrInsert(new Constant(0)), BinaryOperand.BinaryOperator.XOR,
                findValueNodeOrInsert(target));

        insertNode(explicitCopy);

        Variable mark = Variable.nextTemporaryVariable();
        mark.number = 0;
        updateNodeWithMark(explicitCopy, mark);
        return explicitCopy;
    }

    /**
     * Add a destructor node if one node destroys another.
     */
    private void markDestroying(DagNode destroyedNode, DagNode destructor) {
        destroyingNodes.put(destroyedNode, destructor);
    }

    private boolean isAlreadyDestroyed(DagNode node) {
        return destroyingNodes.containsKey(node);
    }

    /**
     * Returns <code>true</code> if the node passed to this method represents a variable in the generated
     * code.
     */
    private boolean nodeRepresentsVariable(DagNode node) {
        if (node instanceof AtomicDagNode) {
            // Atomic node is a Variable
            return ((AtomicDagNode) node).getValue() instanceof Variable;
        } else if (node.hasMarks()) {
            // Marked node is assigned to a variable.
            return node.getBestMark() instanceof Variable;
        }
        return false;
    }

    /**
     * This method inserts sequential dependencies for destroying nodes. The dependencies inserted by this method
     * ensure, that a value is used before it is destroyed.
     */
    private void insertAllUseBeforeDestroyDependencies() {
        destroyingNodes.forEach((destroyedNode, destructor) -> {
            if (nodeRepresentsVariable(destroyedNode)) { // We don't care about constants here, since they cannot be destroyed.
                addUseBeforeDestroyDependencies(destructor, destroyedNode);
            }
        });
    }

    /**
     * This method inserts sequential dependencies for destroying nodes. The dependencies inserted by this method
     * ensure that the (non destructive) parents of the destroyed node are evaluated before the destructive parent
     * is. This way a value is used, before it is destroyed.
     * <p>
     * Since memory reads do not bind a value, a sequential dependency is added to their parents instead. This way
     * the value of the memory read is used before its index is destroyed.
     */
    private void addUseBeforeDestroyDependencies(DagNode destroyingNode, DagNode destroyedNode) {
        Queue<DagNode> requiredDependencies = new ArrayDeque<>(destroyedNode.getDirectParents());

        while (!requiredDependencies.isEmpty()) {
            DagNode parent = requiredDependencies.poll();

            if (isMemoryRead(parent)) { // Memory reads dont bind a value, ensure that they are used too.
                requiredDependencies.addAll(parent.getDirectParents());

            } else if (!isDestroyingNode(parent, destroyedNode)) {
                destroyingNode.addSequentialChildren(parent);
            }
        }
    }

    /**
     * Adds sequential dependencies for entry and exit nodes. This way it is ensured, that the entry node is
     * evaluated first and the exit node is evaluated last.
     * <p>
     * As entry and exit nodes may have direct children on their own (in case of conditional nodes) this method
     * has to ensure, that no circles are created by inserting dependencies.
     */
    private void addEntryExitDependencies() {
        Collection<DagNode> entryChildren = entry.getAllChildren();
        Collection<DagNode> exitChildren = exit.getAllChildren();

        // Add all roots (all computations) as sequential children of exit. They have to be executed before exiting.
        for (DagNode root : nodes) {
            if (root.isRoot() && !root.equals(exit)) {
                if (!exitChildren.contains(root)) { // Do not introduce cyclic dependencies.
                    exit.addSequentialChildren(root);
                }
            }
        }

        // Add entry as sequential child for all leaves (all values). Entry must be emitted before they can be used.
        for (DagNode leaf : nodes) {
            if (leaf.isLeaf()) {
                Queue<DagNode> valuesInGraph = new ArrayDeque<>();
                valuesInGraph.add(leaf);

                while (!valuesInGraph.isEmpty()) {
                    DagNode value = valuesInGraph.poll();

                    if (entryChildren.contains(value)) { // Do not introduce cyclic dependency. Instead all parents of the value require the entry.
                        valuesInGraph.addAll(value.getDirectParents());
                    } else if (!value.equals(entry)) {
                        value.addSequentialChildren(entry);
                    }
                }
            }
        }
    }

    /**
     * Adds a mark to a node. If the mark is a variable, this method registers the node as a creating node for the
     * given variable. If the variable is an alias, it is resolved internally.
     */
    public void updateNodeWithMark(DagNode node, Atom created) {
        if (created instanceof Variable) {
            creatingNodes.put((Variable) created, node);
        }

        node.mark(created);
    }

    /**
     * Returns <code>true</code> if the procedure called by the given {@link CallInstruction}
     * changed memory. Whether or not a procedure changes memory is determined by solving the
     * {@link MemoryManipulatingProcedures} problem and therefore creating a {@link Map} which
     * contains the <code>boolean</code> information for every procedure.
     * <p>
     * If the {@link Map} is not created correctly or has missing entries, this method
     * assumes that the unknown procedure will change memory.
     */
    public boolean callChangesMemory(CallInstruction instruction) {
        return memoryManipulatingCalls.getOrDefault(instruction.procedure, true);
    }

    /**
     * Inserts a memory barrier with the given node as a memory changing node.
     * <p>
     * This method ensures, that no values dependent on the changed memory are used afterwards and that the order
     * of memory operations is correct when generating code from this graph.
     */
    public void addMemoryBarrier(DagNode memoryChangingNode) {
        for (DagNode readingMemoryAccess : readingMemoryAccesses) {
            memoryChangingNode.addSequentialChildren(readingMemoryAccess.getDirectParents());
        }

        for (DagNode readingMemoryAccess : readingMemoryAccesses) {
            liveNodes.removeAll(readingMemoryAccess.getAllParents());
        }
        liveNodes.removeAll(readingMemoryAccesses);

        readingMemoryAccesses.clear();
        memoryChange = memoryChangingNode;
    }

    private void importAnnotationsFromParameterList(EntryExitDagNode node, List<Atom> parameterList) {
        for (Atom parameter : parameterList) {
            List<CSEAnnotationWrapper> annotations = new ArrayList<>();

            for (Annotation annotation : parameter.getAnnotations()) {
                annotations.add(new CSEAnnotationWrapper(this, annotation));
            }
            node.annotations.add(annotations);
        }
    }

    private void importAnnotationsFromInstruction(ArithmeticDagNode node, Instruction instruction) {
        for (Annotation annotation : instruction.getAnnotations()) {
            node.annotations.add(new CSEAnnotationWrapper(this, annotation));
        }
    }

    /**
     * Creates a list of results for a node with multiple values.
     * <p>
     * The results are not placed as marks, as this would imply that all marks have an equal value. Instead
     * the results are inserted as fresh nodes with a sequential dependency to their creating node. Only variables
     * are inserted this way, as constants do not depend on the creating node, since they have a fixed value.
     */
    private List<DagNode> addResultList(List<Atom> resultList, DagNode node) {
        List<DagNode> resultNodes = new ArrayList<>(resultList.size());

        // We can't simply add all results as marks, since that would mean the call only creates one of these values
        // marks and that they are all equal. Thus we insert all of the results into the graph and add sequential
        // dependencies.
        for (Atom created : resultList) {
            DagNode resultNode = findValueNodeOrInsert(created);
            assert resultNode instanceof AtomicDagNode;
            resultNodes.add(resultNode);

            // Variables have to be "returned" by an entry/call. Constants can always be used.
            if (created instanceof Variable
                    // Don't introduce cyclic dependencies in conditional entry.
                    && !node.getAllChildren().contains(resultNode)) {
                resultNode.addSequentialChildren(node);
            }
        }
        return resultNodes;
    }

    /**
     * Creates a list of parameters that are consumed by a node accepting multiple values.
     */
    private List<DagNode> addParameterList(List<Atom> parameters, DagNode node) {
        List<DagNode> resultNode = new ArrayList<>();

        for (Atom parameter : parameters) {
            DagNode returnedNode = findValueNodeOrInsert(parameter);
            if (isAlreadyDestroyed(returnedNode) && nodeRepresentsVariable(returnedNode)) {
                returnedNode = explicitCopy(returnedNode);
            }

            markDestroying(returnedNode, node);
            resultNode.add(returnedNode);
        }

        return resultNode;
    }


    @Override
    protected void visitVoid(ArithmeticAssignment instruction) {
        ArithmeticDagNode.Operator operator = ArithmeticDagNode.Operator.fromModificationOperator(instruction.operator, false);
        ArithmeticDagNode node = findArithmeticNodeOrInsert(instruction.source, operator,
                instruction.left, instruction.arithmeticOperator, instruction.right);

        updateNodeWithMark(node, instruction.target);
        importAnnotationsFromInstruction(node, instruction);
    }

    @Override
    protected void visitVoid(SwapInstruction instruction) {
        DagNode right = findValueNodeOrInsert(instruction.sourceRight);
        updateNodeWithMark(right, instruction.targetRight);

        DagNode left = findValueNodeOrInsert(instruction.sourceLeft);
        updateNodeWithMark(left, instruction.targetLeft);
    }


    @Override
    protected void visitVoid(CallInstruction instruction) {
        ProcedureDagNode callNode = new ProcedureDagNode(instruction.direction, instruction.procedure);

        List<DagNode> parameters = addParameterList(instruction.inputList, callNode);
        callNode.parameters.addAll(parameters);

        if (callChangesMemory(instruction)) {
            addMemoryBarrier(callNode);
        }
        insertNode(callNode);

        List<DagNode> results = addResultList(instruction.outputList, callNode);
        callNode.results.addAll(results);
    }

    @Override
    protected void visitVoid(MemoryAssignment instruction) {
        ArithmeticDagNode.Operator operator = ArithmeticDagNode.Operator.fromModificationOperator(instruction.operator, true);

        DagNode target = findValueNodeOrInsert(instruction.target);
        DagNode left = findValueNodeOrInsert(instruction.left);
        DagNode right = findValueNodeOrInsert(instruction.right);

        ArithmeticDagNode memoryAssignment = new ArithmeticDagNode(target, operator, left, instruction.arithmeticOperator, right);
        addMemoryBarrier(memoryAssignment);
        insertNode(memoryAssignment);
        importAnnotationsFromInstruction(memoryAssignment, instruction);
    }

    @Override
    protected void visitVoid(MemoryInterchangeInstruction instruction) {
        DagNode destroyedNode = findValueNodeOrInsert(instruction.right);
        DagNode memoryNode = findValueNodeOrInsert(instruction.memoryAccess);

        if (isAlreadyDestroyed(destroyedNode) && nodeRepresentsVariable(destroyedNode)) {
            destroyedNode = explicitCopy(destroyedNode);
        }

        ArithmeticDagNode memoryInterchange = ArithmeticDagNode.memoryInterchange(destroyedNode, memoryNode);
        updateNodeWithMark(memoryInterchange, instruction.left);
        markDestroying(destroyedNode, memoryInterchange);

        addMemoryBarrier(memoryInterchange);
        insertNode(memoryInterchange);
        importAnnotationsFromInstruction(memoryInterchange, instruction);
    }

    @Override
    protected void visitVoid(MemorySwapInstruction instruction) {
        DagNode memoryLeft = findValueNodeOrInsert(instruction.left);
        DagNode memoryRight = findValueNodeOrInsert(instruction.right);

        ArithmeticDagNode memorySwap = ArithmeticDagNode.memorySwap(memoryLeft, memoryRight);
        addMemoryBarrier(memorySwap);
        insertNode(memorySwap);
        importAnnotationsFromInstruction(memorySwap, instruction);
    }


    @Override
    protected void visitVoid(EndInstruction instruction) {
        EntryExitDagNode node = new EntryExitDagNode(EntryExitDagNode.Variant.END, instruction.label);
        node.parameterList.addAll(addParameterList(instruction.getParameters(), node));

        insertNode(node);
        importAnnotationsFromParameterList(node, instruction.getParameters());
    }

    @Override
    protected void visitVoid(UnconditionalExit instruction) {
        EntryExitDagNode node = new EntryExitDagNode(EntryExitDagNode.Variant.EXIT_UNCONDITIONAL, instruction.label);
        node.parameterList.addAll(addParameterList(instruction.getParameters(), node));

        insertNode(node);
    }

    @Override
    protected void visitVoid(ConditionalExit instruction) {
        DagNode conditionLeft = findValueNodeOrInsert(instruction.left);
        DagNode conditionRight = findValueNodeOrInsert(instruction.right);

        EntryExitDagNode node = new EntryExitDagNode(EntryExitDagNode.Variant.EXIT_CONDITIONAL,
                instruction.trueLabel, instruction.falseLabel,
                conditionLeft, instruction.operator, conditionRight);
        node.parameterList.addAll(addParameterList(instruction.getParameters(), node));

        insertNode(node);
    }

    @Override
    protected void visitVoid(BeginInstruction instruction) {
        EntryExitDagNode node = new EntryExitDagNode(EntryExitDagNode.Variant.BEGIN, instruction.label);
        node.parameterList.addAll(addResultList(instruction.getParameters(), node));

        insertNode(node);
        importAnnotationsFromParameterList(node, instruction.getParameters());
    }

    @Override
    protected void visitVoid(UnconditionalEntry instruction) {
        EntryExitDagNode node = new EntryExitDagNode(EntryExitDagNode.Variant.ENTRY_UNCONDITIONAL, instruction.label);
        node.parameterList.addAll(addResultList(instruction.getParameters(), node));

        insertNode(node);
    }

    @Override
    protected void visitVoid(ConditionalEntry instruction) {
        DagNode conditionLeft = findValueNodeOrInsert(instruction.left);
        DagNode conditionRight = findValueNodeOrInsert(instruction.right);

        EntryExitDagNode node = new EntryExitDagNode(EntryExitDagNode.Variant.ENTRY_CONDITIONAL,
                instruction.trueLabel, instruction.falseLabel,
                conditionLeft, instruction.operator, conditionRight);
        node.parameterList.addAll(addResultList(instruction.getParameters(), node));

        insertNode(node);
    }

    /**
     * Construct a path through this graph that visits all nodes in an order, that respects every nodes dependency.
     * That is, if a node is visited by this path, all its dependencies have been visited before.
     */
    public Iterable<DagNode> getPath() {
        enforceNoCycles();
        Queue<DagNode> path = new ArrayDeque<>();
        Deque<DagNode> nodeStack = new LinkedList<>();

        nodeStack.push(exit);

        while (!nodeStack.isEmpty()) {
            DagNode node = nodeStack.peek();

            if (path.contains(node)) {
                nodeStack.pop();
                continue;
            }

            if (hasUnfinishedDependencies(node.getSequentialChildren(),
                    path, nodeStack)) {
                continue;
            }

            if (hasUnfinishedDependencies(node.getDirectChildren(),
                    path, nodeStack)) {
                continue;
            }

            nodeStack.pop();
            path.add(node);
        }

        return path;
    }

    private boolean hasUnfinishedDependencies(Collection<DagNode> dependencies,
                                              Queue<DagNode> path, Deque<DagNode> nodeStack) {
        boolean hasDependency = false;

        for (DagNode dependency : dependencies) {
            if (!path.contains(dependency)) {
                nodeStack.push(dependency);
                hasDependency = true;
            }
        }

        return hasDependency;
    }

    private void enforceNoCycles() throws IllegalStateException {
        final Function<DagNode, Set<DagNode>> dependenciesOfNode = (DagNode node) -> SetOperations.union(
                Set.copyOf(node.getDirectChildren()),
                Set.copyOf(node.getSequentialChildren()));

        if (CollectionUtils.containsCycle(dependenciesOfNode, nodes)) {
            throw new IllegalStateException("DAG should not contain cycles!");
        }
    }

    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    // GraphViz and String operations.
    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public String graphString() {
        StringBuilder sb = new StringBuilder();
        // digraph ID {
        sb.append("digraph ")
                .append(graphId())
                .append(" { ");

        for (DagNode node : nodes) {
            // ID[fontname="courier"
            sb.append(GraphViz.validIdFromHash(node.getNodeID()))
                    .append("[fontname=\"courier\"");

            // ,label="
            sb.append(",label=\"")
                    .append(node.getSimpleRepresentation());

            // Add all marks
            if (!node.getMarks().isEmpty()) {
                String markList = node.getMarks().stream()
                        .map(String::valueOf)
                        .collect(Collectors.joining(",", "\\n[", "]"));

                sb.append(markList);
            }


            // "];
            sb.append("\"];");
        }

        for (DagNode node : nodes) {
            Set<DagNode> alreadyDestroyedNode = new HashSet<>();

            for (DagNode directChild : node.getDirectChildren()) {
                if (!alreadyDestroyedNode.contains(directChild) && isDestroyingNode(node, directChild)) {
                    // Prevent duplicate bold lines if Operand occurs as destroyed child as well as used.
                    alreadyDestroyedNode.add(directChild);

                    sb.append(String.format("%s -> %s [style=bold]; ",
                            GraphViz.validIdFromHash(node.getNodeID()), GraphViz.validIdFromHash(directChild.getNodeID())));

                } else {
                    sb.append(String.format("%s -> %s; ",
                            GraphViz.validIdFromHash(node.getNodeID()), GraphViz.validIdFromHash(directChild.getNodeID())));

                }

            }
            for (DagNode sequentialChild : node.getSequentialChildren()) {
                sb.append(String.format("%s -> %s [style=\"dashed\"];",
                        GraphViz.validIdFromHash(node.getNodeID()), GraphViz.validIdFromHash(sequentialChild.getNodeID())));
            }
        }

        // }
        sb.append("}");
        return sb.toString();
    }

    @Override
    public String graphId() {
        return String.format("cse_%s", basicBlock.graphId());
    }
}
