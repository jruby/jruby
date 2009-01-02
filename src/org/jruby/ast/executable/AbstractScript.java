/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.executable;

import java.math.BigInteger;
import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubySymbol;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.parser.LocalStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.CompiledBlockCallback;
import org.jruby.runtime.CallSite;
import org.jruby.runtime.MethodIndex;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author headius
 */
public abstract class AbstractScript implements Script {
    public AbstractScript() {
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, Block block) {
        return __file__(context, self, IRubyObject.NULL_ARRAY, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg, Block block) {
        return __file__(context, self, new IRubyObject[] {arg}, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
        return __file__(context, self, new IRubyObject[] {arg1, arg2}, block);
    }
    
    public IRubyObject __file__(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
        return __file__(context, self, new IRubyObject[] {arg1, arg2, arg3}, block);
    }
    
    public IRubyObject load(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return null;
    }
    
    public IRubyObject run(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
        return __file__(context, self, args, block);
    }

    public final StaticScope getScope(ThreadContext context, String varNamesDescriptor, int index) {
        StaticScope scope = scopes[index];
        if (scope == null) {
            String[] varNames = varNamesDescriptor.split(";");
            for (int i = 0; i < varNames.length; i++) varNames[i] = varNames[i].intern();
            scope = scopes[index] = new LocalStaticScope(context.getCurrentScope().getStaticScope(), varNames);
        }
        return scope;
    }

    public static final int NUMBERED_SCOPE_COUNT = 10;

    public final StaticScope getScope0(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 0);}
    public final StaticScope getScope1(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 1);}
    public final StaticScope getScope2(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 2);}
    public final StaticScope getScope3(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 3);}
    public final StaticScope getScope4(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 4);}
    public final StaticScope getScope5(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 5);}
    public final StaticScope getScope6(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 6);}
    public final StaticScope getScope7(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 7);}
    public final StaticScope getScope8(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 8);}
    public final StaticScope getScope9(ThreadContext context, String varNamesDescriptor) {return getScope(context, varNamesDescriptor, 9);}

    public final CallSite getCallSite(int index) {
        return callSites[index];
    }

    public static final int NUMBERED_CALLSITE_COUNT = 10;

    public final CallSite getCallSite0() {return callSites[0];}
    public final CallSite getCallSite1() {return callSites[1];}
    public final CallSite getCallSite2() {return callSites[2];}
    public final CallSite getCallSite3() {return callSites[3];}
    public final CallSite getCallSite4() {return callSites[4];}
    public final CallSite getCallSite5() {return callSites[5];}
    public final CallSite getCallSite6() {return callSites[6];}
    public final CallSite getCallSite7() {return callSites[7];}
    public final CallSite getCallSite8() {return callSites[8];}
    public final CallSite getCallSite9() {return callSites[9];}

    /**
     * descriptor format is
     *
     * closure_method_name,arity,varname1;varname2;varname3,has_multi_args_head,arg_type,light
     * 
     * @param context
     * @param index
     * @param descriptor
     * @return
     */
    public final BlockBody getBlockBody(ThreadContext context, int index, String descriptor) {
        BlockBody body = blockBodies[index];
        
        if (body == null) {
            return createBlockBody(context, index, descriptor);
        }

        return body;
    }

    public static final int NUMBERED_BLOCKBODY_COUNT = 10;

    public final BlockBody getBlockBody0(ThreadContext context, String descriptor) {return getBlockBody(context, 0, descriptor);}
    public final BlockBody getBlockBody1(ThreadContext context, String descriptor) {return getBlockBody(context, 1, descriptor);}
    public final BlockBody getBlockBody2(ThreadContext context, String descriptor) {return getBlockBody(context, 2, descriptor);}
    public final BlockBody getBlockBody3(ThreadContext context, String descriptor) {return getBlockBody(context, 3, descriptor);}
    public final BlockBody getBlockBody4(ThreadContext context, String descriptor) {return getBlockBody(context, 4, descriptor);}
    public final BlockBody getBlockBody5(ThreadContext context, String descriptor) {return getBlockBody(context, 5, descriptor);}
    public final BlockBody getBlockBody6(ThreadContext context, String descriptor) {return getBlockBody(context, 6, descriptor);}
    public final BlockBody getBlockBody7(ThreadContext context, String descriptor) {return getBlockBody(context, 7, descriptor);}
    public final BlockBody getBlockBody8(ThreadContext context, String descriptor) {return getBlockBody(context, 8, descriptor);}
    public final BlockBody getBlockBody9(ThreadContext context, String descriptor) {return getBlockBody(context, 9, descriptor);}

    public final CompiledBlockCallback getBlockCallback(Ruby runtime, int index, String method) {
        CompiledBlockCallback callback = blockCallbacks[index];

        if (callback == null) {
            return createCompiledBlockCallback(runtime, index, method);
        }

        return callback;
    }

    public static final int NUMBERED_BLOCKCALLBACK_COUNT = 10;

    public final CompiledBlockCallback getBlockCallback0(Ruby runtime, String method) {return getBlockCallback(runtime, 0, method);}
    public final CompiledBlockCallback getBlockCallback1(Ruby runtime, String method) {return getBlockCallback(runtime, 1, method);}
    public final CompiledBlockCallback getBlockCallback2(Ruby runtime, String method) {return getBlockCallback(runtime, 2, method);}
    public final CompiledBlockCallback getBlockCallback3(Ruby runtime, String method) {return getBlockCallback(runtime, 3, method);}
    public final CompiledBlockCallback getBlockCallback4(Ruby runtime, String method) {return getBlockCallback(runtime, 4, method);}
    public final CompiledBlockCallback getBlockCallback5(Ruby runtime, String method) {return getBlockCallback(runtime, 5, method);}
    public final CompiledBlockCallback getBlockCallback6(Ruby runtime, String method) {return getBlockCallback(runtime, 6, method);}
    public final CompiledBlockCallback getBlockCallback7(Ruby runtime, String method) {return getBlockCallback(runtime, 7, method);}
    public final CompiledBlockCallback getBlockCallback8(Ruby runtime, String method) {return getBlockCallback(runtime, 8, method);}
    public final CompiledBlockCallback getBlockCallback9(Ruby runtime, String method) {return getBlockCallback(runtime, 9, method);}

    public final RubySymbol getSymbol(Ruby runtime, int index, String name) {
        RubySymbol symbol = symbols[index];
        if (symbol == null) return symbols[index] = runtime.newSymbol(name);
        return symbol;
    }

    public static final int NUMBERED_SYMBOL_COUNT = 10;

    public final RubySymbol getSymbol0(Ruby runtime, String name) {return getSymbol(runtime, 0, name);}
    public final RubySymbol getSymbol1(Ruby runtime, String name) {return getSymbol(runtime, 1, name);}
    public final RubySymbol getSymbol2(Ruby runtime, String name) {return getSymbol(runtime, 2, name);}
    public final RubySymbol getSymbol3(Ruby runtime, String name) {return getSymbol(runtime, 3, name);}
    public final RubySymbol getSymbol4(Ruby runtime, String name) {return getSymbol(runtime, 4, name);}
    public final RubySymbol getSymbol5(Ruby runtime, String name) {return getSymbol(runtime, 5, name);}
    public final RubySymbol getSymbol6(Ruby runtime, String name) {return getSymbol(runtime, 6, name);}
    public final RubySymbol getSymbol7(Ruby runtime, String name) {return getSymbol(runtime, 7, name);}
    public final RubySymbol getSymbol8(Ruby runtime, String name) {return getSymbol(runtime, 8, name);}
    public final RubySymbol getSymbol9(Ruby runtime, String name) {return getSymbol(runtime, 9, name);}

    public final RubyFixnum getFixnum(Ruby runtime, int index, int value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) return fixnums[index] = RubyFixnum.newFixnum(runtime, value);
        return fixnum;
    }

    public static final int NUMBERED_FIXNUM_COUNT = 10;

    public final RubyFixnum getFixnum0(Ruby runtime, int value) {return getFixnum(runtime, 0, value);}
    public final RubyFixnum getFixnum1(Ruby runtime, int value) {return getFixnum(runtime, 1, value);}
    public final RubyFixnum getFixnum2(Ruby runtime, int value) {return getFixnum(runtime, 2, value);}
    public final RubyFixnum getFixnum3(Ruby runtime, int value) {return getFixnum(runtime, 3, value);}
    public final RubyFixnum getFixnum4(Ruby runtime, int value) {return getFixnum(runtime, 4, value);}
    public final RubyFixnum getFixnum5(Ruby runtime, int value) {return getFixnum(runtime, 5, value);}
    public final RubyFixnum getFixnum6(Ruby runtime, int value) {return getFixnum(runtime, 6, value);}
    public final RubyFixnum getFixnum7(Ruby runtime, int value) {return getFixnum(runtime, 7, value);}
    public final RubyFixnum getFixnum8(Ruby runtime, int value) {return getFixnum(runtime, 8, value);}
    public final RubyFixnum getFixnum9(Ruby runtime, int value) {return getFixnum(runtime, 9, value);}

    public final RubyFixnum getFixnum(Ruby runtime, int index, long value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) return fixnums[index] = RubyFixnum.newFixnum(runtime, value);
        return fixnum;
    }

    public final RubyRegexp getRegexp(Ruby runtime, int index, String pattern, int options) {
        RubyRegexp regexp = regexps[index];
        if (regexp == null) return regexps[index] = RubyRegexp.newRegexp(runtime, pattern, options);
        return regexp;
    }

    public static final int NUMBERED_REGEXP_COUNT = 10;

    public final RubyRegexp getRegexp0(Ruby runtime, String name, int options) {return getRegexp(runtime, 0, name, options);}
    public final RubyRegexp getRegexp1(Ruby runtime, String name, int options) {return getRegexp(runtime, 1, name, options);}
    public final RubyRegexp getRegexp2(Ruby runtime, String name, int options) {return getRegexp(runtime, 2, name, options);}
    public final RubyRegexp getRegexp3(Ruby runtime, String name, int options) {return getRegexp(runtime, 3, name, options);}
    public final RubyRegexp getRegexp4(Ruby runtime, String name, int options) {return getRegexp(runtime, 4, name, options);}
    public final RubyRegexp getRegexp5(Ruby runtime, String name, int options) {return getRegexp(runtime, 5, name, options);}
    public final RubyRegexp getRegexp6(Ruby runtime, String name, int options) {return getRegexp(runtime, 6, name, options);}
    public final RubyRegexp getRegexp7(Ruby runtime, String name, int options) {return getRegexp(runtime, 7, name, options);}
    public final RubyRegexp getRegexp8(Ruby runtime, String name, int options) {return getRegexp(runtime, 8, name, options);}
    public final RubyRegexp getRegexp9(Ruby runtime, String name, int options) {return getRegexp(runtime, 9, name, options);}

    public final BigInteger getBigInteger(Ruby runtime, int index, String pattern) {
        BigInteger bigint = bigIntegers[index];
        if (bigint == null) return bigIntegers[index] = new BigInteger(pattern, 16);
        return bigint;
    }

    public static final int NUMBERED_BIGINTEGER_COUNT = 10;

    public final BigInteger getBigInteger0(Ruby runtime, String name) {return getBigInteger(runtime, 0, name);}
    public final BigInteger getBigInteger1(Ruby runtime, String name) {return getBigInteger(runtime, 1, name);}
    public final BigInteger getBigInteger2(Ruby runtime, String name) {return getBigInteger(runtime, 2, name);}
    public final BigInteger getBigInteger3(Ruby runtime, String name) {return getBigInteger(runtime, 3, name);}
    public final BigInteger getBigInteger4(Ruby runtime, String name) {return getBigInteger(runtime, 4, name);}
    public final BigInteger getBigInteger5(Ruby runtime, String name) {return getBigInteger(runtime, 5, name);}
    public final BigInteger getBigInteger6(Ruby runtime, String name) {return getBigInteger(runtime, 6, name);}
    public final BigInteger getBigInteger7(Ruby runtime, String name) {return getBigInteger(runtime, 7, name);}
    public final BigInteger getBigInteger8(Ruby runtime, String name) {return getBigInteger(runtime, 8, name);}
    public final BigInteger getBigInteger9(Ruby runtime, String name) {return getBigInteger(runtime, 9, name);}

    public final void initScopes(int size) {
        scopes = new StaticScope[size];
    }

    public final void initCallSites(int size) {
        callSites = new CallSite[size];
    }

    public final void initBlockBodies(int size) {
        blockBodies = new BlockBody[size];
    }

    public final void initBlockCallbacks(int size) {
        blockCallbacks = new CompiledBlockCallback[size];
    }

    public final void initSymbols(int size) {
        symbols = new RubySymbol[size];
    }

    public final void initFixnums(int size) {
        fixnums = new RubyFixnum[size];
    }

    public final void initRegexps(int size) {
        regexps = new RubyRegexp[size];
    }

    public final void initBigIntegers(int size) {
        bigIntegers = new BigInteger[size];
    }

    public final void initConstants(int size) {
        constants = new IRubyObject[size];
        constantTargetHashes = new int[size];
        constantGenerations = new int[size];
        Arrays.fill(constantGenerations, -1);
        Arrays.fill(constantTargetHashes, -1);
    }

    public static CallSite[] setCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getCallSite(name);
        return callSites;
    }

    public static CallSite[] setFunctionalCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getFunctionalCallSite(name);
        return callSites;
    }

    public static CallSite[] setVariableCallSite(CallSite[] callSites, int index, String name) {
        callSites[index] = MethodIndex.getVariableCallSite(name);
        return callSites;
    }
    
    public final void setFilename(String filename) {
        this.filename = filename;
    }

    public final IRubyObject getConstant(ThreadContext context, String name, int index) {
        IRubyObject value = getValue(context, name, index);

        // We can callsite cache const_missing if we want
        return value != null ? value :
            context.getRubyClass().callMethod(context, "const_missing", context.getRuntime().fastNewSymbol(name));
    }

    public static final int NUMBERED_CONSTANT_COUNT = 10;

    public final IRubyObject getConstant0(ThreadContext context, String name) {return getConstant(context, name, 0);}
    public final IRubyObject getConstant1(ThreadContext context, String name) {return getConstant(context, name, 1);}
    public final IRubyObject getConstant2(ThreadContext context, String name) {return getConstant(context, name, 2);}
    public final IRubyObject getConstant3(ThreadContext context, String name) {return getConstant(context, name, 3);}
    public final IRubyObject getConstant4(ThreadContext context, String name) {return getConstant(context, name, 4);}
    public final IRubyObject getConstant5(ThreadContext context, String name) {return getConstant(context, name, 5);}
    public final IRubyObject getConstant6(ThreadContext context, String name) {return getConstant(context, name, 6);}
    public final IRubyObject getConstant7(ThreadContext context, String name) {return getConstant(context, name, 7);}
    public final IRubyObject getConstant8(ThreadContext context, String name) {return getConstant(context, name, 8);}
    public final IRubyObject getConstant9(ThreadContext context, String name) {return getConstant(context, name, 9);}

    public IRubyObject getValue(ThreadContext context, String name, int index) {
        IRubyObject value = constants[index]; // Store to temp so it does null out on us mid-stream

        return isCached(context, value, index) ? value : reCache(context, name, index);
    }

    private boolean isCached(ThreadContext context, IRubyObject value, int index) {
        return value != null && constantGenerations[index] == context.getRuntime().getConstantGeneration();
    }

    public IRubyObject reCache(ThreadContext context, String name, int index) {
        int newGeneration = context.getRuntime().getConstantGeneration();
        IRubyObject value = context.getConstant(name);

        constants[index] = value;

        if (value != null) constantGenerations[index] = newGeneration;
        
        return value;
    }

    public final IRubyObject getConstantFrom(RubyModule target, ThreadContext context, String name, int index) {
        IRubyObject value = getValueFrom(target, context, name, index);

        // We can callsite cache const_missing if we want
        return value != null ? value : target.fastGetConstantFromConstMissing(name);
    }

    public static final int NUMBERED_CONSTANTFROM_COUNT = 10;

    public final IRubyObject getConstantFrom0(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 0);}
    public final IRubyObject getConstantFrom1(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 1);}
    public final IRubyObject getConstantFrom2(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 2);}
    public final IRubyObject getConstantFrom3(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 3);}
    public final IRubyObject getConstantFrom4(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 4);}
    public final IRubyObject getConstantFrom5(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 5);}
    public final IRubyObject getConstantFrom6(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 6);}
    public final IRubyObject getConstantFrom7(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 7);}
    public final IRubyObject getConstantFrom8(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 8);}
    public final IRubyObject getConstantFrom9(RubyModule target, ThreadContext context, String name) {return getConstantFrom(target, context, name, 9);}

    public IRubyObject getValueFrom(RubyModule target, ThreadContext context, String name, int index) {
        IRubyObject value = constants[index]; // Store to temp so it does null out on us mid-stream

        return isCachedFrom(target, context, value, index) ? value : reCacheFrom(target, context, name, index);
    }

    private boolean isCachedFrom(RubyModule target, ThreadContext context, IRubyObject value, int index) {
        return
                value != null &&
                constantGenerations[index] == context.getRuntime().getConstantGeneration() &&
                constantTargetHashes[index] == target.hashCode();
    }

    public IRubyObject reCacheFrom(RubyModule target, ThreadContext context, String name, int index) {
        int newGeneration = context.getRuntime().getConstantGeneration();
        IRubyObject value = target.fastGetConstantFromNoConstMissing(name);

        constants[index] = value;

        if (value != null) {
            constantGenerations[index] = newGeneration;
            constantTargetHashes[index] = target.hashCode();
        }

        return value;
    }

    private BlockBody createBlockBody(ThreadContext context, int index, String descriptor) throws NumberFormatException {
        String[] firstSplit = descriptor.split(",");
        String[] secondSplit;

        if (firstSplit[2].length() == 0) {
            secondSplit = new String[0];
        } else {
            secondSplit = firstSplit[2].split(";");

            // FIXME: Big fat hack here, because scope names are expected to be interned strings by the parser
            for (int i = 0; i < secondSplit.length; i++) {
                secondSplit[i] = secondSplit[i].intern();
            }
        }

        BlockBody body = RuntimeHelpers.createCompiledBlockBody(
                context,
                this,
                firstSplit[0],
                Integer.parseInt(firstSplit[1]),
                secondSplit,
                Boolean.valueOf(firstSplit[3]),
                Integer.parseInt(firstSplit[4]),
                Boolean.valueOf(firstSplit[5]));
        return blockBodies[index] = body;
    }

    private CompiledBlockCallback createCompiledBlockCallback(Ruby runtime, int index, String method) {
        CompiledBlockCallback callback = RuntimeHelpers.createBlockCallback(runtime, this, method);
        return blockCallbacks[index] = callback;
    }

    public StaticScope[] scopes;
    public CallSite[] callSites;
    public BlockBody[] blockBodies;
    public CompiledBlockCallback[] blockCallbacks;
    public RubySymbol[] symbols;
    public RubyFixnum[] fixnums;
    public RubyRegexp[] regexps;
    public BigInteger[] bigIntegers;
    public String filename;
    public IRubyObject[] constants;
    public int[] constantGenerations;
    public int[] constantTargetHashes;
}
