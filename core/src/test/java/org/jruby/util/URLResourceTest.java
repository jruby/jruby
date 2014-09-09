package org.jruby.util;

import java.util.Arrays;

import junit.framework.TestCase;

public class URLResourceTest extends TestCase {
    
    public void testDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        // hmm not sure why the url from the classloader does not work :(
        FileResource resource = URLResource.create( null, "uri:" + uri.replace( "file://", "file:/" ));
        
        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertTrue(resource.isDirectory());
        assertTrue(resource.exists());
        assertEquals(Arrays.asList(resource.list()), 
                     Arrays.asList(new String[] {".", "dir_without_listing", "dir_with_listing"}));
    }

    public void testNoneDirectory(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/dir_without_listing" ).toExternalForm();
        // TODO once the URLResource does keep the protocol part of the uri as is we can remove this replace
        FileResource resource = URLResource.create( null, "uri:" + uri.replace( "file:/", "file:///" ));

        assertNotNull(resource );
        // you can open streams on file-system directories
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }

    public void testFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir/.jrubydir" ).toExternalForm();
        // TODO once the URLResource does keep the protocol part of the uri as is we can remove this replace
        FileResource resource = URLResource.create( null, "uri:" + uri.replace( "file:/", "file:///" ));
        
        assertNotNull(resource );
        // you can open streams on file-system directories
        assertTrue(resource.isFile());
        assertTrue(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }
    
    public void testNonExistingFile(){
        String uri = Thread.currentThread().getContextClassLoader().getResource( "somedir" ).toExternalForm();
        // TODO once the URLResource does keep the protocol part of the uri as is we can remove this replace
        FileResource resource = URLResource.create( null, "uri:" + uri.replace( "file:/", "file:///" ) + "/not_there");
        
        assertNotNull(resource );
        assertFalse(resource.isFile());
        assertFalse(resource.exists());
        assertFalse(resource.isDirectory());
        assertNull(resource.list());
    }
}