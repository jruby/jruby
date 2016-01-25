package org.example;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class JRubyTest extends BaseTest {

    @Test
    public void testJRuby() throws Exception {
        runIt("jruby");
    }
}
