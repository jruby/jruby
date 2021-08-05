package org.jruby.runtime.backtrace;

import java.util.HashSet;

import org.jruby.internal.runtime.methods.InterpretedIRBodyMethod;
import org.jruby.internal.runtime.methods.InterpretedIRMethod;
import org.jruby.internal.runtime.methods.MixedModeIRMethod;
import org.jruby.ir.instructions.DefineClassInstr;
import org.jruby.ir.instructions.DefineModuleInstr;
import org.jruby.ir.interpreter.Interpreter;

public enum FrameType {
    METHOD, BLOCK, EVAL, CLASS, MODULE, METACLASS, ROOT, VARARGS_WRAPPER;

    private static final HashSet<String> INTERPRETED_CLASSES = new HashSet<String>(6, 1);

    static {
        INTERPRETED_CLASSES.add(Interpreter.class.getName());
        INTERPRETED_CLASSES.add(MixedModeIRMethod.class.getName());
        INTERPRETED_CLASSES.add(InterpretedIRMethod.class.getName());
        INTERPRETED_CLASSES.add(InterpretedIRBodyMethod.class.getName());
        INTERPRETED_CLASSES.add(InterpretedIRBodyMethod.class.getName());
        INTERPRETED_CLASSES.add(DefineClassInstr.class.getName());
        INTERPRETED_CLASSES.add(DefineModuleInstr.class.getName());
    }

    public static boolean isInterpreterFrame(final String className, final String methodName) {
        return getInterpreterFrame(className, methodName) != null;
    }

    public static FrameType getInterpreterFrame(final String methodName) {
        switch ( methodName ) {
            case "INTERPRET_METHOD" : return FrameType.METHOD;
            case "INTERPRET_EVAL" : return FrameType.EVAL;
            case "INTERPRET_CLASS" : return FrameType.CLASS;
            case "INTERPRET_MODULE" : return FrameType.MODULE;
            case "INTERPRET_METACLASS" : return FrameType.METACLASS;
            case "INTERPRET_BLOCK" : return FrameType.BLOCK;
            case "INTERPRET_ROOT" : return FrameType.ROOT;
        }
        return null;
    }

    public static FrameType getInterpreterFrame(final String className, final String methodName) {
        if ( INTERPRETED_CLASSES.contains(className) ) {
            return getInterpreterFrame(methodName);
        }
        return null;
    }

}
