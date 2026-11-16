package rc3.rssa.pass;


import rc3.januscompiler.Direction;
import rc3.lib.DirectionVar;

public final class LabelEntry {
    private final DirectionVar<Integer> address = new DirectionVar<>(0, 0);
    private final boolean isSubroutine;
    private final int frameSize;

    public LabelEntry(boolean isProcedure, int frameSize,
                      int fwPosition, int bwPosition) {
        this.address.set(fwPosition, bwPosition);
        this.isSubroutine = isProcedure;
        this.frameSize = frameSize;
    }

    public int getLabelAddr(Direction direction) {
        return address.get(direction);
    }

    public int getFrameSize() {
        return frameSize;
    }

    public boolean isSubroutine() {
        return isSubroutine;
    }
}
