package org.jruby.util;

import org.jruby.runtime.CallSite;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.RubyHash;
import org.jruby.RubyArray;
import org.jruby.Ruby;
import org.jruby.runtime.invokedynamic.MethodNames;

import static org.jruby.runtime.Helpers.invokedynamic;

import java.util.Set;
import java.util.HashSet;

public class RecursiveComparator {
    public static <T> IRubyObject compare(ThreadContext context, T invokable, IRubyObject a, IRubyObject b) {

        if (a == b) {
            return context.tru;
        }
        
        boolean clear = false; // whether to clear thread-local set (at top comparison)

        try {
            Set<Pair> seen;

            if (a instanceof RubyHash && b instanceof RubyHash ||
                a instanceof RubyArray && b instanceof RubyArray) {

                RecursiveComparator.Pair pair = new RecursiveComparator.Pair(a, b);

                if ((seen = context.getRecursiveSet()) == null) {
                    // 95+% of time set stays low - holding 1 object
                    // NOTE: maybe its worth starting with a singletonSet?
                    context.setRecursiveSet(seen = new HashSet<Pair>(4));
                    clear = true;
                }
                else if (seen.contains(pair)) { // are we recursing?
                    return context.tru;
                }

                seen.add(pair);
            }

            if (a instanceof RubyHash) {
                RubyHash hash = (RubyHash) a;
                return hash.compare(context, (RubyHash.VisitorWithState<RubyHash>) invokable, b);
            }
            if (a instanceof RubyArray) {
                RubyArray array = (RubyArray) a;
                return array.compare(context, (CallSite) invokable, b);
            }
            return ((CallSite) invokable).call(context, a, a, b);
        }
        finally {
            if (clear) context.setRecursiveSet(null);
        }
    }

    public static class Pair
    {
        final int a;
        final int b;

        public Pair(IRubyObject a, IRubyObject b) {
            this.a = System.identityHashCode(a);
            this.b = System.identityHashCode(b);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) return true;
            if (other instanceof Pair) {
                Pair pair = (Pair) other;
                return a == pair.a && b == pair.b;
            }
            return false;
        }

        @Override
        public int hashCode() {
            return 31 * a + b;
        }
    }

}
