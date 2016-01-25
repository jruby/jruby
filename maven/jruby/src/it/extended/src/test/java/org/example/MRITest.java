package org.example;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;

public class MRITest extends BaseTest {

    @Test
    public void testMRI() throws Exception {
        runIt("mri", "ENV['EXCLUDE_DIR']='test/mri/excludes';");
    }
}
