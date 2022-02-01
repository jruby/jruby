/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2001-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2004-2007 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.RubyBasicObject;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.parser.StaticScope;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.scope.ManyVarsDynamicScope;

/**
 *  Internal live representation of a block ({...} or do ... end).
 */
public class Binding {
    /**
     * frame of method which defined this block
     */
    private final Frame frame;

    public String method;
    public String filename;
    public int line;

    private Visibility visibility;
    /**
     * 'self' at point when the block is defined
     */
    private IRubyObject self;
    
    /**
     * A reference to all variable values (and names) that are in-scope for this block.
     */
    private final DynamicScope dynamicScope;

    /**
     * Binding-local scope for 1.9 mode.
     */
    private DynamicScope evalScope;

    /**
     * Location of eval scope.
     *
     *
     * Because bindings are usually cloned before used for eval, we indirect
     * the reference of the eval scope through another Binding reference,
     * allowing us to share the same eval scope across multiple cloned Binding
     * instances.
     */
    private Binding evalScopeBinding = this;

    public Binding(IRubyObject self, Frame frame,
                   Visibility visibility, DynamicScope dynamicScope, String method, String filename, int line) {
        frame.getClass(); // null check

        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.dynamicScope = dynamicScope;
        this.method = method;
        this.filename = filename;
        this.line = line;
    }

    private Binding(IRubyObject self, Frame frame,
                    Visibility visibility, DynamicScope dynamicScope, String method, String filename, int line, DynamicScope dummyScope) {
        frame.getClass(); // null check

        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.dynamicScope = dynamicScope;
        this.method = method;
        this.filename = filename;
        this.line = line;
        this.dummyScope = dummyScope;
    }
    
    public Binding(Frame frame, DynamicScope dynamicScope, String method, String filename, int line) {
        frame.getClass(); // null check

        this.self = frame.getSelf();
        this.frame = frame;
        this.visibility = frame.getVisibility();
        this.dynamicScope = dynamicScope;
        this.method = method;
        this.filename = filename;
        this.line = line;
    }

    public Binding(IRubyObject self) {
        this.self = self;
        this.frame = Block.NULL_BLOCK.getFrame();
        this.dynamicScope = null;
    }

    public Binding(IRubyObject self, Frame frame,
                   Visibility visibility) {
        frame.getClass(); // null check

        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.dynamicScope = null;
    }

    public Binding(IRubyObject self, DynamicScope dynamicScope) {
        this.self = self;
        this.frame = Block.NULL_BLOCK.getFrame();
        this.dynamicScope = dynamicScope;
    }

    public Binding(IRubyObject self, Frame frame,
                   Visibility visibility, DynamicScope dynamicScope) {
        frame.getClass(); // null check

        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.dynamicScope = dynamicScope;
    }
    
    private Binding(Binding other) {
        this(other.self, other.frame, other.visibility, other.dynamicScope, other.method, other.filename, other.line, other.dummyScope);
    }

    /**
     * Clone the binding, but maintain a reference to the original "eval
     * binding" to continue sharing eval context.
     * 
     * @return a new Binding with shared eval context
     */
    public Binding cloneForEval() {
        Binding clone = new Binding(this);
        clone.evalScopeBinding = this;
        return clone;
    }

    /**
     * Clone the binding. The frame will be duplicated, and eval context will
     * point to the new binding, but other fields will be copied as-is.
     * 
     * @return a new cloned Binding
     */
    public Binding clone() {
        return new Binding(this);
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }
    
    public IRubyObject getSelf() {
        return self;
    }
    
    public void setSelf(IRubyObject self) {
        this.self = self;
    }

    /**
     * Gets the dynamicVariables that are local to this block.   Parent dynamic scopes are also
     * accessible via the current dynamic scope.
     * 
     * @return Returns all relevant variable scoping information
     */
    public DynamicScope getDynamicScope() {
        return dynamicScope;
    }

    private DynamicScope dummyScope;

    public DynamicScope getDummyScope(StaticScope staticScope) {
        if (dummyScope == null || dummyScope.getStaticScope() != staticScope) {
            return dummyScope = DynamicScope.newDummyScope(staticScope, dynamicScope);
        }
        return dummyScope;
    }

    /**
     * Gets the frame.
     * 
     * @return Returns a RubyFrame
     */
    public Frame getFrame() {
        return frame;
    }

    public String getFile() {
        return filename;
    }

    public void setFile(String filename) {
        this.filename = filename;
    }

    public int getLine() {
        return line;
    }

    public void setLine(int line) {
        this.line = line;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public boolean equals(Object other) {
        if(this == other) {
            return true;
        }

        if(!(other instanceof Binding)) {
            return false;
        }

        Binding bOther = (Binding)other;

        return this.self == bOther.self &&
            this.dynamicScope == bOther.dynamicScope;
    }

    // FIXME: This is because we clone the same explicit binding whenever we execute because both the captured Frame
    // and the binding gets mutated during execution.  This means that we cannot share the same instance across
    // concurrent evals of the same binding.  The mutated Frames I think can become new frames during execution and
    // most of the binding clone can probably go away and we can push the values stored in a binding through the execution
    // path.
    public final DynamicScope getEvalScope(Ruby runtime) {
        // We create one extra dynamicScope on a binding so that when we 'eval "b=1", binding' the
        // 'b' will get put into this new dynamic scope.  The original scope does not see the new
        // 'b' and successive evals with this binding will.  Note: This only happens for explicit
        // bindings.  Implicit bindings will always dispose of the scope they create.

        // No eval scope set, so we create one
        if (evalScopeBinding.evalScope == null) {
            // bindings scopes must always be ManyVars scopes since evals can grow them
            evalScopeBinding.evalScope = new ManyVarsDynamicScope(runtime.getStaticScopeFactory().newEvalScope(dynamicScope.getStaticScope()), dynamicScope);
        }

        return evalScopeBinding.evalScope;
    }

    @Deprecated
    public Binding(IRubyObject self, Frame frame,
                   Visibility visibility, DynamicScope dynamicScope, BacktraceElement backtrace) {
        this.self = self;
        this.frame = frame;
        this.visibility = visibility;
        this.dynamicScope = dynamicScope;
        this.method = backtrace.method;
        this.filename = backtrace.filename;
        this.line = backtrace.line;
    }

    @Deprecated
    public Binding(Frame frame, DynamicScope dynamicScope, BacktraceElement backtrace) {
        this.self = frame.getSelf();
        this.frame = frame;
        this.visibility = frame.getVisibility();
        this.dynamicScope = dynamicScope;
        this.method = backtrace.method;
        this.filename = backtrace.filename;
        this.line = backtrace.line;
    }

    @Deprecated
    public BacktraceElement getBacktrace() {
        return new BacktraceElement(method, filename, line);
    }
}
