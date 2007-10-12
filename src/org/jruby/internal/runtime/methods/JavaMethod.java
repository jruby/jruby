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
import org.jruby.internal.runtime.JumpTarget;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;

/**
 */
public abstract class JavaMethod extends DynamicMethod implements JumpTarget, Cloneable {
    protected int arityValue;
    protected Arity arity;
    private Class[] argumentTypes;
    private String javaName;
    private boolean isSingleton;

    public JavaMethod(RubyModule implementationClass, Visibility visibility) {
        super(implementationClass, visibility, CallConfiguration.JAVA_FULL);
    }

    public abstract IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name, IRubyObject[] args, Block block);
    
    public DynamicMethod dup() {
        try {
            JavaMethod msm = (JavaMethod)clone();
            return msm;
        } catch (CloneNotSupportedException cnse) {
            return null;
        }
    }
    
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
    
    @Override
    public boolean isNative() {
        return true;
    }
}
