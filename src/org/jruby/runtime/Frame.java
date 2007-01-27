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
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
    private IRuby runtime;
    private boolean callingZSuper;
    // The block that was in play during this frame.
    private Block block;

    private Scope scope;

    public Frame(ThreadContext threadContext) {
        this(threadContext.getRuntime(), null, IRubyObject.NULL_ARRAY, null, null, 
                threadContext.getPosition(), null); 
    }

    public Frame(ThreadContext threadContext, IRubyObject self, IRubyObject[] args, 
    		String lastFunc, RubyModule lastClass, ISourcePosition position, Block block) {
    	this(threadContext.getRuntime(), self, args, lastFunc, lastClass, position, block);
    }

    private Frame(IRuby runtime, IRubyObject self, IRubyObject[] args, String lastFunc,
                 RubyModule lastClass, ISourcePosition position, Block block) {
        this.self = self;
        this.args = args;
        this.lastFunc = lastFunc;
        this.lastClass = lastClass;
        this.position = position;
        this.runtime = runtime;
        this.block = block;
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

    /** Getter for property lastClass.
     * @return Value of property lastClass.
     */
    RubyModule getLastClass() {
        return lastClass;
    }
    
    public void setLastClass(RubyModule lastClass) {
        this.lastClass = lastClass;
    }
    
    public void setLastFunc(String lastFunc) {
        this.lastFunc = lastFunc;
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
    
    public void newScope() {
        setScope(new Scope());
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

        return new Frame(runtime, self, newArgs, lastFunc, lastClass, position, block);
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
        StringBuffer sb = new StringBuffer(50);
        sb.append(position != null ? position.toString() : "-1");
        sb.append(':');
        sb.append(lastClass + " " + lastFunc);
        if (lastFunc != null) {
            sb.append("in ");
            sb.append(lastFunc);
        }
        return sb.toString();
    }
    
    public boolean getCallingZSuper() {
        return callingZSuper;
    }

    public void setCallingZSuper(boolean callingZSuper) {
        this.callingZSuper = callingZSuper;
    }
    
    public Block getBlock() {
        return block;
    }
}
