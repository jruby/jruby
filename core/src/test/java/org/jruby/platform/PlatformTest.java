package org.jruby.platform;

import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeTrue;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Test;

public class PlatformTest {
    
    private String osName;

    @Before
    public void setUp() {
        osName = System.getProperty("os.name", "unknown").toLowerCase();
    }

    @Test
    public void testIsMac() {
        assumeTrue(osName.indexOf("mac") != -1);
        assertTrue(Platform.getPlatform() instanceof UnixPlatform);
    }
    @Test
    public void testIsWindows() {
        assumeTrue(osName.indexOf("windows") != -1);
        assertTrue(Platform.getPlatform() instanceof NTPlatform);
    }

    @Test
    public void testIsSolaris() {
        assumeTrue(osName.indexOf("sunos") != -1);
        assertTrue(Platform.getPlatform() instanceof UnixPlatform);
    }

    @Test
    public void testIsLinux() {
        assumeTrue(osName.indexOf("linux") != -1);
        assertTrue(Platform.getPlatform() instanceof UnixPlatform);
    }
}
