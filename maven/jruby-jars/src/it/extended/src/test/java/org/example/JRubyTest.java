package org.example;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.io.File;
import java.io.StringWriter;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import org.junit.Before;
import org.junit.Test;

public class JRubyTest extends BaseTest {

    @Test
    public void testJRuby() throws Exception {
        runIt("jruby");
    }
}
