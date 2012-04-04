package org.jruby.ir;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.jruby.RubyInstanceConfig;
import org.jruby.ir.operands.BooleanLiteral;
import org.jruby.ir.operands.Nil;
import org.jruby.ir.passes.BasicCompilerPassListener;
import org.jruby.ir.passes.CompilerPass;
import org.jruby.ir.passes.CompilerPassListener;
import org.jruby.runtime.load.LoadService;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private final IRModuleBody classMetaClass = new IRMetaClassBody(this, null, getMetaClassName(), "", 0, null);
    private final IRModuleBody object = new IRClassBody(this, null, "Object", "", 0, null);
    private final Nil nil = new Nil();
    private final BooleanLiteral trueObject = new BooleanLiteral(true);
    private final BooleanLiteral falseObject = new BooleanLiteral(false);
    private Set<CompilerPassListener> passListeners = new HashSet<CompilerPassListener>();
    
    public IRManager() {
        if (RubyInstanceConfig.IR_COMPILER_DEBUG) addListener(new BasicCompilerPassListener());
    }
    
    public Nil getNil() {
        return nil;
    }
    
    public BooleanLiteral getTrue() {
        return trueObject;
    }
    
    public BooleanLiteral getFalse() {
        return falseObject;
    }

    public IRModuleBody getObject() {
        return object;
    }
    
    public List<CompilerPass> getCompilerPasses(IRScope scope) {
        return null;
    }
    
    public Set<CompilerPassListener> getListeners() {
        return passListeners;
    }
    
    public void addListener(CompilerPassListener listener) {
        passListeners.add(listener);
    }
    
    public void removeListener(CompilerPassListener listener) {
        passListeners.remove(listener);
    }
    
    public IRModuleBody getClassMetaClass() {
        return classMetaClass;
    }
    
    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }
}
