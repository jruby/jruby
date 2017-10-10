/*
 ***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
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

import org.jruby.RubyModule;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.cli.Options;

import java.lang.invoke.MethodHandles;

/**
 * A set of factory methods to construct optimizing utilities for compilation,
 * cache invalidation, and so on.
 */
public class OptoFactory {

    /**
     * Create a new "constant" representation for this object, conforming to the given concrete type. This is currently
     * only used by invokedynamic to cache "constant" method handle wrappers for common literal fixnums and symbols.
     * @param type the class to which the constant should conform
     * @return a "constant" representation of this object appropriate to the current JVM and runtime modes
     */
    public static final Object newConstantWrapper(Class type, Object object) {
        return OptoFactory.CONSTANT_FACTORY.create(type, object);
    }

    public static Invalidator newConstantInvalidator() {
        return new SwitchPointInvalidator();
    }

    private static Boolean indyEnabled() {
        return Options.COMPILE_INVOKEDYNAMIC.load();
    }

    public static Invalidator newGlobalInvalidator(int maxFailures) {
        return new FailoverSwitchPointInvalidator(maxFailures);
    }

    public static Invalidator newMethodInvalidator(RubyModule module) {
        if (indyEnabled()) {
            try {
                return new GenerationAndSwitchPointInvalidator(module);
            } catch (Error e) {
                disableIndy();
                throw e;
            } catch (Throwable t) {
                disableIndy();
            }
        }
        return new GenerationInvalidator(module);
    }

    private static Boolean indyConstants() {
        return Options.INVOKEDYNAMIC_CACHE_CONSTANTS.load();
    }

    private static void disableIndy() {
        Options.COMPILE_INVOKEDYNAMIC.force("false");
    }

    /**
     * A factory for abstract "constant" representations of objects. This is currently only used by our invokedynamic
     * support to cache the "constant" handles that wrap common literal fixnums and symbols. See #2058.
     */
    public static interface ConstantFactory {
        /**
         * Return a representation of a "constant" suitable for optimization in the current runtime. For invokedynamic,
         * this produces a MethodHandles.constant wrapper around the given object, typed with the given type.
         *
         * @param type the type to which the constant should conform
         * @param object the object which represents the constant's value
         * @return a constant representation suitable for optimization
         */
        public Object create(Class type, Object object);
    }

    /**
     * A constant factory that produces MethodHandle constants that drop an initial ThreadContext argument.
     */
    private static class MethodHandleConstantFactory implements ConstantFactory {
        public Object create(Class type, Object object) {
            return MethodHandles.dropArguments(
                    MethodHandles.constant(type, object),
                    0,
                    ThreadContext.class);
        }
    }

    /**
     * A dummy factory, for when we are not running with invokedynamic.
     */
    private static class DummyConstantFactory implements ConstantFactory {
        public Object create(Class type, Object object) {
            return null;
        }
    }

    /**
     * The constant factory we'll be using for this run.
     */
    private static final ConstantFactory CONSTANT_FACTORY = Options.COMPILE_INVOKEDYNAMIC.load() ?
            new MethodHandleConstantFactory() :
            new DummyConstantFactory();
}
