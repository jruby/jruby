package org.jruby.util;

import java.nio.channels.Channel;
import java.util.Arrays;

import jnr.constants.platform.OpenFlags;
import org.jruby.Ruby;

import junit.framework.TestCase;

public class URLResourceTest extends TestCase {

    public void testDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create((Ruby) null, "uri:" + uri, false);

        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertTrue(resource.isDirectory());
        assertTrue(resource.exists());
        assertEquals(Arrays.asList(resource.list()),
                Arrays.asList(new String[] {".", "dir_without_listing", "dir_with_listing"}));
    }

    public void testDirectoryWithTrailingSlash(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create((Ruby) null, "uri:" + uri + "/", false);

        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertTrue(resource.isDirectory());
        assertTrue(resource.exists());
        assertEquals(Arrays.asList(resource.list()),
                Arrays.asList(new String[] {".", "dir_without_listing", "dir_with_listing"}));
    }

    public void testNoneDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/dir_without_listing" ).toExternalForm();
        FileResource resource = URLResource.create((Ruby) null, "uri:" + uri, false);

        assertNotNull(resource );
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }

    public void testFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/.jrubydir" ).toExternalForm();
        FileResource resource = URLResource.create((Ruby) null, "uri:" + uri, false);

        assertNotNull(resource );
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }

    public void testNonExistingFile() throws Throwable {
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        String pathname = "uri:" + uri + "/not_there";
        FileResource resource = URLResource.create(null, pathname, false);

        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertFalse(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());

        try {
            resource.openChannel(OpenFlags.O_RDONLY.intValue(), 0x600);
            fail("non-existing resource should not produce a Channel");
        } catch (ResourceException.NotFound nf) {
            assertEquals(nf.getPath(), resource.absolutePath());
            assertTrue(nf.getMessage().contains(resource.absolutePath()));
        }

        try {
            resource.openInputStream();
            fail("non-existing resource should not produce an InputStream");
        } catch (ResourceException.NotFound nf) {
            assertEquals(nf.getPath(), resource.absolutePath());
            assertTrue(nf.getMessage().contains(resource.absolutePath()));
        }
    }

    public void testDirectoryClassloader() {
        FileResource resource = URLResource.create((Ruby) null,
                "uri:classloader:/somedir", false);

        assertNotNull(resource);
        assertFalse(resource.isFile());
        assertTrue(resource.isDirectory());
        assertTrue(resource.exists());
        assertEquals(Arrays.asList(resource.list()),
                Arrays.asList(new String[]{".", "dir_without_listing",
                        "dir_with_listing"}));
    }

    public void testDirectoryWithTrailingClassloader()
    {
        FileResource resource = URLResource.create((Ruby) null,
                "uri:classloader:/somedir/", false);

        assertNotNull(resource);
        assertFalse(resource.isFile());
        assertTrue(resource.isDirectory());
        assertTrue(resource.exists());
        assertEquals(Arrays.asList(resource.list()),
                Arrays.asList(new String[]{".", "dir_without_listing",
                        "dir_with_listing"}));
    }

    public void testNoneDirectoryClassloader()
    {
        FileResource resource = URLResource.create((Ruby) null,
                "uri:classloader:/somedir/dir_without_listing", false);

        assertNotNull( resource );
        assertFalse( resource.isFile() );
        assertTrue( resource.exists() );
        assertTrue( resource.isDirectory() );
        assertEquals( Arrays.asList( resource.list() ),
                      Arrays.asList( new String[] { ".", "..", ".empty" } ) );
    }

    public void testFileClassloader()
    {
        FileResource resource = URLResource.create((Ruby) null,
                "uri:classloader:/somedir/.jrubydir", true );

        assertNotNull( resource );
        assertTrue( resource.isFile() );
        assertTrue( resource.exists() );
        assertFalse( resource.isDirectory() );
        assertNull( resource.list() );
    }

    public void testNonExistingFileClassloader()
    {
        FileResource resource = URLResource.create((Ruby) null,
                "uri:classloader:/somedir/not_there", false );

        assertNotNull( resource );
        assertFalse( resource.isFile() );
        assertFalse( resource.exists() );
        assertFalse( resource.isDirectory() );
        assertNull( resource.list() );
    }
}
