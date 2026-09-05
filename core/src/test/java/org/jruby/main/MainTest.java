package org.jruby.main;

import java.util.Arrays;

import junit.framework.TestCase;

import org.jruby.platform.Platform;

public class MainTest extends TestCase {
    public void testPagerCommandPreservesOptions() {
        String pager = "less -RSs";

        if (Platform.IS_WINDOWS) {
            assertEquals(Arrays.asList("cmd.exe", "/c", pager), Arrays.asList(Main.pagerCommand(pager)));
        } else {
            assertEquals(Arrays.asList("/bin/sh", "-c", pager), Arrays.asList(Main.pagerCommand(pager)));
        }
    }

    public void testPagerCommandPreservesQuotedExecutable() {
        String pager = "'/path with spaces/less' -R";

        assertEquals(pager, Main.pagerCommand(pager)[2]);
    }
}
