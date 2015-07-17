package org.example;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.io.File;
import java.io.StringWriter;
import java.net.URL;
import java.net.URLClassLoader;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import org.junit.Before;
import org.junit.Test;

public class MRITest extends BaseTest {

    @Test
    public void testMRI() throws Exception {
        runIt("mri", "ENV['EXCLUDE_DIR']='test/mri/excludes';");
    }
}
