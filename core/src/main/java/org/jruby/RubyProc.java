/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
import org.jruby.ir.runtime.IRRuntimeHelpers;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.BlockBody;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.Signature;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

import static org.jruby.runtime.Helpers.arrayOf;
import static org.jruby.util.RubyStringBuilder.types;

/**
 * @author  jpetersen
 */
@JRubyClass(name="Proc")
public class RubyProc extends RubyObject implements DataType {
    private Block block = Block.NULL_BLOCK;
    private final Block.Type type;
    private String file = null;
    private int line = -1;

    protected RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type) {
        super(runtime, rubyClass);

        this.type = type;
    }

    @Deprecated
    protected RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type, ISourcePosition sourcePosition) {
        this(runtime, rubyClass, type, sourcePosition.getFile(), sourcePosition.getLine());
    }

    protected RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type, String file, int line) {
        this(runtime, rubyClass, type);

        this.file = file;
        this.line = line;
    }


    public RubyProc(Ruby runtime, RubyClass rubyClass, Block block, String file, int line) {
        this(runtime, rubyClass, block.type, file, line);
        this.block = block;
    }

    public static RubyClass createProcClass(Ruby runtime) {
        RubyClass procClass = runtime.defineClass("Proc", runtime.getObject(), ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);

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
        // The three valid types of execution here are PROC/LAMBDA/THREAD.  NORMAL should not normally
        // be passed but when it is we just assume it will be a PROC.
        if (type == Block.Type.NORMAL) type = Block.Type.PROC;

        RubyProc proc = new RubyProc(runtime, runtime.getProc(), type);
        proc.setup(block);

        return proc;
    }

    @Deprecated
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
        if (block.isGiven() && block.getProcObject() != null && block.getProcObject().metaClass == recv) {
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
            block = new Block(procBlock.getBody(), newBinding, type);

            // Mark as escaped, so non-local flow errors immediately
            block.escape();

            // modify the block with a new backref/lastline-grabbing scope
            StaticScope oldScope = block.getBody().getStaticScope();
            StaticScope newScope = oldScope.duplicate();
            block.getBody().setStaticScope(newScope);
        } else {
            // just use as is unless type differs
            if (type != procBlock.type) {
                block = procBlock.cloneBlockAsType(type);
            } else {
                block = procBlock;
            }
        }

        // force file/line info into the new block's binding
        block.getBinding().setFile(block.getBody().getFile());
        block.getBinding().setLine(block.getBody().getLine());

        block.setProcObject(this);

        // pre-request dummy scope to avoid clone overhead in lightweight blocks
        block.getBinding().getDummyScope(block.getBody().getStaticScope());
    }

    @JRubyMethod(name = "clone")
    @Override
    public IRubyObject rbClone() {
    	return newProc(getRuntime(), block, type, file, line);
    }

    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        return newProc(getRuntime(), block, type, file, line);
    }

    @Override
    @JRubyMethod(name = "to_s", alias = "inspect")
    public IRubyObject to_s() {
        Ruby runtime = getRuntime();
        RubyString string = runtime.newString("#<");

        string.append(types(runtime, type()));
        string.catString(":0x" + Integer.toString(System.identityHashCode(block), 16));

        String file = block.getBody().getFile();
        if (file != null) string.catString("@" + file + ":" + (block.getBody().getLine() + 1));

        if (isLambda()) string.catString(" (lambda)");
        string.catString(">");

        if (isTaint()) string.setTaint(true);

        return string;
    }

    @JRubyMethod(name = "binding")
    public IRubyObject binding() {
        return getRuntime().newBinding(block.getBinding());
    }

    /**
     * For non-lambdas transforms the given arguments appropriately for the given arity (i.e. trimming to one arg for fixed
     * arity of one, etc.)
     *
     * Note: nothing should be calling this any more.
     */
    @Deprecated
    public static IRubyObject[] prepareArgs(ThreadContext context, Block.Type type, BlockBody blockBody, IRubyObject[] args) {
        if (type == Block.Type.LAMBDA) return args;

        int arityValue = blockBody.getSignature().arityValue();
        if (args.length == 1 && (arityValue < -1 || arityValue > 1)) args = IRRuntimeHelpers.toAry(context, args);
        return args;
    }

    private static IRubyObject[] checkArityForLambda(ThreadContext context, Block.Type type, BlockBody blockBody, IRubyObject... args) {
        if (type == Block.Type.LAMBDA) {
            blockBody.getSignature().checkArity(context.runtime, args);
        }

        return args;
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, rest = true, omit = true)
    public final IRubyObject call(ThreadContext context, IRubyObject[] args, Block blockCallArg) {
        return block.call(context, args, blockCallArg);
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, omit = true)
    public final IRubyObject call(ThreadContext context, Block blockCallArg) {
        return block.call(
                context,
                checkArityForLambda(context, type, block.getBody(), NULL_ARRAY),
                blockCallArg);
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, omit = true)
    public final IRubyObject call(ThreadContext context, IRubyObject arg0, Block blockCallArg) {
        return block.call(context, new IRubyObject[] { arg0 }, blockCallArg);
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, omit = true)
    public final IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, Block blockCallArg) {
        return block.call(
                context,
                checkArityForLambda(context, type, block.getBody(), arg0, arg1),
                blockCallArg);
    }

    @JRubyMethod(name = {"call", "[]", "yield", "==="}, omit = true)
    public final IRubyObject call(ThreadContext context, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block blockCallArg) {
        return block.call(
                context,
                checkArityForLambda(context, type, block.getBody(), arg0, arg1, arg2),
                blockCallArg);
    }

    public final IRubyObject call(ThreadContext context, IRubyObject arg) {
        return block.call(context, arg);
    }

    public final IRubyObject call(ThreadContext context, IRubyObject... args) {
        return block.call(context, args);
    }

    public final IRubyObject call(ThreadContext context, IRubyObject[] args, IRubyObject self, RubyModule sourceModule, Block passedBlock) {
        assert args != null;

        Block newBlock;

        // bind to new self and source module, if given
        if (self == null || sourceModule == null) {
            newBlock = block;
        } else {
            newBlock = block.cloneBlockAndFrame();
            if (self != null) {
                newBlock.getBinding().setSelf(self);
            }
            if (sourceModule != null) {
                newBlock.getFrame().setKlazz(sourceModule);
            }
        }

        return newBlock.call(context, args, passedBlock);
    }

    @JRubyMethod(name = "arity")
    public RubyFixnum arity() {
        Signature signature = block.getSignature();

        if (block.type == Block.Type.LAMBDA) return getRuntime().newFixnum(signature.arityValue());

        // FIXME: Consider min/max like MRI here instead of required + kwarg count.
        return getRuntime().newFixnum(signature.hasRest() ? signature.arityValue() : signature.required() + signature.getRequiredKeywordForArityCount());
    }

    @JRubyMethod(name = "to_proc")
    public RubyProc to_proc() {
    	return this;
    }

    @JRubyMethod
    public IRubyObject source_location(ThreadContext context) {
        Ruby runtime = context.runtime;
        if (file != null) {
            return runtime.newArray(runtime.newString(file), runtime.newFixnum(line + 1 /*zero-based*/));
        }

        if (block != null) {
            Binding binding = block.getBinding();

            // block+binding may exist for a core method, which will have a null filename
            if (binding.getFile() != null) {
                return runtime.newArray(
                        runtime.newString(binding.getFile()),
                        runtime.newFixnum(binding.getLine() + 1 /*zero-based*/));
            }
        }

        return context.nil;
    }

    @JRubyMethod
    public IRubyObject parameters(ThreadContext context) {
        BlockBody body = this.getBlock().getBody();

        return Helpers.argumentDescriptorsToParameters(context.runtime,
                body.getArgumentDescriptors(), isLambda());
    }

    @JRubyMethod(name = "lambda?")
    public IRubyObject lambda_p(ThreadContext context) {
        return RubyBoolean.newBoolean(context, isLambda());
    }

    private boolean isLambda() {
        return type.equals(Block.Type.LAMBDA);
    }

    //private boolean isProc() {
    //    return type.equals(Block.Type.PROC);
    //}

    private boolean isThread() {
        return type.equals(Block.Type.THREAD);
    }

    @Deprecated
    public final IRubyObject call19(ThreadContext context, IRubyObject[] args, Block block) {
        return call(context, args, block);
    }

    @Deprecated
    public IRubyObject to_s19() {
        return to_s();
    }

    @Deprecated
    public final IRubyObject call(ThreadContext context, IRubyObject[] args, IRubyObject self, Block passedBlock) {
        return block.call(context, args, passedBlock);
    }

}
