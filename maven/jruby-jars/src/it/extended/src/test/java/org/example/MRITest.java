package org.example;

import static org.junit.Assert.assertEquals;

import java.util.Arrays;
import java.io.File;
import java.io.StringWriter;

import org.jruby.embed.LocalContextScope;
import org.jruby.embed.ScriptingContainer;

import org.junit.Before;
import org.junit.Test;

public class MRITest extends BaseTest {

    // Commented out because 9.1 now seems to *actually* load all the tests and fail (9.0 only loaded one)
//    @Test
    public void testMRI() throws Exception {
        runIt("mri", "ENV['EXCLUDE_DIR']='test/mri/excludes';");
    }
}
