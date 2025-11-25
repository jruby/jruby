package org.jruby;

/**
 * Flags used by RubyBasicObject descendants.
 */
public interface ObjectFlags {
    FlagRegistry registry = new FlagRegistry();

    // These flags must be registered from top of hierarchy down to maintain order.
    // TODO: Replace these during the build with their calculated values.
    int FALSE_F = registry.newFlag(RubyBasicObject.class);
    int NIL_F = registry.newFlag(RubyBasicObject.class);
    @Deprecated(since = "10.0.3.0")
    int FROZEN_F = registry.newFlag(RubyBasicObject.class);

    int CACHEPROXY_F = registry.newFlag(RubyModule.class);
    int NEEDSIMPL_F = registry.newFlag(RubyModule.class);
    int REFINED_MODULE_F = registry.newFlag(RubyModule.class);
    int IS_OVERLAID_F = registry.newFlag(RubyModule.class);
    int OMOD_SHARED = registry.newFlag(RubyModule.class);
    int INCLUDED_INTO_REFINEMENT = registry.newFlag(RubyModule.class);
    int TEMPORARY_NAME = registry.newFlag(RubyModule.class);

    // order is important here; CR_7BIT_f needs to be 16 and CR_VALID_F needs to be 32 to match values in Prism parser
    int FSTRING      = registry.newFlag(RubyString.class);
    int CR_7BIT_F    = registry.newFlag(RubyString.class);
    int CR_VALID_F   = registry.newFlag(RubyString.class);
    int CHILLED_LITERAL_F = registry.newFlag(RubyString.class);
    int CHILLED_SYMBOL_TO_S_F = registry.newFlag(RubyString.class);

    int MATCH_BUSY = registry.newFlag(RubyMatchData.class);

    int COMPARE_BY_IDENTITY_F = registry.newFlag(RubyHash.class);
    int KEYWORD_REST_ARGUMENTS_F = registry.newFlag(RubyHash.class);
    int PROCDEFAULT_HASH_F = registry.newFlag(RubyHash.class);
    int KEYWORD_ARGUMENTS_F = registry.newFlag(RubyHash.class);
    int RUBY2_KEYWORD_F = registry.newFlag(RubyHash.class);
}
