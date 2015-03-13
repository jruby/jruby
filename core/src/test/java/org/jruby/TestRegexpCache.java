package org.jruby;

import org.joni.Regex;
import org.jruby.test.TestRubyBase;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

public class TestRegexpCache extends TestRubyBase {
    // GH-2078
    private static final ByteList GH2078_TEST_BYTELIST = ByteList.create("GH2078");
    public void testCacheRetention() {
        RubyRegexp.quotedPatternCache.clear();

        // Should be the same object if cached
        assertSame(RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST, GH2078_TEST_BYTELIST.getEncoding(), RegexpOptions.NULL_OPTIONS), RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST, GH2078_TEST_BYTELIST.getEncoding(), RegexpOptions.NULL_OPTIONS));

        // Should only have one entry
        assertEquals(1, RubyRegexp.quotedPatternCache.size());

        // attempt to trigger GC and clean up cache for ten seconds
        long time = System.currentTimeMillis();
        while ((System.currentTimeMillis() - time) < 10000) {
            Thread.yield();
            System.gc();
            if (RubyRegexp.quotedPatternCache.size() == 0) break;
        }

        // Should be no retained references once cleaned
        assertEquals(0, RubyRegexp.quotedPatternCache.size());
    }
}
