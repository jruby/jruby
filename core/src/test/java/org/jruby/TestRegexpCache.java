package org.jruby;

import org.joni.Regex;
import org.jruby.test.TestRubyBase;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

public class TestRegexpCache extends TestRubyBase {

    // GH-2078
    public void testCacheRetention() {
        // final ByteList GH2078_TEST_BYTELIST = ByteList.create("GH2078");

        RubyRegexp.quotedPatternCache.clear();

        // Should be the same object if cached
        //Regex regex = RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST, RegexpOptions.NULL_OPTIONS, false);
        //assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST, RegexpOptions.NULL_OPTIONS, false));
        //assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST.dup(), RegexpOptions.NULL_OPTIONS, false));
        RubyString str = RubyString.newString(runtime, ByteList.create("GH2078"));

        Regex regex = RubyRegexp.getQuotedRegexpFromCache(runtime, str, RegexpOptions.NULL_OPTIONS);
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, str, RegexpOptions.NULL_OPTIONS));
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, (RubyString) str.dup(), RegexpOptions.NULL_OPTIONS));
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, RubyString.newString(runtime, "GH2078"), RegexpOptions.NULL_OPTIONS));
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, str.newFrozen(), RegexpOptions.NULL_OPTIONS));

        // Should only have one entry
        assertEquals(1, RubyRegexp.quotedPatternCache.size());

//        // attempt to trigger GC and clean up cache for ten seconds
//        long time = System.currentTimeMillis();
//        while ((System.currentTimeMillis() - time) < 10000) {
//            Thread.yield();
//            System.gc();
//            if (RubyRegexp.quotedPatternCache.size() == 0) break;
//        }

        // Should be no retained references once cleaned
        //assertEquals(0, RubyRegexp.quotedPatternCache.size());
    }

    public void testByteListCacheKeySharing() {
        RubyString str = RubyString.newString(runtime, "regexp");
        ByteList strBytes = str.getByteList();

        RubyRegexp.patternCache.clear();

        // Should be the same object if cached
        RubyRegexp regexp = new RubyRegexp(runtime);
        regexp.initialize_m(str); // Regexp.new(str)

        assertEquals(1, RubyRegexp.patternCache.size());

        assertNotNull( RubyRegexp.patternCache.get(strBytes) );

        // str[0] = 'R'
        str.op_aset(runtime.getCurrentContext(), runtime.newFixnum(0), RubyString.newString(runtime, "R"));

        assertEquals(ByteList.create("Regexp"), strBytes);

        strBytes.invalidate(); // force hash recalculation

        assertNull( RubyRegexp.patternCache.get(strBytes) );
        assertNull( RubyRegexp.patternCache.get( ByteList.create("Regexp") ) );
        assertNotNull( RubyRegexp.patternCache.get( ByteList.create("regexp") ) );

        RubyRegexp.newRegexp(runtime, ByteList.create("regexp"));
        assertEquals(1, RubyRegexp.patternCache.size());

        RubyRegexp.newRegexp(runtime, ByteList.create("Regexp"));
        assertEquals(2, RubyRegexp.patternCache.size());
    }

}
