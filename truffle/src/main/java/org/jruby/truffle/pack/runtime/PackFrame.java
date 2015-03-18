package org.jruby.truffle.pack.runtime;

import com.oracle.truffle.api.frame.FrameDescriptor;
import com.oracle.truffle.api.frame.FrameSlot;
import com.oracle.truffle.api.frame.FrameSlotKind;

public class PackFrame {

    public static final PackFrame INSTANCE = new PackFrame();

    private FrameDescriptor frameDescriptor;
    private FrameSlot sourceSlot;
    private FrameSlot sourceLengthSlot;
    private FrameSlot sourcePositionSlot;
    private FrameSlot outputSlot;
    private FrameSlot outputPositionSlot;

    private PackFrame() {
        frameDescriptor = new FrameDescriptor();
        sourceSlot = frameDescriptor.addFrameSlot("source", FrameSlotKind.Object);
        sourceLengthSlot = frameDescriptor.addFrameSlot("source-length", FrameSlotKind.Int);
        sourcePositionSlot = frameDescriptor.addFrameSlot("source-position", FrameSlotKind.Int);
        outputSlot = frameDescriptor.addFrameSlot("output", FrameSlotKind.Object);
        outputPositionSlot = frameDescriptor.addFrameSlot("output-position", FrameSlotKind.Int);
    }

    public FrameDescriptor getFrameDescriptor() {
        return frameDescriptor;
    }

    public FrameSlot getSourceSlot() {
        return sourceSlot;
    }

    public FrameSlot getSourceLengthSlot() {
        return sourceLengthSlot;
    }

    public FrameSlot getSourcePositionSlot() {
        return sourcePositionSlot;
    }

    public FrameSlot getOutputSlot() {
        return outputSlot;
    }

    public FrameSlot getOutputPositionSlot() {
        return outputPositionSlot;
    }

}
