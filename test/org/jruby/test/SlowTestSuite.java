package org.jruby.test;

import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * Test suite for "slow" tests, such as those that repeatedly launch subprocesses
 * or perform long IO or timed operations. Moved here to make the day-to-day
 * test suite faster, so people aren't tempted to skip it.
 */
public class SlowTestSuite extends TestSuite {
    public static final String TEST_INDEX = "slow_index";

    public static Test suite() throws Exception {
        return new TestUnitTestSuite(TEST_INDEX);
    }
}
