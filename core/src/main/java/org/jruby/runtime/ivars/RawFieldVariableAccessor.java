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

package org.jruby.runtime.ivars;

import com.headius.invokebinder.Binder;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.java.proxies.ConcreteJavaProxy;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.specialized.RubyObjectSpecializer;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;

/**
 * A variable accessor that accesses a reified java field directly (Storing java objects that may not be IRubyObjects
 */
public class RawFieldVariableAccessor extends FieldVariableAccessor {
    /**
     * Construct a new RawFieldVariableAccessor for the given "real" class,
     * variable name, variable index, class ID, and field offset
     * 
     * @param realClass the "real" class
     * @param unwrapInSet if the setter should unwrap ruby objects for java use use
     * @param toJava the variable's java type
     * @param name the variable's name
     * @param index the variable's index
     * @param classId the class's ID
     * @param getter the getter handle for the field
     * @param setter the setter handle for the field
     */
    public RawFieldVariableAccessor(RubyClass realClass, boolean unwrapInSet, Class<?> toJava, Class<?> returnType, String name, int index, int classId, MethodHandle getter, MethodHandle setter) {
        this(realClass, name, index, classId, wrapGetter(getter, realClass, returnType), wrapSetter(setter, realClass, unwrapInSet, toJava, returnType));
    }

    private RawFieldVariableAccessor(RubyClass realClass, String name, int index, int classId, MethodHandle getter, MethodHandle setter) {
        super(realClass, name, index, classId, getter, setter);
    }

    public RawFieldVariableAccessor cloneFor(RubyClass newRealClass) {
        return new RawFieldVariableAccessor(newRealClass, name, index, classId, getter, setter);
    }
    
    @Override
    protected MethodHandle wrapSetter(MethodHandle setter) {

        // using java_field + ivar storage is an advanced use of jruby for integrating with the jvm more tightly.
        // JVM semantics take precedence over ruby semantics in such usage
        
        // reified concrete classes are never frozen, though non-concrete are. However, we can't freeze fields, and to prevent more 
        // nonsense like @foo= throwing and self.foo= not throwing, when you configure this accessor we use JVM semantics instead of Ruby semantics
        
        return setter; // no frozen check
    }
    
    protected static MethodHandle wrapSetter(MethodHandle setter, RubyClass realClass,boolean unwrap, Class<?> toJava, Class<?> basetype)
    {
        // this method is simple, but we must repeat almost everthing 3 times because the arguments change each configuration for the setters
        try {
            // accepts objects, yay, just need to check if we are proxied
            if (!unwrap)
            {
                if (Boolean.TRUE.equals(realClass.getIsReifiedExtendedJavaClass()))
                {
                    MethodHandle cjpUnwrap = Binder
                            .from(realClass.reifiedClass(), ConcreteJavaProxy.class, Object.class)
                            .dropLast()
                            .cast(Object.class, ConcreteJavaProxy.class)
                            .invokeVirtual(RubyObjectSpecializer.LOOKUP, "unwrap");
                    MethodHandle wrapperSetter = 
                            Binder
                            .from(Object.class, realClass.reifiedClass(), ConcreteJavaProxy.class, Object.class)
                            .permute(0,2)
                            .cast(void.class, realClass.reifiedClass(), basetype)
                            .invoke(setter);
                    MethodHandle temp = MethodHandles.foldArguments(wrapperSetter, cjpUnwrap);
                    return temp;
                }
                else
                return setter;
            }
            // have to unwrap the type for java
            else
            {
                // check if we are proxied
                if (Boolean.TRUE.equals(realClass.getIsReifiedExtendedJavaClass()))
                {
                    // we are given a ConcreteJavaProxy, must unwrap first
                    MethodHandle cjpUnwrap = Binder
                            .from(realClass.reifiedClass(), ConcreteJavaProxy.class, Object.class)
                            .dropLast()
                            .cast(Object.class, ConcreteJavaProxy.class)
                            .invokeVirtual(RubyObjectSpecializer.LOOKUP, "unwrap");
    
                    MethodHandle wrapperSetConvert = 
                            Binder
                            .from(Object.class, realClass.reifiedClass(), ConcreteJavaProxy.class, Object.class)
                            .dropFirst(2)
                            .cast(Object.class, IRubyObject.class)
                            .insert(1, toJava)
                            .invokeVirtual(RubyObjectSpecializer.LOOKUP, "toJava");
                    
                    MethodHandle wrapperSetter = 
                            Binder
                            .from(Object.class, Object.class, realClass.reifiedClass(), ConcreteJavaProxy.class, Object.class)
                            .permute(1,0)
                            .cast(void.class, realClass.reifiedClass(), basetype)
                            .invoke(setter);
    
                    MethodHandle temp = MethodHandles.foldArguments(wrapperSetter, wrapperSetConvert);
                    temp = MethodHandles.foldArguments(temp, cjpUnwrap);
                    return temp;
                }
                else
                {
    
                    MethodHandle wrapperSetConvert = 
                            Binder
                            .from(Object.class, realClass.reifiedClass(), Object.class)
                            .dropFirst()
                            .cast(Object.class, IRubyObject.class)
                            .insert(1, toJava)
                            .invokeVirtual(RubyObjectSpecializer.LOOKUP, "toJava");
                    
                    MethodHandle wrapperSetter = 
                            Binder
                            .from(Object.class, Object.class, realClass.reifiedClass(), Object.class)
                            .permute(1,0)
                            .cast(void.class, realClass.reifiedClass(), basetype)
                            .invoke(setter);
    
                    MethodHandle temp = MethodHandles.foldArguments(wrapperSetter, wrapperSetConvert);
                    return temp;
                }
            }
            
        } catch (NoSuchMethodException |IllegalAccessException e) {
            RaiseException r = realClass.getClassRuntime().newRuntimeError("JRuby bug detected. Please file an issue if not already filed. Raw Field Accessor Setter gave errors: " + e.getMessage());
            r.initCause(e);
            throw r;
        }
    }
    
    protected static MethodHandle wrapGetter(MethodHandle getter, RubyClass realClass, Class<?> basetype) {

        // getter much shorter as we dont need to juggle arguments
        try {
            MethodHandle wrapperGet = 
                    Binder
                    .from(IRubyObject.class, basetype)
                    .cast(IRubyObject.class, Object.class)
                    .insert(0, realClass.getRuntime())
                    .invokeStatic(RubyObjectSpecializer.LOOKUP, JavaUtil.class, "convertJavaToUsableRubyObject");
            
            MethodHandle temp = MethodHandles.filterReturnValue(getter, wrapperGet);

            if (Boolean.TRUE.equals(realClass.getIsReifiedExtendedJavaClass()))
            {
                // we are given a ConcreteJavaProxy, must unwrap first
                MethodHandle cjpUnwrap = Binder
                        .from(realClass.reifiedClass(), Object.class)
                        .cast(Object.class, ConcreteJavaProxy.class)
                        .invokeVirtual(RubyObjectSpecializer.LOOKUP, "unwrap");
    
                temp = MethodHandles.filterReturnValue(cjpUnwrap, temp);
            }
        
            return temp;
            
        } catch (NoSuchMethodException |IllegalAccessException e) {
            RaiseException r = realClass.getClassRuntime().newRuntimeError("JRuby bug detected. Please file an issue if not already filed. Raw Field Accessor getter gave errors: " + e.getMessage());
            r.initCause(e);
            throw r;
        }
    }
}
