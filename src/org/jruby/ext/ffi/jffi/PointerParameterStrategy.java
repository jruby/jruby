package org.jruby.ext.ffi.jffi;

import com.kenai.jffi.ObjectParameterStrategy;
import com.kenai.jffi.ObjectParameterType;

/**
 *
 */
abstract public class PointerParameterStrategy extends ObjectParameterStrategy {

    PointerParameterStrategy(boolean isDirect) {
        super(isDirect);
    }

    PointerParameterStrategy(boolean isDirect, ObjectParameterType objectType) {
        super(isDirect, objectType);
    }

    PointerParameterStrategy(StrategyType type) {
        super(type);
    }

    PointerParameterStrategy(StrategyType type, ObjectParameterType objectType) {
        super(type, objectType);
    }
}
