package org.jruby.runtime.backtrace;

import java.util.HashMap;
import java.util.Map;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.interpreter.Interpreter;

public enum FrameType {
    METHOD, BLOCK, EVAL, CLASS, ROOT;
    public static final Map<String, FrameType> INTERPRETED_FRAMES = new HashMap<String, FrameType>();
    static {
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_METHOD", FrameType.METHOD);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_EVAL", FrameType.EVAL);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_CLASS", FrameType.CLASS);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_BLOCK", FrameType.BLOCK);
        INTERPRETED_FRAMES.put(ASTInterpreter.class.getName() + ".INTERPRET_ROOT", FrameType.ROOT);
        INTERPRETED_FRAMES.put(Interpreter.class.getName() + ".INTERPRET_METHOD", FrameType.ROOT);
    }
}
