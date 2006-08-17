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
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime;

import org.jruby.IRuby;
import org.jruby.RubyModule;
import org.jruby.ast.Node;
import org.jruby.evaluator.EvaluationState;
import org.jruby.lexer.yacc.ISourcePosition;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public class Frame {
    private IRubyObject self;
    private IRubyObject[] args;
    private String lastFunc;
    private RubyModule lastClass;
    private final ISourcePosition position;
    private Iter iter;
    private IRuby runtime;
    private EvaluationState evalState;
    private Block blockArg;

    private Scope scope;
    
    public Frame(ThreadContext threadContext, Iter iter, Block blockArg) {
        this(threadContext.getRuntime(), null, IRubyObject.NULL_ARRAY, null, null, threadContext.getPosition(), 
             iter, blockArg);   
    }

    public Frame(ThreadContext threadContext, IRubyObject self, IRubyObject[] args, 
    		String lastFunc, RubyModule lastClass, ISourcePosition position, Iter iter, Block blockArg) {
    	this(threadContext.getRuntime(), self, args, lastFunc, lastClass, position, iter, blockArg);
    }

    private Frame(IRuby runtime, IRubyObject self, IRubyObject[] args, String lastFunc,
                 RubyModule lastClass, ISourcePosition position, Iter iter, Block blockArg) {
        this.self = self;
        this.args = args;
        this.lastFunc = lastFunc;
        this.lastClass = lastClass;
        this.position = position;
        this.iter = iter;
        this.runtime = runtime;
        this.blockArg = blockArg;
    }
    
    public void begin(Node node) {
        evalState.begin2(node);
    }
    
    public void step() {
        if (evalState.hasNext()) {
            evalState.executeNext();
        }
    }

    /** Getter for property args.
     * @return Value of property args.
     */
    IRubyObject[] getArgs() {
        return args;
    }

    /** Setter for property args.
     * @param args New value of property args.
     */
    void setArgs(IRubyObject[] args) {
        this.args = args;
    }

    /**
     * @return the frames current position
     */
    ISourcePosition getPosition() {
        return position;
    }

    /** Getter for property iter.
     * @return Value of property iter.
     */
    Iter getIter() {
        return iter;
    }

    /** Setter for property iter.
     * @param iter New value of property iter.
     */
    void setIter(Iter iter) {
        this.iter = iter;
    }

    boolean isBlockGiven() {
        return iter.isBlockGiven();
    }

    /** Getter for property lastClass.
     * @return Value of property lastClass.
     */
    RubyModule getLastClass() {
        return lastClass;
    }

    /** Getter for property lastFunc.
     * @return Value of property lastFunc.
     */
    String getLastFunc() {
        return lastFunc;
    }

    /** Getter for property self.
     * @return Value of property self.
     */
    IRubyObject getSelf() {
        return self;
    }

    /** Setter for property self.
     * @param self New value of property self.
     */
    void setSelf(IRubyObject self) {
        this.self = self;
    }
    
    void newScope(String[] localNames) {
        setScope(new Scope(runtime, localNames));
    }
    
    Scope getScope() {
        return scope;
    }
    
    Scope setScope(Scope newScope) {
        Scope oldScope = scope;
        
        scope = newScope;
        
        return oldScope;
    }
    
    public Frame duplicate() {
        IRubyObject[] newArgs;
        if (args.length != 0) {
            newArgs = new IRubyObject[args.length];
            System.arraycopy(args, 0, newArgs, 0, args.length);
        } else {
        	newArgs = args;
        }

        return new Frame(runtime, self, newArgs, lastFunc, lastClass, position, iter, blockArg);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(50);
        sb.append(position != null ? position.toString() : "-1");
        sb.append(':');
        if (lastFunc != null) {
            sb.append("in ");
            sb.append(lastFunc);
        }
        return sb.toString();
    }

    EvaluationState getEvalState() {
        return evalState != null ? evalState : (evalState = new EvaluationState(runtime, self));
    }

    void setEvalState(EvaluationState evalState) {
        this.evalState = evalState;
    }
    
    Block getBlockArg() {
        return blockArg;
    }
}
