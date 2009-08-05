package org.jruby.util;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ThreadContext;
import org.jruby.RubyHash;
import org.jruby.RubyArray;
import org.jruby.Ruby;

import java.util.Set;
import java.util.HashSet;

public class RecursiveComparator
{
    public static IRubyObject compare(ThreadContext context, String method, IRubyObject a, IRubyObject b, Set<Pair> seen) {
        Ruby runtime = context.getRuntime();

        if (a == b) {
            return runtime.getTrue();
        }

        if (a instanceof RubyHash && b instanceof RubyHash ||
            a instanceof RubyArray && b instanceof RubyArray) {

            RecursiveComparator.Pair pair = new RecursiveComparator.Pair(a, b);

            if (seen == null) {
                seen = new HashSet<Pair>();
            }
            else if (seen.contains(pair)) { // are we recursing?
                return runtime.getTrue();
            }

            seen.add(pair);
        }

        if (a instanceof RubyHash) {
            RubyHash hash = (RubyHash) a;
            return hash.compare(context, method, b, seen);
        }
        else if (a instanceof RubyArray) {
            RubyArray array = (RubyArray) a;
            return array.compare(context, method, b, seen);
        }
        else {
            return a.callMethod(context, method, b);
        }
    }

    public static class Pair
    {
        private int a;
        private int b;

        public Pair(IRubyObject a, IRubyObject b) {
            this.a = System.identityHashCode(a);
            this.b = System.identityHashCode(b);
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (other == null || !(other instanceof Pair)) {
                return false;
            }

            Pair pair = (Pair) other;

            return a == pair.a && b == pair.b;
        }

        @Override
        public int hashCode() {
            int result = a;
            result = 31 * result + b;
            return result;
        }
    }

}
