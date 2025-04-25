package org.jruby.ir.targets.indy;

import org.jcodings.Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyClass;
import org.jruby.RubyEncoding;
import org.jruby.RubyRange;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.ir.IRManager;
import org.jruby.ir.instructions.CallBase;
import org.jruby.ir.operands.StringLiteral;
import org.jruby.ir.operands.UndefinedValue;
import org.jruby.ir.targets.IRBytecodeAdapter;
import org.jruby.ir.targets.JVM;
import org.jruby.ir.targets.ValueCompiler;
import org.jruby.ir.targets.simple.NormalValueCompiler;
import org.jruby.runtime.CallType;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.callsite.CachingCallSite;
import org.jruby.util.ByteList;
import org.jruby.util.CodegenUtils;
import org.jruby.util.cli.Options;

import java.math.BigInteger;
import java.util.ArrayList;
import java.util.List;

import static org.jruby.util.CodegenUtils.ci;
import static org.jruby.util.CodegenUtils.p;
import static org.jruby.util.CodegenUtils.params;
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
        compiler.adapter.invokedynamic("runtime", sig(Ruby.class, ThreadContext.class), LiteralValueBootstrap.CONTEXT_VALUE_HANDLE);
    }
    public void pushArrayClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getArray", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushHashClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getHash", sig(RubyClass.class, ThreadContext.class));
    }

    public void pushObjectClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getObject", sig(RubyClass.class, ThreadContext.class));
    }
    public void pushSymbolClass() {
        compiler.loadContext();
        compiler.invokeIRHelper("getSymbol", sig(RubyClass.class, ThreadContext.class));
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
        compiler.adapter.invokedynamic("string", sig(RubyString.class, ThreadContext.class), StringBootstrap.STRING_BOOTSTRAP, RubyEncoding.decodeRaw(bl), bl.getEncoding().toString(), cr);
    }

    public void pushChilledString(ByteList bl, int cr, String file, int line) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("chilled", sig(RubyString.class, ThreadContext.class), StringBootstrap.CSTRING_BOOTSTRAP, RubyEncoding.decodeRaw(bl), bl.getEncoding().toString(), cr, file, line);
    }

    public void pushFrozenString(ByteList bl, int cr, String file, int line) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("frozen", sig(RubyString.class, ThreadContext.class), StringBootstrap.FSTRING_BOOTSTRAP, RubyEncoding.decodeRaw(bl), bl.getEncoding().toString(), cr, file, line);
    }

    public void pushFrozenString(ByteList bl, int cr) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("frozen", sig(RubyString.class, ThreadContext.class), StringBootstrap.FSTRING_SIMPLE_BOOTSTRAP, RubyEncoding.decodeRaw(bl), bl.getEncoding().toString(), cr);
    }

    public void pushEmptyString(Encoding encoding) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("emptyString", sig(RubyString.class, ThreadContext.class), StringBootstrap.EMPTY_STRING_BOOTSTRAP, encoding.toString());
    }

    public void pushBufferString(Encoding encoding, int size) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("bufferString", sig(RubyString.class, ThreadContext.class), StringBootstrap.BUFFER_STRING_BOOTSTRAP, encoding.toString(), size);
    }

    public void buildDynamicString(Encoding encoding, int estimatedSize, boolean frozen, boolean chilled, boolean debugFrozen, String file, int line, List<DStringElement> elements) {
        if (elements.size() > 50 || !Options.COMPILE_INVOKEDYNAMIC.load()) {
            normalValueCompiler.buildDynamicString(encoding, estimatedSize, frozen, chilled, debugFrozen, file, line, elements);
            return;
        }

        compiler.loadContext();

        long descriptor = 0;
        int bit = 0;
        int otherCount = 0;
        ArrayList bootstrapArgs = new ArrayList();
        for (DStringElement elt: elements) {
            switch (elt.type()) {
                case STRING:
                    StringLiteral str = (StringLiteral) elt.value();
                    descriptor |= (1 << bit);
                    bootstrapArgs.add(RubyEncoding.decodeRaw(str.getByteList()));
                    bootstrapArgs.add(str.getByteList().getEncoding().toString());
                    bootstrapArgs.add(str.getCodeRange());
                    break;
                case OTHER:
                    ((Runnable) elt.value()).run();
                    otherCount++;
                    break;
            }
            bit++;
        }
        bootstrapArgs.add(estimatedSize * 3/2);
        bootstrapArgs.add(encoding.toString());
        bootstrapArgs.add(frozen);
        bootstrapArgs.add(chilled);
        bootstrapArgs.add(descriptor);
        bootstrapArgs.add(bit);

        compiler.adapter.invokedynamic("buildDynamicString", sig(RubyString.class, params(ThreadContext.class, IRubyObject.class, otherCount)), BuildDynamicStringSite.BUILD_DSTRING_BOOTSTRAP, bootstrapArgs.toArray());
    }

    public void pushByteList(ByteList bl) {
        compiler.adapter.invokedynamic("bytelist", sig(ByteList.class), StringBootstrap.BYTELIST_BOOTSTRAP, RubyEncoding.decodeRaw(bl), bl.getEncoding().toString());
    }

    public void pushRange(Runnable begin, Runnable end, boolean exclusive) {
        compiler.loadContext();
        begin.run();
        end.run();
        compiler.adapter.invokedynamic("range", sig(RubyRange.class, ThreadContext.class, IRubyObject.class, IRubyObject.class), RangeObjectSite.BOOTSTRAP, exclusive ? 1 : 0);
    }

    public void pushRange(long begin, long end, boolean exclusive) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("range", sig(RubyRange.class, ThreadContext.class), RangeObjectSite.BOOTSTRAP_LONG_LONG, begin, end, exclusive ? 1 : 0);
    }

    public void pushEndlessRange(long end, boolean exclusive) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(RangeObjectSite.RANGE_ENDLESS, sig(RubyRange.class, ThreadContext.class), RangeObjectSite.BOOTSTRAP_LONG, end, exclusive ? 1 : 0);
    }

    public void pushBeginlessRange(long begin, boolean exclusive) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(RangeObjectSite.RANGE_BEGINLESS, sig(RubyRange.class, ThreadContext.class), RangeObjectSite.BOOTSTRAP_LONG, begin, exclusive ? 1 : 0);
    }

    public void pushRange(ByteList begin, int beginCR, ByteList end, int endCR, boolean exclusive) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("range", sig(RubyRange.class, ThreadContext.class), RangeObjectSite.BOOTSTRAP_STRING_STRING, RubyEncoding.decodeRaw(begin), begin.getEncoding().toString(), beginCR, RubyEncoding.decodeRaw(end), end.getEncoding().toString(), endCR, exclusive ? 1 : 0);
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

    public void pushRubyEncoding(Encoding encoding) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("rubyEncoding", sig(RubyEncoding.class, ThreadContext.class), LiteralValueBootstrap.CONTEXT_VALUE_STRING_HANDLE, new String(encoding.getName()));
    }

    public void pushEncoding(Encoding encoding) {
        compiler.loadContext();
        compiler.adapter.invokedynamic("encoding", sig(RubyEncoding.class, ThreadContext.class), LiteralValueBootstrap.CONTEXT_VALUE_STRING_HANDLE, new String(encoding.getName()));
    }

    public void pushNil() {
        compiler.loadContext();
        compiler.adapter.invokedynamic("nil", sig(IRubyObject.class, ThreadContext.class), LiteralValueBootstrap.CONTEXT_VALUE_HANDLE);
    }

    public void pushBoolean(boolean b) {
        compiler.loadContext();
        compiler.adapter.invokedynamic(b ? "True" : "False", sig(IRubyObject.class, ThreadContext.class), LiteralValueBootstrap.CONTEXT_VALUE_HANDLE);
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
            method.invokedynamic("callSite", sig(CachingCallSite.class), CallSiteCacheBootstrap.CALLSITE, call.getId(), callType.ordinal());
            return;
        }

        normalValueCompiler.pushCallSite(className, siteName, scopeFieldName, call);
    }

    @Override
    public void pushConstantLookupSite(String className, String siteName, ByteList name) {
        normalValueCompiler.pushConstantLookupSite(className, siteName, name);
    }

    @Override
    public void pushFixnumArray(List<Long> values) {
        String fixnumString = Helpers.encodeLongString(values);

        compiler.adapter.invokedynamic("fixnumArray", sig(RubyArray.class, ThreadContext.class), ArrayBootstrap.NUMERIC_ARRAY, fixnumString);
    }

    @Override
    public void pushFloatArray(List<Double> values) {
        String doubleString = Helpers.encodeDoubleString(values);

        compiler.adapter.invokedynamic("floatArray", sig(RubyArray.class, ThreadContext.class), ArrayBootstrap.NUMERIC_ARRAY, doubleString);
    }

}
