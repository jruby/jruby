package org.jruby.runtime.backtrace;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jruby.evaluator.ASTInterpreter;
import org.jruby.ir.interpreter.Interpreter;

public enum FrameType {
    METHOD, BLOCK, EVAL, CLASS, ROOT;
    public static final Set<String> INTERPRETED_CLASSES = new HashSet<String>();
    public static final Map<String, FrameType> INTERPRETED_FRAMES = new HashMap<String, FrameType>();

    static {
        INTERPRETED_CLASSES.add(ASTInterpreter.class.getName());
        INTERPRETED_CLASSES.add(Interpreter.class.getName());

        INTERPRETED_FRAMES.put("INTERPRET_METHOD", FrameType.METHOD);
        INTERPRETED_FRAMES.put("INTERPRET_EVAL", FrameType.EVAL);
        INTERPRETED_FRAMES.put("INTERPRET_CLASS", FrameType.CLASS);
        INTERPRETED_FRAMES.put("INTERPRET_BLOCK", FrameType.BLOCK);
        INTERPRETED_FRAMES.put("INTERPRET_ROOT", FrameType.ROOT);
    }
}
