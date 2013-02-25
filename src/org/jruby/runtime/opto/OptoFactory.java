/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.runtime.opto;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.impl.BaseBodyCompiler;
import org.jruby.compiler.impl.InheritedCacheCompiler;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.compiler.impl.StandardInvocationCompiler;
import org.objectweb.asm.Opcodes;

/**
 * A set of factory methods to construct optimizing utilities for compilation,
 * cache invalidation, and so on.
 */
public class OptoFactory {
    public static InvocationCompiler newInvocationCompiler(BaseBodyCompiler bodyCompiler, SkinnyMethodAdapter method) {
        if (invDynInvCompilerConstructor != null) {
            try {
                return (InvocationCompiler) invDynInvCompilerConstructor.newInstance(bodyCompiler, method);
            } catch (InstantiationException ie) {
                // do nothing, fall back on default compiler below
                } catch (IllegalAccessException ie) {
                // do nothing, fall back on default compiler below
                } catch (InvocationTargetException ie) {
                // do nothing, fall back on default compiler below
                }
        }
        return new StandardInvocationCompiler(bodyCompiler, method);
    }
    
    public static CacheCompiler newCacheCompiler(StandardASMCompiler scriptCompiler) {
        if (invDynCacheCompilerConstructor != null) {
            try {
                return (CacheCompiler)invDynCacheCompilerConstructor.newInstance(scriptCompiler);
            } catch (InstantiationException ie) {
                // do nothing, fall back on default compiler below
                } catch (IllegalAccessException ie) {
                // do nothing, fall back on default compiler below
                } catch (InvocationTargetException ie) {
                // do nothing, fall back on default compiler below
                }
        }
        return new InheritedCacheCompiler(scriptCompiler);
    }
    
    public static Invalidator newConstantInvalidator() {
        if (RubyInstanceConfig.JAVA_VERSION == Opcodes.V1_7 && RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            try {
                return (Invalidator)Class.forName("org.jruby.runtime.opto.SwitchPointInvalidator").newInstance();
            } catch (Throwable t) {
                // ignore
            }
        }
        return new ObjectIdentityInvalidator();
    }
    
    public static Invalidator newGlobalInvalidator(int maxFailures) {
        if (RubyInstanceConfig.JAVA_VERSION == Opcodes.V1_7 && RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            try {
                Class failoverInvalidator = Class.forName("org.jruby.runtime.opto.FailoverSwitchPointInvalidator");
                Constructor constructor = failoverInvalidator.getConstructor(int.class);
                return (Invalidator)constructor.newInstance(maxFailures);
            } catch (Throwable t) {
                // ignore
            }
        }
        return new ObjectIdentityInvalidator();
    }
    
    public static Invalidator newMethodInvalidator(RubyModule module) {
        if (RubyInstanceConfig.JAVA_VERSION == Opcodes.V1_7 && RubyInstanceConfig.INVOKEDYNAMIC_INVOCATION_SWITCHPOINT) {
            try {
                return (Invalidator)Class.forName("org.jruby.runtime.opto.GenerationAndSwitchPointInvalidator").getConstructor(RubyModule.class).newInstance(module);
            } catch (Throwable t) {
                // ignore
            }
        }
        return new GenerationInvalidator(module);
    }
    
    // constructors for compiler impls
    public static final Constructor invDynInvCompilerConstructor;
    public static final Constructor invDynCacheCompilerConstructor;
    
    static {
        Constructor invCompilerConstructor = null;
        if (RubyInstanceConfig.INVOKEDYNAMIC_INVOCATION) {
            try {
                Class compiler =
                        Class.forName("org.jruby.compiler.impl.InvokeDynamicInvocationCompiler");
                invCompilerConstructor = compiler.getConstructor(BaseBodyCompiler.class, SkinnyMethodAdapter.class);
            } catch (Exception e) {
                // leave it null and fall back on our normal invocation logic
            }
        }
        invDynInvCompilerConstructor = invCompilerConstructor;
        
        Constructor cacheCompilerConstructor = null;
        if (RubyInstanceConfig.INVOKEDYNAMIC_CACHE) {
            try {
                Class compiler =
                        Class.forName("org.jruby.compiler.impl.InvokeDynamicCacheCompiler");
                cacheCompilerConstructor = compiler.getConstructor(StandardASMCompiler.class);
            } catch (Exception e) {
                // leave it null and fall back on our normal invocation logic
            }
        }
        invDynCacheCompilerConstructor = cacheCompilerConstructor;
    }
}
