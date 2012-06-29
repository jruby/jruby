package org.jruby.java.util;

import org.jruby.IncludedModuleWrapper;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.internal.runtime.methods.DynamicMethod;

import java.util.regex.Pattern;

/**
 * This special module wrapper is used by the Java "package modules" in order to
 * simulate a blank slate. Only a certain subset of method names will carry
 * through to searching the superclass, with all others returning null and
 * triggering the method_missing call needed to handle lazy Java package
 * discovery.
 *
 * Because this is in the hierarchy, it does mean any methods that are not Java
 * packages or otherwise defined on the JavaPackageModuleTemplate module will
 * be inaccessible.
 */
public class BlankSlateWrapper extends IncludedModuleWrapper {
    public BlankSlateWrapper(Ruby runtime, RubyClass superClass, RubyModule delegate) {
        super(runtime, superClass, delegate);
    }

    @Override
    public DynamicMethod searchMethodInner(String name) {
        // this module is special and only searches itself; do not go to superclasses
        // except for special methods

        if (name.equals("__constants__")) {
            return superClass.searchMethodInner("constants");
        }

        if (name.equals("__methods__")) {
            return superClass.searchMethodInner("methods");
        }

        if (KEEP.matcher(name).find()) {
            return superClass.searchMethodInner(name);
        }

        return null;
    }

    private static final Pattern KEEP = Pattern.compile("^(__|<|>|=)|^(class|initialize_copy|singleton_method_added|const_missing|inspect|method_missing|to_s)$|(\\?|!|=)$");
}
