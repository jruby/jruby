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
import org.jruby.ext.ripper.RubyLexer;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Binding;
import org.jruby.runtime.Block;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asBoolean;
import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newArray;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Define.defineClass;
import static org.jruby.util.RubyStringBuilder.str;

/**
 * @author  jpetersen
 */
@JRubyClass(name="Binding")
public class RubyBinding extends RubyObject {
    private Binding binding;

    public RubyBinding(Ruby runtime, RubyClass rubyClass, Binding binding) {
        super(runtime, rubyClass);
        
        this.binding = binding;
    }

    private RubyBinding(Ruby runtime, RubyClass rubyClass) {
        super(runtime, rubyClass);
    }

    public static RubyClass createBindingClass(ThreadContext context, RubyClass Object) {
        return defineClass(context, "Binding", Object, RubyBinding::new).
                reifiedClass(RubyBinding.class).
                classIndex(ClassIndex.BINDING).
                defineMethods(context, RubyBinding.class).
                tap(c -> c.getSingletonClass().undefMethods(context, "new"));
    }

    public Binding getBinding() {
        return binding;
    }

    // Proc class

    public static RubyBinding newBinding(Ruby runtime, Binding binding) {
        return new RubyBinding(runtime, runtime.getBinding(), binding);
    }

    @Deprecated
    public static RubyBinding newBinding(Ruby runtime) {
        return newBinding(runtime, runtime.getCurrentContext().currentBinding());
    }

    @Deprecated
    public static RubyBinding newBinding(Ruby runtime, IRubyObject self) {
       return newBinding(runtime, runtime.getCurrentContext().currentBinding(self));
    }
    
    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize(ThreadContext context) {
        binding = context.currentBinding();
        
        return this;
    }

    @JRubyMethod
    public IRubyObject dup(ThreadContext context) {
        RubyBinding newBinding = newBinding(context.runtime, binding.dup(context));

        copyInstanceVariablesInto(newBinding);

        return newBinding;
    }

    @JRubyMethod(name = "initialize_copy", visibility = Visibility.PRIVATE)
    @Override
    public IRubyObject initialize_copy(IRubyObject other) {
        RubyBinding otherBinding = (RubyBinding)other;
        
        binding = otherBinding.binding.clone();
        
        return this;
    }

    // c: bind_eval
    @JRubyMethod(name = "eval", required = 1, optional = 2, checkArity = false)
    public IRubyObject eval(ThreadContext context, IRubyObject[] args) {
        int argc = Arity.checkArgumentCount(context, args, 1, 3);

        IRubyObject[] newArgs = new IRubyObject[argc+1];
        newArgs[0] = args[0]; // eval string
        newArgs[1] = this; // binding
        if (argc > 1) {
            newArgs[2] = args[1]; // file
            if (argc > 2) {
                newArgs[3] = args[2]; // line
            }
        }

        return RubyKernel.eval(context, this, newArgs, Block.NULL_BLOCK);
    }

    @JRubyMethod(name = "local_variable_defined?")
    public IRubyObject local_variable_defined_p(ThreadContext context, IRubyObject symbol) {
        String id = checkLocalId(context, symbol);
        return asBoolean(context, binding.getEvalScope(context.runtime).getStaticScope().isDefined(id) != -1);
    }

    @JRubyMethod
    public IRubyObject local_variable_get(ThreadContext context, IRubyObject symbol) {
        String id = checkLocalId(context, symbol);
        DynamicScope evalScope = binding.getEvalScope(context.runtime);
        int slot = evalScope.getStaticScope().isDefined(id);

        if (slot == -1) throw context.runtime.newNameError(str(context.runtime, "local variable '", symbol, "' not defined for " + inspect()), symbol);

        return evalScope.getValueOrNil(slot & 0xffff, slot >> 16, context.nil);
    }

    @JRubyMethod
    public IRubyObject local_variable_set(ThreadContext context, IRubyObject symbol, IRubyObject value) {
        String id = checkLocalId(context, symbol);
        DynamicScope evalScope = binding.getEvalScope(context.runtime);
        int slot = evalScope.getStaticScope().isDefined(id);

        if (slot == -1) { // Yay! New variable associated with this binding
            slot = evalScope.getStaticScope().addVariable(id.intern());
            evalScope.growIfNeeded();
        }

        return evalScope.setValue(slot & 0xffff, value, slot >> 16);
    }

    // MRI: check_local_id
    private String checkLocalId(ThreadContext context, IRubyObject obj) {
        String id = RubySymbol.idStringFromObject(context, obj);

        if (!RubyLexer.isIdentifierChar(id.charAt(0))) {
            throw context.runtime.newNameError(str(context.runtime, "wrong local variable name '", obj, "' for ", this), id);
        }

        return id;
    }
    @JRubyMethod
    public IRubyObject local_variables(ThreadContext context) {
        return binding.getEvalScope(context.runtime).getStaticScope().getLocalVariables(context);
    }

    @JRubyMethod(name = "receiver")
    public IRubyObject receiver(ThreadContext context) {
        return binding.getSelf();
    }

    @JRubyMethod
    public IRubyObject source_location(ThreadContext context) {
        IRubyObject filename = newString(context, binding.getFile()).freeze(context);
        RubyFixnum line = asFixnum(context, binding.getLine() + 1); /* zero-based */
        return newArray(context, filename, line);
    }
}
