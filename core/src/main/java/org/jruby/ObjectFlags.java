package org.jruby;

import org.jruby.ext.stringio.StringIO;

/**
 * Flags used by RubyBasicObject descendants.
 */
public interface ObjectFlags {
    FlagRegistry registry = new FlagRegistry();

    // These flags must be registered from top of hierarchy down to maintain order.
    // TODO: Replace these during the build with their calculated values.
    int FALSE_F = registry.newFlag(RubyBasicObject.class);
    int NIL_F = registry.newFlag(RubyBasicObject.class);
    int FROZEN_F = registry.newFlag(RubyBasicObject.class);

    // Deprecated and unused but don't move due to checks elsewhere for the following flags
    @Deprecated
    int TAINTED_F = registry.newFlag(RubyBasicObject.class);

    int CACHEPROXY_F = registry.newFlag(RubyModule.class);
    int NEEDSIMPL_F = registry.newFlag(RubyModule.class);
    int REFINED_MODULE_F = registry.newFlag(RubyModule.class);
    int IS_OVERLAID_F = registry.newFlag(RubyModule.class);
    int OMOD_SHARED = registry.newFlag(RubyModule.class);
    int INCLUDED_INTO_REFINEMENT = registry.newFlag(RubyModule.class);

    int CR_7BIT_F    = registry.newFlag(RubyString.class);
    int CR_VALID_F   = registry.newFlag(RubyString.class);

    int STRIO_READABLE = registry.newFlag(StringIO.class);
    int STRIO_WRITABLE = registry.newFlag(StringIO.class);

    int MATCH_BUSY = registry.newFlag(RubyMatchData.class);

    int COMPARE_BY_IDENTITY_F = registry.newFlag(RubyHash.class);
    int PROCDEFAULT_HASH_F = registry.newFlag(RubyHash.class);
    int KEYWORD_ARGUMENTS_F = registry.newFlag(RubyHash.class);
    int RUBY2_KEYWORD_F = registry.newFlag(RubyHash.class);
}
