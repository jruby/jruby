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
package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.runtime.Arity;
import org.jruby.runtime.DynamicMethod;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * MultiStubMethod provides a mapping from an individual Ruby method to one of
 * the Java methods directly invokable behind a MultiStub. It uses the
 * given index to switch and choose the correct MultiStub method to invoke.
 */
public class MultiStubMethod extends AbstractMethod implements
        Cloneable {
    private Arity arity;
    private MultiStub stub;
    private int index;
    
    public MultiStubMethod(MultiStub stub, int index, RubyModule implementationClass, Arity arity, Visibility visibility) {
        super(implementationClass, visibility);
        this.arity = arity;
        this.stub = stub;
        this.index = index;
        
        assert arity != null;
    }

    public void preMethod(ThreadContext context, RubyModule lastClass, IRubyObject recv, String name, IRubyObject[] args, boolean noSuper) {
        context.preReflectedMethodInternalCall(implementationClass, lastClass, recv, name, args, noSuper);
    }
    
    public void postMethod(ThreadContext context) {
        context.postReflectedMethodInternalCall();
    }
    
    public IRubyObject internalCall(ThreadContext context, IRubyObject receiver, RubyModule lastClass, String name, IRubyObject[] args, boolean noSuper) {
        switch (index) {
        case 0:
            return stub.method0(context, receiver, args);
        case 1:
            return stub.method1(context, receiver, args);
        case 2:
            return stub.method2(context, receiver, args);
        case 3:
            return stub.method3(context, receiver, args);
        case 4:
            return stub.method4(context, receiver, args);
        case 5:
            return stub.method5(context, receiver, args);
        case 6:
            return stub.method6(context, receiver, args);
        case 7:
            return stub.method7(context, receiver, args);
        case 8:
            return stub.method8(context, receiver, args);
        case 9:
            return stub.method9(context, receiver, args);
        }
        
        assert false;
        return null;
    }
    
    // TODO:  Perhaps abstract method should contain this and all other Methods should pass in decent value
    public Arity getArity() {
        return arity;
    }

    public DynamicMethod dup() {
        try {
            MultiStubMethod msm = (MultiStubMethod)clone();
            return msm;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }
}
