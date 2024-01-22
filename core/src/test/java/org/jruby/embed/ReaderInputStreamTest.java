/*
 **** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2009-2017 Yoko Harada <yokolet@gmail.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.embed;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import org.jruby.embed.io.ReaderInputStream;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Yoko Harada &lt;<a href="mailto:yokolet@gmail.com">yokolet@gmail.com</a>&gt;
 */
public class ReaderInputStreamTest {
    private String basedir = new File(System.getProperty("user.dir")).getParent();
    private String filename = basedir + "/core/src/test/ruby/org/jruby/embed/ruby/readertest.rb";
    private String filename2 = basedir + "/core/src/test/ruby/test_yaml.rb";

    static Logger logger0 = Logger.getLogger(MultipleScriptsRunner.class.getName());
    static Logger logger1 = Logger.getLogger(MultipleScriptsRunner.class.getName());
    static OutputStream outStream = null;

    public ReaderInputStreamTest() {
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
    }

    @Before
    public void setUp() throws FileNotFoundException {
        outStream = new FileOutputStream(basedir + "/core/target/run-junit-embed.log", true);
        Handler handler = new StreamHandler(outStream, new SimpleFormatter());
        logger0.addHandler(handler);
        logger0.setUseParentHandlers(false);
        logger0.setLevel(Level.INFO);
        logger1.setUseParentHandlers(false);
        logger1.addHandler(new ConsoleHandler());
        logger1.setLevel(Level.WARNING);
    }

    @After
    public void tearDown() {
    }

    /**
     * Test of available method, of class ReaderInputStream.
     */
    @Test
    public void testAvailable() throws Exception {
        logger1.info("available");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        int expResult = 40;
        int result = instance.available();
        assertEquals(expResult, result);
        instance.close();
    }

    /**
     * Test of close method, of class ReaderInputStream.
     */
    @Test
    public void testClose() throws Exception {
        logger1.info("close");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename2));
        instance.close();
    }

    /**
     * Test of mark method, of class ReaderInputStream.
     */
    @Test
    public void testMark() throws IOException {
        logger1.info("mark");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        int readlimit = 0;
        instance.mark(readlimit);
        instance.close();
    }

    /**
     * Test of markSupported method, of class ReaderInputStream.
     */
    @Test
    public void testMarkSupported() throws IOException {
        logger1.info("markSupported");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        boolean expResult = true;
        boolean result = instance.markSupported();
        assertEquals(expResult, result);
        instance.close();
    }

    /**
     * Test of read method, of class ReaderInputStream.
     */
    @Test
    public void testRead_0args() throws IOException {
        logger1.info("read 0args");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        int expResult = 0x70;  // is 'p' by UTF-8
        int result = instance.read();
        assertEquals(expResult, result);
        instance.close();
    }

    /**
     * Test of read method, of class ReaderInputStream.
     */
    @Test
    public void testRead_byteArr() throws IOException {
        logger1.info("read byteArr");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        byte[] b = new byte[4];
        int expResult = 4;
        int result = instance.read(b);
        assertEquals(expResult, result);
        assertEquals(0x70, b[0]);
        instance.close();
    }

    /**
     * Test of read method, of class ReaderInputStream.
     */
    @Test
    public void testRead_3args() throws IOException {
        logger1.info("read 3args");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        byte[] b = new byte[15];
        int off = 6;
        int len = b.length;
        int expResult = 15;
        int result = instance.read(b, off, len);
        assertEquals(expResult, result);
        instance.close();
    }

    /**
     * Test of read method, of class ReaderInputStream.
     */
    @Test
    public void testRead_3args_bigfile() throws Exception {
        logger1.info("read 3args big file");
        StringBuffer sb = new StringBuffer();
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename2));
        while (instance.available() > 0) {
            byte[] buf = new byte[1024];
            int size = instance.read(buf, 0, buf.length);
            if (size != -1) {
                sb.append(new String(buf));
            }
        }
        instance.close();
        String expected = Files.readString(Paths.get(filename2));
        assertEquals(expected.trim(), sb.toString().trim());
    }

    /**
     * Test of reset method, of class ReaderInputStream.
     */
    @Test
    public void testReset() throws Exception {
        logger1.info("reset");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        int readlimit = 0;
        instance.mark(readlimit);
        instance.reset();
        instance.close();
    }

    /**
     * Test of skip method, of class ReaderInputStream.
     */
    @Test
    public void testSkip() throws Exception {
        logger1.info("skip");
        ReaderInputStream instance = new ReaderInputStream(new FileReader(filename));
        long n = 0L;
        long expResult = 0L;
        long result = instance.skip(n);
        assertEquals(expResult, result);
        instance.close();
    }

}
