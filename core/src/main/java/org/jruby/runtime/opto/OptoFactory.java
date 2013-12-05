/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
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
import org.jruby.RubyInstanceConfig;
import org.jruby.RubyModule;
import org.jruby.compiler.CacheCompiler;
import org.jruby.compiler.InvocationCompiler;
import org.jruby.compiler.impl.BaseBodyCompiler;
import org.jruby.compiler.impl.InheritedCacheCompiler;
import org.jruby.compiler.impl.InvokeDynamicCacheCompiler;
import org.jruby.compiler.impl.InvokeDynamicInvocationCompiler;
import org.jruby.compiler.impl.SkinnyMethodAdapter;
import org.jruby.compiler.impl.StandardASMCompiler;
import org.jruby.compiler.impl.StandardInvocationCompiler;
import org.jruby.util.cli.Options;

/**
 * A set of factory methods to construct optimizing utilities for compilation,
 * cache invalidation, and so on.
 */
public class OptoFactory {
    // failure switches to avoid retrying classes that can't be instantiated
    public static boolean tryIndy = Options.COMPILE_INVOKEDYNAMIC.load();
    
    public static InvocationCompiler newInvocationCompiler(BaseBodyCompiler bodyCompiler, SkinnyMethodAdapter method) {
        if (tryIndy) {
            try {
                return new InvokeDynamicInvocationCompiler(bodyCompiler, method);
            } catch (Error e) {
                tryIndy = false;
                throw e;
            } catch (Throwable t) {
                tryIndy = false;
            }
        }
        return new StandardInvocationCompiler(bodyCompiler, method);
    }
    
    public static CacheCompiler newCacheCompiler(StandardASMCompiler scriptCompiler) {
        if (tryIndy) {
            try {
                return new InvokeDynamicCacheCompiler(scriptCompiler);
            } catch (Error e) {
                tryIndy = false;
                throw e;
            } catch (Throwable t) {
                tryIndy = false;
            }
        }
        return new InheritedCacheCompiler(scriptCompiler);
    }
    
    public static Invalidator newConstantInvalidator() {
        if (tryIndy && Options.COMPILE_INVOKEDYNAMIC.load() && RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            try {
                return new SwitchPointInvalidator();
            } catch (Error e) {
                tryIndy = false;
                throw e;
            } catch (Throwable t) {
                tryIndy = false;
            }
        }
        return new ObjectIdentityInvalidator();
    }
    
    public static Invalidator newGlobalInvalidator(int maxFailures) {
        if (tryIndy && Options.COMPILE_INVOKEDYNAMIC.load() && RubyInstanceConfig.INVOKEDYNAMIC_CONSTANTS) {
            try {
                return new FailoverSwitchPointInvalidator(maxFailures);
            } catch (Error e) {
                tryIndy = false;
                throw e;
            } catch (Throwable t) {
                tryIndy = false;
            }
        }
        return new ObjectIdentityInvalidator();
    }
    
    public static Invalidator newMethodInvalidator(RubyModule module) {
        if (tryIndy && Options.COMPILE_INVOKEDYNAMIC.load() && RubyInstanceConfig.INVOKEDYNAMIC_INVOCATION_SWITCHPOINT) {
            try {
                return new GenerationAndSwitchPointInvalidator(module);
            } catch (Error e) {
                tryIndy = false;
                throw e;
            } catch (Throwable t) {
                tryIndy = false;
            }
        }
        return new GenerationInvalidator(module);
    }
}
