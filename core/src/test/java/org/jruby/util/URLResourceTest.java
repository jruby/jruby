package org.jruby.util;

import java.util.Arrays;

import jnr.constants.platform.OpenFlags;
import org.jruby.Ruby;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

public class URLResourceTest {
    Ruby ruby;

    @Before
    public void setup() {
        ruby = Ruby.newInstance();
    }

    @After
    public void tearDown() {
        ruby.tearDown();
    }

    @Test
    public void testDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create(ruby, "uri:" + uri, false);

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertTrue(resource.isDirectory());
        Assert.assertTrue(resource.exists());
        Assert.assertEquals(Arrays.asList(resource.list()), Arrays.asList(new String[] {".", "dir_without_listing", "dir_with_listing"}));
    }

    @Test
    public void testDirectoryWithTrailingSlash(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create(ruby, "uri:" + uri + "/", false);

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertTrue(resource.isDirectory());
        Assert.assertTrue(resource.exists());
        Assert.assertEquals(Arrays.asList(resource.list()), Arrays.asList(new String[] {".", "dir_without_listing", "dir_with_listing"}));
    }

    @Test
    public void testNoneDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/dir_without_listing" ).toExternalForm();
        FileResource resource = URLResource.create(ruby, "uri:" + uri, false);

        Assert.assertNotNull(resource);
        Assert.assertTrue(resource.isFile());
        Assert.assertTrue(resource.exists());
        Assert.assertFalse(resource.isDirectory());
        Assert.assertNull(resource.list());
    }

    @Test
    public void testFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/.jrubydir" ).toExternalForm();
        FileResource resource = URLResource.create(ruby, "uri:" + uri, false);

        Assert.assertNotNull(resource);
        Assert.assertTrue(resource.isFile());
        Assert.assertTrue(resource.exists());
        Assert.assertFalse(resource.isDirectory());
        Assert.assertNull(resource.list());
    }

    @Test
    public void testNonExistingFile() throws Throwable {
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        String pathname = "uri:" + uri + "/not_there";
        FileResource resource = URLResource.create(ruby, pathname, false);

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertFalse(resource.exists());
        Assert.assertFalse(resource.isDirectory());
        Assert.assertNull(resource.list());

        try {
            resource.openChannel(OpenFlags.O_RDONLY.intValue(), 0x600);
            Assert.fail("non-existing resource should not produce a Channel");
        } catch (ResourceException.NotFound nf) {
            Assert.assertEquals(nf.getPath(), resource.absolutePath());
            Assert.assertTrue(nf.getMessage().contains(resource.absolutePath()));
        }

        try {
            resource.openInputStream();
            Assert.fail("non-existing resource should not produce an InputStream");
        } catch (ResourceException.NotFound nf) {
            Assert.assertEquals(nf.getPath(), resource.absolutePath());
            Assert.assertTrue(nf.getMessage().contains(resource.absolutePath()));
        }
    }

    @Test
    public void testDirectoryClassloader() {
        FileResource resource = URLResource.create(ruby,
                "uri:classloader:/somedir", false);

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertTrue(resource.isDirectory());
        Assert.assertTrue(resource.exists());
        Assert.assertEquals(Arrays.asList(resource.list()), Arrays.asList(new String[]{".", "dir_without_listing",
                "dir_with_listing"}));
    }

    @Test
    public void testDirectoryWithTrailingClassloader()
    {
        FileResource resource = URLResource.create(ruby,
                "uri:classloader:/somedir/", false);

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertTrue(resource.isDirectory());
        Assert.assertTrue(resource.exists());
        Assert.assertEquals(Arrays.asList(resource.list()), Arrays.asList(new String[]{".", "dir_without_listing",
                "dir_with_listing"}));
    }

    @Test
    public void testNoneDirectoryClassloader()
    {
        FileResource resource = URLResource.create(ruby,
                "uri:classloader:/somedir/dir_without_listing", false);

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertTrue(resource.exists());
        Assert.assertTrue(resource.isDirectory());
        Assert.assertEquals(Arrays.asList( resource.list() ), Arrays.asList( new String[] { ".", "..", ".empty" } ));
    }

    @Test
    public void testFileClassloader()
    {
        FileResource resource = URLResource.create(ruby,
                "uri:classloader:/somedir/.jrubydir", true );

        Assert.assertNotNull(resource);
        Assert.assertTrue(resource.isFile());
        Assert.assertTrue(resource.exists());
        Assert.assertFalse(resource.isDirectory());
        Assert.assertNull(resource.list());
    }

    @Test
    public void testNonExistingFileClassloader()
    {
        FileResource resource = URLResource.create(ruby,
                "uri:classloader:/somedir/not_there", false );

        Assert.assertNotNull(resource);
        Assert.assertFalse(resource.isFile());
        Assert.assertFalse(resource.exists());
        Assert.assertFalse(resource.isDirectory());
        Assert.assertNull(resource.list());
    }
}
