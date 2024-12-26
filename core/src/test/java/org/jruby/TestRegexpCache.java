package org.jruby;

import org.joni.Regex;
import org.jruby.test.Base;
import org.jruby.util.ByteList;
import org.jruby.util.RegexpOptions;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;

public class TestRegexpCache extends Base {

    // GH-2078
    public void testCacheRetention() {
        // final ByteList GH2078_TEST_BYTELIST = ByteList.create("GH2078");

        RubyRegexp.quotedPatternCache.clear();

        // Should be the same object if cached
        //Regex regex = RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST, RegexpOptions.NULL_OPTIONS, false);
        //assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST, RegexpOptions.NULL_OPTIONS, false));
        //assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(runtime, GH2078_TEST_BYTELIST.dup(), RegexpOptions.NULL_OPTIONS, false));
        RubyString str = newString(context, ByteList.create("GH2078"));

        Regex regex = RubyRegexp.getQuotedRegexpFromCache(context, str, RegexpOptions.NULL_OPTIONS);
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(context, str, RegexpOptions.NULL_OPTIONS));
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(context, (RubyString) str.dup(), RegexpOptions.NULL_OPTIONS));
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(context, newString(context, "GH2078"), RegexpOptions.NULL_OPTIONS));
        assertSame(regex, RubyRegexp.getQuotedRegexpFromCache(context, str.newFrozen(), RegexpOptions.NULL_OPTIONS));

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
        RubyString str = newString(context, "regexp");
        ByteList strBytes = str.getByteList();

        RubyRegexp.patternCache.clear();

        // Should be the same object if cached
        RubyRegexp regexp = new RubyRegexp(context.runtime);
        regexp.initialize_m(str); // Regexp.new(str)

        assertEquals(1, RubyRegexp.patternCache.size());

        assertNotNull( RubyRegexp.patternCache.get(strBytes) );

        // str[0] = 'R'
        str.op_aset(context, asFixnum(context, 0), newString(context, "R"));

        assertEquals(ByteList.create("Regexp"), strBytes);

        strBytes.invalidate(); // force hash recalculation

        assertNull( RubyRegexp.patternCache.get(strBytes) );
        assertNull( RubyRegexp.patternCache.get( ByteList.create("Regexp") ) );
        assertNotNull( RubyRegexp.patternCache.get( ByteList.create("regexp") ) );

        RubyRegexp.newRegexp(context.runtime, ByteList.create("regexp"));
        assertEquals(1, RubyRegexp.patternCache.size());

        RubyRegexp.newRegexp(context.runtime, ByteList.create("Regexp"));
        assertEquals(2, RubyRegexp.patternCache.size());
    }

}
