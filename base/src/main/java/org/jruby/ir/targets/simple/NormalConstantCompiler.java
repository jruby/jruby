package org.jruby.ir.targets.simple;

import org.jruby.ir.targets.ConstantCompiler;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
import static org.jruby.util.CodegenUtils.sig;

public class NormalConstantCompiler implements ConstantCompiler {
    private final IRBytecodeAdapter compiler;

    public NormalConstantCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
    }

    @Override
    public void searchConst(String id, ByteList name, boolean noPrivateConsts) {
        compiler.getValueCompiler().pushConstantLookupSite(compiler.getClassData().clsName, compiler.getUniqueSiteName(id), name);
        compiler.adapter.dup_x2();
        compiler.adapter.pop();
        compiler.adapter.ldc(noPrivateConsts);
        compiler.adapter.invokevirtual(p(ConstantLookupSite.class), "searchConst", sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class, boolean.class)));
    }

    @Override
    public void searchModuleForConst(String id, ByteList name, boolean noPrivateConsts, boolean callConstMissing) {
        compiler.getValueCompiler().pushConstantLookupSite(compiler.getClassData().clsName, compiler.getUniqueSiteName(id), name);
        compiler.adapter.dup_x2();
        compiler.adapter.pop();
        compiler.adapter.ldc(noPrivateConsts);
        compiler.adapter.ldc(callConstMissing);
        compiler.adapter.invokevirtual(p(ConstantLookupSite.class), "searchModuleForConst", sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class, boolean.class, boolean.class)));
    }

    @Override
    public void inheritanceSearchConst(String id, ByteList name) {
        compiler.getValueCompiler().pushConstantLookupSite(compiler.getClassData().clsName, compiler.getUniqueSiteName(id), name);
        compiler.adapter.dup_x2();
        compiler.adapter.pop();
        compiler.adapter.invokevirtual(p(ConstantLookupSite.class), "inheritanceSearchConst", sig(JVM.OBJECT, params(ThreadContext.class, IRubyObject.class)));
    }

    @Override
    public void lexicalSearchConst(String id, ByteList name) {
        compiler.getValueCompiler().pushConstantLookupSite(compiler.getClassData().clsName, compiler.getUniqueSiteName(id), name);
        compiler.adapter.dup_x2();
        compiler.adapter.pop();
        compiler.adapter.invokevirtual(p(ConstantLookupSite.class), "lexicalSearchConst", sig(JVM.OBJECT, params(ThreadContext.class, StaticScope.class)));
    }
}
