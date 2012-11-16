/*
 ***** BEGIN LICENSE BLOCK *****
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
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime;

import org.jruby.Ruby;
import org.jruby.runtime.backtrace.BacktraceElement;
import org.jruby.RubyModule;
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
    private final BacktraceElement backtrace;
    private final RubyModule klass;

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
     *
     * Because bindings are usually cloned before used for eval, we make this an array,
     * so it can be shared between both the original binding and the cloned copy.
     */
    private DynamicScope[] evalScope = new DynamicScope[1];
    
    public Binding(IRubyObject self, Frame frame,
            Visibility visibility, RubyModule klass, DynamicScope dynamicScope, BacktraceElement backtrace) {
        this.self = self;
        this.frame = frame.duplicate();
        this.visibility = visibility;
        this.klass = klass;
        this.dynamicScope = dynamicScope;
        this.backtrace = backtrace;
    }

    private Binding(IRubyObject self, Frame frame,
                   Visibility visibility, RubyModule klass, DynamicScope dynamicScope, DynamicScope[] evalScope, BacktraceElement backtrace) {
        this.self = self;
        this.frame = frame.duplicate();
        this.visibility = visibility;
        this.klass = klass;
        this.dynamicScope = dynamicScope;
        this.evalScope = evalScope;
        this.backtrace = backtrace;
    }
    
    public Binding(Frame frame, RubyModule bindingClass, DynamicScope dynamicScope, BacktraceElement backtrace) {
        this.self = frame.getSelf();
        this.frame = frame.duplicate();
        this.visibility = frame.getVisibility();
        this.klass = bindingClass;
        this.dynamicScope = dynamicScope;
        this.backtrace = backtrace;
    }

    public Binding clone() {
        return new Binding(self, frame, visibility, klass, dynamicScope, evalScope, backtrace);
    }

    public Binding deepClone() {
        return new Binding(self, frame.duplicate(), visibility, klass, dynamicScope.cloneScope(), evalScope.clone(), backtrace);
    }

    public Binding clone(Visibility visibility) {
        return new Binding(self, frame, visibility, klass, dynamicScope, evalScope, backtrace);
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
     * @return Returns all relevent variable scoping information
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

    /**
     * Gets the klass.
     * @return Returns a RubyModule
     */
    public RubyModule getKlass() {
        return klass;
    }

    public String getFile() {
        return backtrace.filename;
    }

    public void setFile(String file) {
        backtrace.filename = file;
    }

    public int getLine() {
        return backtrace.line;
    }

    public void setLine(int line) {
        backtrace.line = line;
    }

    public String getMethod() {
        return backtrace.method;
    }

    public void setMethod(String method) {
        backtrace.method = method;
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

    public final DynamicScope getEvalScope(Ruby runtime) {
        // We create one extra dynamicScope on a binding so that when we 'eval "b=1", binding' the
        // 'b' will get put into this new dynamic scope.  The original scope does not see the new
        // 'b' and successive evals with this binding will.  I take it having the ability to have
        // succesive binding evals be able to share same scope makes sense from a programmers
        // perspective.   One crappy outcome of this design is it requires Dynamic and Static
        // scopes to be mutable for this one case.

        // Note: In Ruby 1.9 all of this logic can go away since they will require explicit
        // bindings for evals.

        // We only define one special dynamic scope per 'logical' binding.  So all bindings for
        // the same scope should share the same dynamic scope.  This allows multiple evals with
        // different different bindings in the same scope to see the same stuff.

        // No binding scope so we should create one
        if (evalScope[0] == null) {
            // If the next scope out has the same binding scope as this scope it means
            // we are evaling within an eval and in that case we should be sharing the same
            // binding scope.
            DynamicScope parent = dynamicScope.getNextCapturedScope();
            if (parent != null && parent.getEvalScope(runtime) == dynamicScope) {
                evalScope[0] = dynamicScope;
            } else {
                // bindings scopes must always be ManyVars scopes since evals can grow them
                evalScope[0] = new ManyVarsDynamicScope(runtime.getStaticScopeFactory().newEvalScope(dynamicScope.getStaticScope()), dynamicScope);
            }
        }

        return evalScope[0];
    }
}
