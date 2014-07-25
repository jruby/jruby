package org.jruby.util;

import java.net.URL;

class ClasspathResource extends URLResource {

    private static final String CLASSPATH = "classpath:/";

    ClasspathResource(URL url)
    {
        // this effectively switch from "classpath:" protocol to whatever the claasloader delivers
        super(url);
    }

    public static FileResource create(String pathname)
    {
        if (!pathname.startsWith(CLASSPATH)) {
            return null;
        }
        
        String path = pathname.substring(CLASSPATH.length() );
        // this is the J2EE case
        URL url = Thread.currentThread().getContextClassLoader().getResource( path );
        if (url == null && ClasspathResource.class.getClassLoader() != null) {
            // this is OSGi case
            url = ClasspathResource.class.getClassLoader().getResource( path );                
        }
        if (url != null) {
            return new ClasspathResource(url);
        }
        return null;
    }
    
}