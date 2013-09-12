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
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.MethodBlock;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

import java.util.Arrays;

import static org.jruby.CompatVersion.RUBY1_8;
import static org.jruby.CompatVersion.RUBY1_9;

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

    public static RubyClass createProcClass(Ruby runtime) {
        RubyClass procClass = runtime.defineClass("Proc", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        runtime.setProc(procClass);

        procClass.index = ClassIndex.PROC;
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
        
        if (isLambda() && procBlock == null) {
            // TODO: warn "tried to create Proc object without a block"
        }

        if (isThread()) {
            // binding for incoming proc must not share frame
            Binding oldBinding = procBlock.getBinding();
            Binding newBinding = new Binding(
                    oldBinding.getSelf(),
                    oldBinding.getFrame().duplicate(),
                    oldBinding.getVisibility(),
                    oldBinding.getKlass(), 
                    oldBinding.getDynamicScope(),
                    oldBinding.getBacktrace().clone());
            block = new Block(procBlock.getBody(), newBinding);

            // modify the block with a new backref/lastline-grabbing scope
            StaticScope oldScope = block.getBody().getStaticScope();
            StaticScope newScope = getRuntime().getStaticScopeFactory().newBlockScope(oldScope.getEnclosingScope(), oldScope.getVariables());
            newScope.setPreviousCRefScope(oldScope.getPreviousCRefScope());
            newScope.setModule(oldScope.getModule());
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
    
    @JRubyMethod(name = "to_s", compat = RUBY1_8)
    @Override
    public IRubyObject to_s() {
        return RubyString.newString(
                getRuntime(),"#<Proc:0x" + Integer.toString(block.hashCode(), 16) + "@" +
                block.getBody().getFile() + ":" + (block.getBody().getLine() + 1) + ">");
    }

    @JRubyMethod(name = "to_s", compat = RUBY1_9)
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

    @JRubyMethod(name = {"call", "[]"}, rest = true, compat = RUBY1_8)
    public IRubyObject call(ThreadContext context, IRubyObject[] args, Block block) {
        return call(context, args, null, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return call(context, args, null, Block.NULL_BLOCK);
    }

    /**
     * Transforms the given arguments appropriately for the given arity (i.e. trimming to one arg for fixed
     * arity of one, etc.)
     */
    public static IRubyObject[] prepareProcArgs(ThreadContext context, Arity arity, IRubyObject[] args) {
        boolean isFixed = arity.isFixed();
        int required = arity.required();
        int actual = args.length;
        
        // for procs and blocks, single array passed to multi-arg must be spread
        if (arity != Arity.ONE_ARGUMENT &&  required != 0 && 
                (isFixed || arity != Arity.OPTIONAL) &&
                actual == 1 && args[0].respondsTo("to_ary")) {
            args = args[0].convertToArray().toJavaArray();
            actual = args.length;
        }
        
        // fixed arity > 0 with mismatch needs a new args array
        if (isFixed && required > 0 && required != actual) {
            
            IRubyObject[] newArgs = Arrays.copyOf(args, required);
            
            // pad with nil
            if (required > actual) {
                Helpers.fillNil(newArgs, actual, required, context.runtime);
            }
            
            args = newArgs;
        }

        return args;
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, rest = true, compat = RUBY1_9)
    public IRubyObject call19(ThreadContext context, IRubyObject[] args, Block blockCallArg) {
        if (isLambda()) {
            block.arity().checkArity(context.runtime, args.length);
        }
        if (isProc()) args = prepareProcArgs(context, block.arity(), args);

        return call(context, args, null, blockCallArg);
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
        
        int jumpTarget = newBlock.getBinding().getFrame().getJumpTarget();
        
        try {
            return newBlock.call(context, args, passedBlock);
            } catch (NullPointerException npe) {
                npe.printStackTrace();
                throw npe;
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(getRuntime(), newBlock, bj, jumpTarget);
        } catch (JumpException.ReturnJump rj) {
            return handleReturnJump(context, rj, jumpTarget);
        } catch (JumpException.RetryJump rj) {
            return handleRetryJump(getRuntime(), rj);
        }
    }

    private IRubyObject handleBreakJump(Ruby runtime, Block newBlock, JumpException.BreakJump bj, int jumpTarget) {
        switch(newBlock.type) {
            case LAMBDA: 
                if (bj.getTarget() == jumpTarget) return (IRubyObject) bj.getValue();
                
                throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
            case PROC:
                if (newBlock.isEscaped()) throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "break from proc-closure");
        }
        
        throw bj;
    }

    private IRubyObject handleReturnJump(ThreadContext context, JumpException.ReturnJump rj, int jumpTarget) {
        int target = rj.getTarget();

        // lambda always just returns the value
        if (target == jumpTarget && isLambda()) return (IRubyObject) rj.getValue();

        // returns can't propagate out of threads. rethrow to let thread handle it
        if (isThread()) throw rj;

        // If the block-receiving method is not still active and the original
        // enclosing frame is no longer on the stack, it's a bad return.
        // FIXME: this is not very efficient for cases where it won't error
        if (target == jumpTarget && !context.isJumpTargetAlive(target, 0)) {
            throw context.runtime.newLocalJumpError(RubyLocalJumpError.Reason.RETURN,
                    (IRubyObject) rj.getValue(), "unexpected return");
        }

        // otherwise, let it propagate
        throw rj;
    }

    private IRubyObject handleRetryJump(Ruby runtime, JumpException.RetryJump rj) {
        throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.RETRY, (IRubyObject)rj.getValue(), "retry not supported outside rescue");
    }

    @JRubyMethod(name = "arity")
    public RubyFixnum arity() {
        return getRuntime().newFixnum(block.arity().getValue());
    }
    
    @JRubyMethod(name = "to_proc")
    public RubyProc to_proc() {
    	return this;
    }

    @JRubyMethod(name = "source_location", compat = RUBY1_9)
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

    @JRubyMethod(name = "parameters", compat = RUBY1_9)
    public IRubyObject parameters(ThreadContext context) {
        BlockBody body = this.getBlock().getBody();

        if (body instanceof MethodBlock) return ((MethodBlock) body).getMethod().parameters(context);

        return Helpers.parameterListToParameters(context.runtime,
                body.getParameterList(), isLambda());
    }

    @JRubyMethod(name = "lambda?", compat = RUBY1_9)
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
