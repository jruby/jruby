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
 * Copyright (C) 2008 Thomas E Enebo <enebo@acm.org>
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

package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.ast.ArgsNode;
import org.jruby.ast.Node;
import org.jruby.compiler.ASTInspector;
import org.jruby.evaluator.ASTInterpreter;
import org.jruby.exceptions.JumpException;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.PositionAware;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class InterpretedMethod extends DynamicMethod implements MethodArgs, PositionAware {
    private StaticScope staticScope;
    private Node body;
    private ArgsNode argsNode;
    private ISourcePosition position;
    private String file;
    private int line;
    private boolean needsScope;

    public InterpretedMethod(RubyModule implementationClass, StaticScope staticScope, Node body,
            String name, ArgsNode argsNode, Visibility visibility, ISourcePosition position) {
        super(implementationClass, visibility, CallConfiguration.FrameFullScopeFull, name);
        this.body = body;
        this.staticScope = staticScope;
        this.argsNode = argsNode;
        this.position = position;
        
        // we get these out ahead of time
        this.file = position.getFile();
        this.line = position.getLine();

        ASTInspector inspector = new ASTInspector();
        inspector.inspect(body);
        inspector.inspect(argsNode);

        // This optimization is temporarily disabled because of the complications
        // arising from moving backref/lastline into scope and not being able
        // to accurately detect that situation.
//        if (inspector.hasClosure() || inspector.hasScopeAwareMethods() || staticScope.getNumberOfVariables() != 0) {
//            // must have scope
            needsScope = true;
//        } else {
//            needsScope = false;
//        }
		
        assert argsNode != null;
    }
    
    public Node getBodyNode() {
        return body;
    }
    
    public ArgsNode getArgsNode() {
        return argsNode;
    }
    
    public StaticScope getStaticScope() {
        return staticScope;
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block) {
        assert args != null;
        
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, block, runtime);
            argsNode.checkArgCount(runtime, name, args.length);
            argsNode.prepare(context, runtime, self, args, block);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, block, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args) {
        return call(context, self, clazz, name, args, Block.NULL_BLOCK);
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, Block.NULL_BLOCK, runtime);
            argsNode.checkArgCount(runtime, name, 0);
            argsNode.prepare(context, runtime, self, Block.NULL_BLOCK);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, Block.NULL_BLOCK, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, Block block) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, block, runtime);
            argsNode.checkArgCount(runtime, name, 0);
            argsNode.prepare(context, runtime, self, block);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, block, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, Block.NULL_BLOCK, runtime);
            argsNode.checkArgCount(runtime, name, 1);
            argsNode.prepare(context, runtime, self, arg0, Block.NULL_BLOCK);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, Block.NULL_BLOCK, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, Block block) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, block, runtime);
            argsNode.checkArgCount(runtime, name, 1);
            argsNode.prepare(context, runtime, self, arg0, block);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, block, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, Block.NULL_BLOCK, runtime);
            argsNode.checkArgCount(runtime, name, 2);
            argsNode.prepare(context, runtime, self, arg0, arg1, Block.NULL_BLOCK);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, Block.NULL_BLOCK, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, Block block) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, block, runtime);
            argsNode.checkArgCount(runtime, name, 2);
            argsNode.prepare(context, runtime, self, arg0, arg1, block);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, block, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }
    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, Block.NULL_BLOCK, runtime);
            argsNode.checkArgCount(runtime, name, 3);
            argsNode.prepare(context, runtime, self, arg0, arg1, arg2, Block.NULL_BLOCK);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, Block.NULL_BLOCK, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }

    @Override
    public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject arg0, IRubyObject arg1, IRubyObject arg2, Block block) {
        Ruby runtime = context.getRuntime();
        int callNumber = context.callNumber;

        try {
            pre(context, name, self, block, runtime);
            argsNode.checkArgCount(runtime, name, 3);
            argsNode.prepare(context, runtime, self, arg0, arg1, arg2, block);

            return ASTInterpreter.INTERPRET_METHOD(runtime, context, file, line, getImplementationClass(), body, name, self, block, isTraceable());
        } catch (JumpException.ReturnJump rj) {
            return handleReturn(context, rj, callNumber);
        } catch (JumpException.RedoJump rj) {
            return handleRedo(runtime);
        } catch (JumpException.BreakJump bj) {
            return handleBreak(context, runtime, bj, callNumber);
        } finally {
            post(runtime, context, name);
        }
    }


    protected void pre(ThreadContext context, String name, IRubyObject self, Block block, Ruby runtime) {
        if (needsScope) {
            context.preMethodFrameAndScope(getImplementationClass(), name, self, block, staticScope);
        } else {
            context.preMethodFrameAndDummyScope(getImplementationClass(), name, self, block, staticScope);
        }
    }

    protected void post(Ruby runtime, ThreadContext context, String name) {
        context.postMethodFrameAndScope();
    }

    protected boolean isTraceable() {
        return false;
    }

    public ISourcePosition getPosition() {
        return position;
    }

    public String getFile() {
        return position.getFile();
    }

    public int getLine() {
        return position.getLine();
    }

    @Override
    public Arity getArity() {
        return argsNode.getArity();
    }
    
    public DynamicMethod dup() {
        return new InterpretedMethod(getImplementationClass(), staticScope, body, name, argsNode, getVisibility(), position);
    }
}
