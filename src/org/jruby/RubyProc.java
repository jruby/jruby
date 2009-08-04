/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.jruby.anno.JRubyMethod;
import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.JumpException;
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.java.MiniJava;
import org.jruby.parser.BlockStaticScope;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Block;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.marshal.DataType;

/**
 * @author  jpetersen
 */
@JRubyClass(name="Proc")
public class RubyProc extends RubyObject implements JumpTarget, DataType {
    private Block block = Block.NULL_BLOCK;
    private Block.Type type;
    private String file;
    private int line;

    public RubyProc(Ruby runtime, RubyClass rubyClass, Block.Type type) {
        super(runtime, rubyClass);
        
        this.type = type;
    }
    
    private static ObjectAllocator PROC_ALLOCATOR = new ObjectAllocator() {
        public IRubyObject allocate(Ruby runtime, RubyClass klass) {
            RubyProc instance = RubyProc.newProc(runtime, Block.Type.PROC);

            instance.setMetaClass(klass);

            return instance;
        }
    };

    public static RubyClass createProcClass(Ruby runtime) {
        RubyClass procClass = runtime.defineClass("Proc", runtime.getObject(), PROC_ALLOCATOR);
        runtime.setProc(procClass);
        
        procClass.defineAnnotatedMethods(RubyProc.class);
        
        return procClass;
    }

    public Block getBlock() {
        return block;
    }

    // Proc class

    public static RubyProc newProc(Ruby runtime, Block.Type type) {
        return new RubyProc(runtime, runtime.getProc(), type);
    }
    public static RubyProc newProc(Ruby runtime, Block block, Block.Type type) {
        RubyProc proc = new RubyProc(runtime, runtime.getProc(), type);
        proc.callInit(NULL_ARRAY, block);
        
        return proc;
    }
    
    /**
     * Create a new instance of a Proc object.  We override this method (from RubyClass)
     * since we need to deal with special case of Proc.new with no arguments or block arg.  In 
     * this case, we need to check previous frame for a block to consume.
     */
    @JRubyMethod(name = "new", rest = true, frame = true, meta = true)
    public static IRubyObject newInstance(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        // No passed in block, lets check next outer frame for one ('Proc.new')
        if (!block.isGiven()) {
            block = context.getPreviousFrame().getBlock();
        }
        
        if (block.isGiven() && block.getProcObject() != null) {
            return block.getProcObject();
        }
        
        IRubyObject obj = ((RubyClass) recv).allocate();
        
        obj.callMethod(context, "initialize", args, block);
        return obj;
    }
    
    @JRubyMethod(name = "initialize", frame = true, visibility = Visibility.PRIVATE)
    public IRubyObject initialize(ThreadContext context, Block procBlock) {
        if (!procBlock.isGiven()) {
            throw getRuntime().newArgumentError("tried to create Proc object without a block");
        }
        
        if (type == Block.Type.LAMBDA && procBlock == null) {
            // TODO: warn "tried to create Proc object without a block"
        }
        
        block = procBlock.cloneBlock();

        if (type == Block.Type.THREAD) {
            // modify the block with a new backref/lastline-grabbing scope
            StaticScope oldScope = block.getBody().getStaticScope();
            StaticScope newScope = new BlockStaticScope(oldScope.getEnclosingScope(), oldScope.getVariables());
            newScope.setBackrefLastlineScope(true);
            newScope.setPreviousCRefScope(oldScope.getPreviousCRefScope());
            newScope.setModule(oldScope.getModule());
            block.getBody().setStaticScope(newScope);
        }

        block.type = type;
        block.setProcObject(this);

        file = context.getFile();
        line = context.getLine();
        return this;
    }
    
    @JRubyMethod(name = "clone")
    @Override
    public IRubyObject rbClone() {
    	RubyProc newProc = new RubyProc(getRuntime(), getRuntime().getProc(), type);
    	newProc.block = getBlock();
    	newProc.file = file;
    	newProc.line = line;
    	// TODO: CLONE_SETUP here
    	return newProc;
    }

    @JRubyMethod(name = "dup")
    @Override
    public IRubyObject dup() {
        RubyProc newProc = new RubyProc(getRuntime(), getRuntime().getProc(), type);
        newProc.block = getBlock();
        newProc.file = file;
        newProc.line = line;
        return newProc;
    }
    
    @JRubyMethod(name = "==", required = 1)
    public IRubyObject op_equal(IRubyObject other) {
        if (!(other instanceof RubyProc)) return getRuntime().getFalse();
        
        if (this == other || this.block.equals(((RubyProc)other).block)) {
            return getRuntime().getTrue();
        }
        
        return getRuntime().getFalse();
    }
    
    @JRubyMethod(name = "to_s")
    @Override
    public IRubyObject to_s() {
        return RubyString.newString(getRuntime(), 
                "#<Proc:0x" + Integer.toString(block.hashCode(), 16) + "@" + 
                file + ":" + (line + 1) + ">");
    }

    @JRubyMethod(name = "binding")
    public IRubyObject binding() {
        return getRuntime().newBinding(block.getBinding());
    }

    @JRubyMethod(name = {"call", "[]"}, rest = true, frame = true, compat = CompatVersion.RUBY1_8)
    public IRubyObject call(ThreadContext context, IRubyObject[] args) {
        return call(context, args, null, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = {"call", "[]"}, rest = true, frame = true, compat = CompatVersion.RUBY1_9)
    public IRubyObject call19(ThreadContext context, IRubyObject[] args, Block block) {
        return call(context, args, null, block);
    }

    public IRubyObject call(ThreadContext context, IRubyObject[] args, IRubyObject self, Block passedBlock) {
        assert args != null;
        
        Block newBlock = block.cloneBlock();
        JumpTarget jumpTarget = newBlock.getBinding().getFrame().getJumpTarget();
        
        try {
            if (self != null) newBlock.getBinding().setSelf(self);
            
            return newBlock.call(context, args, passedBlock);
        } catch (JumpException.BreakJump bj) {
            return handleBreakJump(getRuntime(), newBlock, bj, jumpTarget);
        } catch (JumpException.ReturnJump rj) {
            return handleReturnJump(getRuntime(), rj, jumpTarget);
        } catch (JumpException.RetryJump rj) {
            return handleRetryJump(getRuntime(), rj);
        }
    }

    private IRubyObject handleBreakJump(Ruby runtime, Block newBlock, JumpException.BreakJump bj, JumpTarget jumpTarget) {
        switch(newBlock.type) {
        case LAMBDA: if (bj.getTarget() == jumpTarget) {
            return (IRubyObject) bj.getValue();
        } else {
            throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "unexpected break");
        }
        case PROC:
            if (newBlock.isEscaped()) {
                throw runtime.newLocalJumpError(RubyLocalJumpError.Reason.BREAK, (IRubyObject)bj.getValue(), "break from proc-closure");
            } else {
                throw bj;
            }
        default: throw bj;
        }
    }

    private IRubyObject handleReturnJump(Ruby runtime, JumpException.ReturnJump rj, JumpTarget jumpTarget) {
        Object target = rj.getTarget();

        if (target == jumpTarget && block.type == Block.Type.LAMBDA) return (IRubyObject) rj.getValue();

        if (type == Block.Type.THREAD) {
            throw runtime.newThreadError("return can't jump across threads");
        }
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
    
    @Override
    public IRubyObject as(Class asClass) {
        final Ruby ruby = getRuntime();
        if (!asClass.isInterface()) {
            throw ruby.newTypeError(asClass.getCanonicalName() + " is not an interface");
        }

        return MiniJava.javaToRuby(ruby, Proxy.newProxyInstance(Ruby.getClassLoader(), new Class[] {asClass}, new InvocationHandler() {
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                IRubyObject[] rubyArgs = new IRubyObject[args.length + 1];
                rubyArgs[0] = RubySymbol.newSymbol(ruby, method.getName());
                for (int i = 1; i < rubyArgs.length; i++) {
                    rubyArgs[i] = MiniJava.javaToRuby(ruby, args[i - 1]);
                }
                return MiniJava.rubyToJava(call(ruby.getCurrentContext(), rubyArgs));
            }
        }));
    }
}
