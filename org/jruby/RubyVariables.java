package org.jruby;

import org.jruby.runtime.*;

public class RubyVariables {
    public static void createVariables(Ruby ruby) {
        SafeAccessor safeAccessor = new SafeAccessor();
        ruby.defineVirtualVariable("$SAFE", safeAccessor, safeAccessor);

    }

    private static class SafeAccessor implements RubyGlobalEntry.GetterMethod, RubyGlobalEntry.SetterMethod {
        /*
         * @see GetterMethod#get(String, RubyObject, RubyGlobalEntry)
         */
        public RubyObject get(String id, RubyObject value, RubyGlobalEntry entry) {
            return RubyFixnum.newFixnum(entry.getRuby(), entry.getRuby().getSafeLevel());
        }

        /*
         * @see SetterMethod#set(RubyObject, String, RubyObject, RubyGlobalEntry)
         */
        public void set(RubyObject value, String id, RubyObject data, RubyGlobalEntry entry) {
            Ruby ruby = entry.getRuby();

            int level = RubyFixnum.fix2int(value);

            if (level < ruby.getSafeLevel()) {
                throw new SecurityException("tried to downgrade level from " + ruby.getSafeLevel() + " to " + level);
            } else {
                ruby.setSafeLevel(level);
                // thread.setSafeLevel(level);
            }
        }
    }
}