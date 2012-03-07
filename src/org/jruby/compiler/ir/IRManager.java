package org.jruby.compiler.ir;

import java.util.List;
import org.jruby.compiler.ir.compiler_pass.CompilerPass;
import org.jruby.compiler.ir.operands.BooleanLiteral;
import org.jruby.compiler.ir.operands.Nil;

/**
 */
public class IRManager {
    private int dummyMetaClassCount = 0;
    private final IRModuleBody classMetaClass = new IRMetaClassBody(this, null, getMetaClassName(), "", 0, null);
    private final IRModuleBody object = new IRClassBody(this, null, "Object", "", 0, null);
    private final Nil nil = new Nil();
    private final BooleanLiteral trueObject = new BooleanLiteral(true);
    private final BooleanLiteral falseObject = new BooleanLiteral(false);
    
    public IRManager() {
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
    
    public IRModuleBody getClassMetaClass() {
        return classMetaClass;
    }
    
    public String getMetaClassName() {
        return "<DUMMY_MC:" + dummyMetaClassCount++ + ">";
    }
}
