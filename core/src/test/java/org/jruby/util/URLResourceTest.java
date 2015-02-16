package org.jruby.util;

import java.util.Arrays;

import org.jruby.Ruby;

import junit.framework.TestCase;

public class URLResourceTest extends TestCase {
    
    public void testDirectory(){        
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:" + uri);
        
        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertTrue(resource.isDirectory());
        assertTrue(resource.exists());
        assertEquals(Arrays.asList(resource.list()), 
                     Arrays.asList(new String[] {".", "dir_without_listing", "dir_with_listing"}));
    }

    public void testNoneDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/dir_without_listing" ).toExternalForm();
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:" + uri);

        assertNotNull(resource );
        // you can open streams on file-system directories
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }

    public void testFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/.jrubydir" ).toExternalForm();
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:" + uri);
        
        assertNotNull(resource );
        // you can open streams on file-system directories
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }
    
    public void testNonExistingFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:" + uri + "/not_there");
        
        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertFalse(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }

    public void testDirectoryClassloader()
    {
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:classloader:/somedir");

        assertNotNull( resource );
        assertFalse( resource.isFile() );
        assertTrue( resource.isDirectory() );
        assertTrue( resource.exists() );
        assertEquals( Arrays.asList( resource.list() ),
                      Arrays.asList( new String[] { ".", "dir_without_listing",
                                                   "dir_with_listing" } ) );
    }

    public void testNoneDirectoryClassloader()
    {
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:classloader:/somedir/dir_without_listing");

        assertNotNull( resource );
        // you can open streams on file-system directories
        assertTrue( resource.isFile() );
        assertTrue( resource.exists() );
        assertFalse( resource.isDirectory() );
        assertNull( resource.list() );
    }

    public void testFileClassloader()
    {
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:classloader:/somedir/.jrubydir" );

        assertNotNull( resource );
        // you can open streams on file-system directories
        assertTrue( resource.isFile() );
        assertTrue( resource.exists() );
        assertFalse( resource.isDirectory() );
        assertNull( resource.list() );
    }

    public void testNonExistingFileClassloader()
    {
        FileResource resource = URLResource.create(Ruby.getGlobalRuntime(), "uri:classloader:/somedir/not_there" );

        assertNotNull( resource );
        assertFalse( resource.isFile() );
        assertFalse( resource.exists() );
        assertFalse( resource.isDirectory() );
        assertNull( resource.list() );
    }
}