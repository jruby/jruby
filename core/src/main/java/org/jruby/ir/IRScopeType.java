package org.jruby.ir;

import org.jruby.ir.persistence.IRPersistableEnum;

public enum IRScopeType implements IRPersistableEnum {
    CLOSURE, EVAL_SCRIPT, INSTANCE_METHOD, CLASS_METHOD, MODULE_BODY, CLASS_BODY, METACLASS_BODY, SCRIPT_BODY;
} 
