/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.ast.executable;

import java.util.Arrays;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyRegexp;
import org.jruby.RubySymbol;
import org.jruby.javasupport.util.RuntimeHelpers;
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

    public final CallSite getCallSite(int index) {
        return callSites[index];
    }

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

    public final CompiledBlockCallback getBlockCallback(Ruby runtime, int index, String method) {
        CompiledBlockCallback callback = blockCallbacks[index];

        if (callback == null) {
            return createCompiledBlockCallback(runtime, index, method);
        }

        return callback;
    }

    public final RubySymbol getSymbol(Ruby runtime, int index, String name) {
        RubySymbol symbol = symbols[index];
        if (symbol == null) return symbols[index] = runtime.newSymbol(name);
        return symbol;
    }

    public final RubyFixnum getFixnum(Ruby runtime, int index, int value) {
        RubyFixnum fixnum = fixnums[index];
        if (fixnum == null) return fixnums[index] = RubyFixnum.newFixnum(runtime, value);
        return fixnum;
    }

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

    public final void initConstants(int size) {
        constants = new IRubyObject[size];
        constantGenerations = new int[size];
        Arrays.fill(constantGenerations, -1);
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

    public CallSite[] callSites;
    public BlockBody[] blockBodies;
    public CompiledBlockCallback[] blockCallbacks;
    public RubySymbol[] symbols;
    public RubyFixnum[] fixnums;
    public RubyRegexp[] regexps;
    public String filename;
    public IRubyObject[] constants;
    public int[] constantGenerations;
}
