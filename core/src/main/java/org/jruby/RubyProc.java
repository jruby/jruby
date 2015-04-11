/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Chad Fowler <chadfowler@chadfowler.com>
 * Copyright (C) 2001 Alan Moore <alan_moore@gmx.net>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2007 Miguel Covarrubias <mlcovarrubias@gmail.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.lexer.yacc.SimpleSourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.CompiledIRBlockBody;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.IRBlockBody;
import org.jruby.runtime.InterpretedIRBlockBody;
import org.jruby.runtime.MethodBlock;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

import java.util.Arrays;

/**
 * @author  jpetersen
 */
@JRubyClass(name="Proc")
public class RubyProc extends RubyObject implements DataType {
    private Block block = Block.NULL_BLOCK;
    private Block.Type type;
    private ISourcePosition sourcePosition;

    protected RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type) {
        super(runtime, rubyClass);
        
        this.type = type;
    }

    protected RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type, ISourcePosition sourcePosition) {
        this(runtime, rubyClass, type);
        this.sourcePosition = sourcePosition;
    }

    protected RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type, String file, int line) {
        this(runtime, rubyClass, type, new SimpleSourcePosition(file, line));
    }

    public static RubyClass createProcClass(Ruby runtime) {
        RubyClass procClass = runtime.defineClass("Proc", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setProc(procClass);

        procClass.setClassIndex(ClassIndex.PROC);
        procClass.setReifiedClass(RubyProc.class);
        
        procClass.defineAnnotatedMethods(RubyProc.class);

        return procClass;
    }

    public Block getBlock() {
        return block;
    }

    // Proc class

    @Deprecated
    public static RubyProc newProc(Ruby runtime, Block.Type type) {
        throw runtime.newRuntimeError("deprecated RubyProc.newProc with no block; do not use");
    }

    public static RubyProc newProc(Ruby runtime, Block block, Block.Type type) {
        return newProc(runtime, block, type, null);
    }

    public static RubyProc newProc(Ruby runtime, Block block, Block.Type type, ISourcePosition sourcePosition) {
        RubyProc proc = new RubyProc(runtime, runtime.getProc(), type, sourcePosition);
        proc.setup(block);

        return proc;
    }

    public static RubyProc newProc(Ruby runtime, Block block, Block.Type type, String file, int line) {
        RubyProc proc = new RubyProc(runtime, runtime.getProc(), type, file, line);
        proc.setup(block);

        return proc;
    }
    
    /**
     * Create a new instance of a Proc object.  We override this method (from RubyClass)
     * since we need to deal with special case of Proc.new with no arguments or block arg.  In 
     * this case, we need to check previous frame for a block to consume.
     */
    @JRubyMethod(name = "new", rest = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // No passed in block, lets check next outer frame for one ('Proc.new')
        if (!block.isGiven()) block = context.getCurrentFrame().getBlock();

        // This metaclass == recv check seems gross, but MRI seems to do the same:
        // if (!proc && ruby_block->block_obj && CLASS_OF(ruby_block->block_obj) == klass) {
        if (block.isGiven() && block.getProcObject() != null && block.getProcObject().getMetaClass() == recv) {
            return block.getProcObject();
        }
        
        RubyProc obj = new RubyProc(context.runtime, (RubyClass)recv, Block.Type.PROC);
        obj.setup(block);
        
        obj.callMethod(context, "initialize", args, block);
        return obj;
    }
    
    private void setup(Block procBlock) {
        if (!procBlock.isGiven()) {
            throw getRuntime().newArgumentError("tried to create Proc object without a block");
        }
        
        if (isLambda()) {
            // TODO: warn "tried to create Proc object without a block"
        }

        if (isThread()) {
            // binding for incoming proc must not share frame
            Binding oldBinding = procBlock.getBinding();
            Binding newBinding = new Binding(
                    oldBinding.getSelf(),
                    oldBinding.getFrame().duplicate(),
                    oldBinding.getVisibility(),
                    oldBinding.getDynamicScope(),
                    oldBinding.getMethod(),
                    oldBinding.getFile(),
                    oldBinding.getLine());
            block = new Block(procBlock.getBody(), newBinding);

            // modify the block with a new backref/lastline-grabbing scope
            StaticScope oldScope = block.getBody().getStaticScope();
            StaticScope newScope = oldScope.duplicate();
            block.getBody().setStaticScope(newScope);
        } else {
            // just use as is
            block = procBlock;
        }

        // force file/line info into the new block's binding
        block.getBinding().setFile(block.getBody().getFile());
        block.getBinding().setLine(block.getBody().getLine());

        block.type = type;
        block.setProcObject(this);
        
        // pre-request dummy scope to avoid clone overhead in lightweight blocks
        block.getBinding().getDummyScope(block.getBody().getStaticScope());
    }
    
    @JRubyMethod(name = "clone")
    @Override
    public IRubyObject rbClone() {
    	RubyProc newProc = newProc(getRuntime(), block, type, sourcePosition);
    	// TODO: CLONE_SETUP here
    	return newProc;
    }

    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        return newProc(getRuntime(), block, type, sourcePosition);
    }
    
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        return getRuntime().newBoolean(other instanceof RubyProc &&
                (this == other || this.block.equals(((RubyProc)other).block)));
    }
    
    @Override
    public IRubyObject to_s() {
        return to_s19();
    }

    @JRubyMethod(name = "to_s", alias = "inspect")
    public IRubyObject to_s19() {
        StringBuilder sb = new StringBuilder("#<Proc:0x" + Integer.toString(block.hashCode(), 16) + "@" +
                block.getBody().getFile() + ":" + (block.getBody().getLine() + 1));
        if (isLambda()) sb.append(" (lambda)");
        sb.append(">");
        
        return RubyString.newString(getRuntime(), sb.toString());
    }

    @JRubyMethod(name = "binding")
    public IRubyObject binding() {
        return getRuntime().newBinding(block.getBinding());
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        return call19(context, args, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return call(context, args, null, Block.NULL_BLOCK);
    }

    /**
     * For Type.LAMBDA, ensures that the args have the correct arity.
     *
     * For others, transforms the given arguments appropriately for the given arity (i.e. trimming to one arg for fixed
     * arity of one, etc.)
     */
    public static IRubyObject[] prepareArgs(ThreadContext context, Block.Type type, BlockBody blockBody, IRubyObject[] args) {
        // FIXME: Arity marked for death
        Arity arity = blockBody.arity();
        if (arity == null) return args;

        if (args == null) return IRubyObject.NULL_ARRAY;

        if (type == Block.Type.LAMBDA) {
            if (blockBody instanceof IRBlockBody) {
                ((IRBlockBody) blockBody).getSignature().checkArity(context.runtime, args);
            } else {
                arity.checkArity(context.runtime, args.length);
            }
            return args;
        }

        boolean isFixed = arity.isFixed();
        int required = arity.required();
        int actual = args.length;
        boolean restKwargs = blockBody instanceof IRBlockBody && ((IRBlockBody) blockBody).getSignature().kwargs();

        // FIXME: This is a hot mess.  restkwargs factors into destructing a single element array as well.  I just weaved it into this logic.
        // for procs and blocks, single array passed to multi-arg must be spread
        if ((arity != Arity.ONE_ARGUMENT &&  required != 0 && (isFixed || arity != Arity.OPTIONAL) || restKwargs) &&
                actual == 1 && args[0].respondsTo("to_ary")) {
            args = args[0].convertToArray().toJavaArray();
            actual = args.length;
        }

        // FIXME: NOTE IN THE BLOCKCAPALYPSE: I think we only care if there is any kwargs syntax at all and if so it is +1
        // argument.  This ended up more complex because required() on signature adds +1 is required kwargs.  I suspect
        // required() is used for two purposes and the +1 might be useful in some other way so I made it all work and
        // after this we should clean this up (IRBlockBody and BlockBody are also messing with args[] so that should
        // be part of this cleanup.

        // We add one to our fill and adjust number of incoming args code when there are kwargs.  We subtract one
        // if it happens to be requiredkwargs since required gets a +1.  This is horrible :)
        int needsKwargs = blockBody instanceof IRBlockBody && ((IRBlockBody) blockBody).getSignature().kwargs() ?
                1 - ((IRBlockBody) blockBody).getSignature().getRequiredKeywordCount() : 0;

        // fixed arity > 0 with mismatch needs a new args array
        if (isFixed && required > 0 && required+needsKwargs != actual) {
            IRubyObject[] newArgs = Arrays.copyOf(args, required+needsKwargs);


            if (required > actual) {                      // Not enough required args pad.
                Helpers.fillNil(newArgs, actual, required, context.runtime);
                // ENEBO: what if we need kwargs here?
            } else if (needsKwargs != 0) {
                if (args.length < required+needsKwargs) { // Not enough args and we need an empty {} for kwargs processing.
                    newArgs[newArgs.length - 1] = RubyHash.newHash(context.runtime);
                } else {                                  // We have more args than we need and kwargs is always the last arg.
                    newArgs[newArgs.length - 1] = args[args.length - 1];
                }
            }

            args = newArgs;
        }

        return args;
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, rest = true)
    public IRubyObject call19(ThreadContext context, IRubyObject[] args, Block blockCallArg) {
        IRubyObject[] preppedArgs = prepareArgs(context, type, block.getBody(), args);

        return call(context, preppedArgs, null, blockCallArg);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, IRubyObject self, Block passedBlock) {
        assert args != null;
        
        Block newBlock;
        
        // bind to new self, if given
        if (self == null) {
            newBlock = block;
        } else {
            newBlock = block.cloneBlockAndFrame();
            newBlock.getBinding().setSelf(self);
        }
        
        return newBlock.call(context, args, passedBlock);
    }

    @JRubyMethod(name = "arity")
    public RubyFixnum arity() {
        return getRuntime().newFixnum(block.arity().getValue());
    }
    
    @JRubyMethod(name = "to_proc")
    public RubyProc to_proc() {
    	return this;
    }

    @JRubyMethod
    public IRubyObject source_location(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (sourcePosition != null) {
            return runtime.newArray(runtime.newString(sourcePosition.getFile()),
                    runtime.newFixnum(sourcePosition.getLine() + 1 /*zero-based*/));
        } else if (block != null) {
            Binding binding = block.getBinding();
            return runtime.newArray(runtime.newString(binding.getFile()),
                    runtime.newFixnum(binding.getLine() + 1 /*zero-based*/));
        }

        return runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject parameters(ThreadContext context) {
        BlockBody body = this.getBlock().getBody();

        if (body instanceof MethodBlock) return ((MethodBlock) body).getMethod().parameters(context);

        return Helpers.parameterListToParameters(context.runtime,
                body.getParameterList(), isLambda());
    }

    @JRubyMethod(name = "lambda?")
    public IRubyObject lambda_p(ThreadContext context) {
        return context.runtime.newBoolean(isLambda());
    }

    private boolean isLambda() {
        return type.equals(Block.Type.LAMBDA);
    }
    
    private boolean isProc() {
        return type.equals(Block.Type.PROC);
    }

    private boolean isThread() {
        return type.equals(Block.Type.THREAD);
    }

}
