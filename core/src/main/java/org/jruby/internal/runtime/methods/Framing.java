package org.jruby.internal.runtime.methods;

import java.util.EnumSet;
import org.jruby.anno.FrameField;
import static org.jruby.anno.FrameField.*;

public enum Framing {
    Full(EnumSet.allOf(FrameField.class)),
    Backtrace(EnumSet.of(METHODNAME, FILENAME, LINE)),
    None(EnumSet.noneOf(FrameField.class));

    private final EnumSet<FrameField> frameField;

    Framing(EnumSet<FrameField> frameField) {
        this.frameField = frameField;
    }
}