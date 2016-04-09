package org.jruby.util;

import java.util.Arrays;

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

    public void testNonExistingFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create((Ruby) null, "uri:" + uri + "/not_there", false);

        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertFalse(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
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
                        "dir_with_listing", ".."}));
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
                        "dir_with_listing", ".."}));
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
