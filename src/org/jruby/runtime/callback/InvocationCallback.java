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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.runtime.callback;

import java.lang.reflect.Member;
import org.jruby.Ruby;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.ThreadKill;
import org.jruby.exceptions.MainExitException;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class InvocationCallback implements Callback {
    public static final Class[] EMPTY_ARGS = new Class[0];
    public static final Class[] OPTIONAL_ARGS = new Class[] {IRubyObject[].class};
    protected int arityValue;
    protected Arity arity;
    private Class[] argumentTypes;
    private String javaName;
    private boolean isSingleton;
    private Member target;
    
    public InvocationCallback() {
        this.argumentTypes = EMPTY_ARGS;
    }

    public IRubyObject execute(IRubyObject recv, IRubyObject[] oargs, Block block) {
        if (arityValue >= 0) {
            if (oargs.length != arityValue) {
                throw recv.getRuntime().newArgumentError("wrong number of arguments(" + oargs.length + " for " + arityValue + ")");
            }
        } else {
            if (oargs.length < -(1 + arityValue)) {
                throw recv.getRuntime().newArgumentError("wrong number of arguments(" + oargs.length + " for " + -(1 + arityValue) + ")");
            }
        }
        
        try {
            return call(recv,oargs,block);
        } catch(RaiseException e) {
            throw e;
        } catch(JumpException e) {
            throw e;
        } catch(ThreadKill e) {
            throw e;
        } catch(MainExitException e) {
            throw e;
        } catch(Exception e) {
            Ruby runtime = recv.getRuntime();
            runtime.getJavaSupport().handleNativeException(e, getTarget());
            return runtime.getNil();
        }        
    }

    public abstract IRubyObject call(Object receiver, Object[] args, Block block);

    public void setArity(Arity arity) {
        this.arity = arity;
        this.arityValue = arity.getValue();
    }

    public Arity getArity() {
        return arity;
    }
    
    public void setArgumentTypes(Class[] argumentTypes) {
        this.argumentTypes = argumentTypes;
    }
    
    public Class[] getArgumentTypes() {
        return argumentTypes;   
    }
    
    public void setJavaName(String javaName) {
        this.javaName = javaName;
    }
    
    public String getJavaName() {
        return javaName;
    }
    
    public void setSingleton(boolean isSingleton) {
        this.isSingleton = isSingleton;
    }
    
    public boolean isSingleton() {
        return isSingleton;
    }

    public void setTarget(Member target) {
        this.target = target;
    }

    public Member getTarget() {
        return target;
    }
}// InvocationCallback
