package org.jruby.ir;

import java.util.Map;

import org.jruby.RubyModule;
import org.jruby.parser.StaticScope;
import org.jruby.util.ByteList;

/**
 * The root of a Proc#refined clone tree -- the closure backing the returned proc.  Carries the recipe
 * (source closure + accumulated modules) so a chained call re-clones from the original with the merged
 * list; the nested scopes it encloses remain ordinary IRClosure/IRFor/IRMethod clones.
 */
public class IRRefinedClosure extends IRClosure {
    private IRClosure refinementsSource;
    private RubyModule[] refinementsModules;

    IRRefinedClosure(IRClosure c, IRScope lexicalParent, int closureId, ByteList fullName, StaticScope staticScope) {
        super(c, lexicalParent, closureId, fullName, staticScope);
    }

    @Override
    public boolean isRefinementsClone() {
        return true;
    }

    /** Set before the proc can escape, so materialization cannot precede it. */
    void setRefinementsRecipe(IRClosure source, RubyModule[] modules) {
        this.refinementsSource = source;
        this.refinementsModules = modules;
    }

    public IRClosure getRefinementsSource() {
        return refinementsSource;
    }

    public RubyModule[] getRefinementsModules() {
        return refinementsModules;
    }

    /**
     * Deferring the memo write to materialization means a never-called chain intermediate cannot evict
     * a live entry.
     */
    @Override
    protected void publishRefinementsClone() {
        Map<IRClosure, RefinementsCache> cacheMap = getManager().getRefinementsCloneCache();
        RefinementsCache cache = cacheMap.get(refinementsSource);
        if (cache != null && !cache.matches(refinementsModules)) {
            getManager().getRuntime().getWarnings().warnPerformance(
                    "Proc#refined called with different modules for the same block disables memoization");
        }
        cacheMap.put(refinementsSource, new RefinementsCache(refinementsModules, this));
    }
}
