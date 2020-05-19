package org.jruby.util;

import junit.framework.TestCase;

public class JarResourceTest extends TestCase {
    
    public void testCreateJarResource(){
        String jar = Thread.currentThread().getContextClassLoader().getResource( "foobar.jar" ).toExternalForm();
        JarResource resource = JarResource.create( jar + "!/foo.rb" );
        assertNotNull( resource );
        resource = JarResource.create( jar + "!/f o.rb" );
        assertNotNull( resource );
        resource = JarResource.create( jar + "!/doesnotexist.rb" );
        assertNull( resource );
        resource = JarResource.create( jar.replace( ".jar", ".zip" ) + "!/foo.rb" );
        assertNull( resource );
    }
    
    public void testCreateJarResourceWithSpaceCharInPath(){
        String jar = Thread.currentThread().getContextClassLoader().getResource( "space bar/foobar.jar" ).toExternalForm();
        JarResource resource = JarResource.create( jar + "!/foo.rb" );
        assertNotNull( resource );
        resource = JarResource.create( jar + "!/f o.rb" );
        assertNotNull( resource );
        resource = JarResource.create( jar + "!/doesnotexist.rb" );
        assertNull( resource );
        resource = JarResource.create( jar.replace( ".jar", ".zip" ) + "!/foo.rb" );
        assertNull( resource );
    }
}