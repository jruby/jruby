package org.jruby.ir.targets.indy;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRManager;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.ValueCompiler;
import org.jruby.ir.targets.simple.NormalValueCompiler;
import org.jruby.runtime.CallType;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;

import java.math.BigInteger;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.sig;

public class IndyValueCompiler implements ValueCompiler {
    private final IRBytecodeAdapter compiler;
    private final NormalValueCompiler normalValueCompiler;

    public IndyValueCompiler(IRBytecodeAdapter compiler) {
        this.compiler = compiler;
        this.normalValueCompiler = new NormalValueCompiler(compiler);
    }

    public void pushRuntime() {
        compiler.loadContext();
        compiler.adapter.invokedynamic("runtime", sig(Ruby.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushObjectClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getObject", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushUndefined() {
        compiler.adapter.getstatic(p(UndefinedValue.class), "UNDEFINED", ci(UndefinedValue.class));
    }

    public void pushFixnum(long l) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("fixnum", CodegenUtils.sig(JVM.OBJECT, ThreadContext.class), FixnumObjectSite.BOOTSTRAP, l);
    }

    public void pushFloat(double d) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("flote", sig(JVM.OBJECT, ThreadContext.class), FloatObjectSite.BOOTSTRAP, d);
    }

    public void pushString(ByteList bl, int cr) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("string", sig(RubyString.class, ThreadContext.class), Bootstrap.string(), RubyEncoding.decodeRaw(bl), bl.getEncoding().toString(), cr);
    }

    public void pushFrozenString(ByteList bl, int cr, String file, int line) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("frozen", sig(RubyString.class, ThreadContext.class), Bootstrap.fstring(), RubyEncoding.decodeRaw(bl), bl.getEncoding().toString(), cr, file, line);
    }

    public void pushEmptyString(Encoding encoding) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("emptyString", sig(RubyString.class, ThreadContext.class), Bootstrap.EMPTY_STRING_BOOTSTRAP, encoding.toString());
    }

    public void pushByteList(ByteList bl) {
        compiler.adapter.invokedynamic("bytelist", sig(ByteList.class), Bootstrap.bytelist(), RubyEncoding.decodeRaw(bl), bl.getEncoding().toString());
    }

    public void pushRegexp(ByteList source, int options) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("regexp", sig(RubyRegexp.class, ThreadContext.class), RegexpObjectSite.BOOTSTRAP, RubyEncoding.decodeRaw(source), source.getEncoding().toString(), options);
    }

    public void pushSymbol(final ByteList bytes) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("symbol", sig(JVM.OBJECT, ThreadContext.class), SymbolObjectSite.BOOTSTRAP, RubyEncoding.decodeRaw(bytes), bytes.getEncoding().toString());
    }

    public void pushSymbolProc(final ByteList bytes) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("symbolProc", sig(JVM.OBJECT, ThreadContext.class), SymbolProcObjectSite.BOOTSTRAP, RubyEncoding.decodeRaw(bytes), bytes.getEncoding().toString());
    }

    public void pushEncoding(Encoding encoding) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("encoding", sig(RubyEncoding.class, ThreadContext.class), Bootstrap.contextValueString(), new String(encoding.getName()));
    }

    public void pushNil() {
        compiler.loadContext();
        compiler.adapter.invokedynamic("nil", sig(IRubyObject.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushBoolean(boolean b) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(b ? "True" : "False", sig(IRubyObject.class, ThreadContext.class), Bootstrap.contextValue());
    }

    public void pushBignum(BigInteger bigint) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("bignum", sig(RubyBignum.class, ThreadContext.class), BignumObjectSite.BOOTSTRAP, bigint.toString());
    }

    @Override
    public void pushCallSite(String className, String siteName, String scopeFieldName, CallBase call) {
        CallType callType = call.getCallType();
        boolean profileCandidate = call.hasLiteralClosure() && scopeFieldName != null && IRManager.IR_INLINER;
        boolean profiled = false;
        boolean refined = call.isPotentiallyRefined();

        boolean specialSite = profiled || refined || profileCandidate;

        SkinnyMethodAdapter method = compiler.adapter;

        if (!specialSite) {
            // use indy to cache the site object
            method.invokedynamic("callSite", sig(CachingCallSite.class), Bootstrap.CALLSITE, call.getId(), callType.ordinal());
            return;
        }

        normalValueCompiler.pushCallSite(className, siteName, scopeFieldName, call);
    }

    @Override
    public void pushConstantLookupSite(String className, String siteName, ByteList name) {
        normalValueCompiler.pushConstantLookupSite(className, siteName, name);
    }
}
