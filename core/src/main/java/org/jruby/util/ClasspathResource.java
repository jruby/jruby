package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jnr.posix.FileStat;
import jnr.posix.POSIX;

import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

public class ClasspathResource implements FileResource {

    public static final String CLASSPATH = "classpath:/";

    private final String uri;

    private String[] list = null;
    
    private final JarFileStat fileStat;

    ClasspathResource(String uri, InputStream is, String[] files)
    {
        this.uri = uri;
        this.fileStat = new JarFileStat(this);
        this.list = files;
    }

    public static URL getResourceURL(String pathname) {
        String path = pathname.substring(CLASSPATH.length() );
        // this is the J2EE case
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if ( url != null ) {
            return url;
        }
        else if (ClasspathResource.class.getClassLoader() != null) {
            // this is OSGi case
            return ClasspathResource.class.getClassLoader().getResource(path);
        }
        return null;
    }
    
    public static FileResource create(String pathname) {
        if (!pathname.startsWith(CLASSPATH)) {
            return null;
        }
        
        URL url = getResourceURL(pathname);
        if (url != null) {
            try
            {
                // TODO open stream on demand
                return new ClasspathResource(pathname, url.openStream(), null);
            }
            catch (IOException e)
            {
               return null;
            }
        }
        return null;
    }

    @Override
    public String absolutePath()
    {
        return uri;
    }

    @Override
    public boolean exists()
    {
        return true;
    }

    @Override
    public boolean isDirectory()
    {
        return list != null;
    }

    @Override
    public boolean isFile()
    {
        return list == null;
    }

    @Override
    public long lastModified()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public long length()
    {
        // TODO Auto-generated method stub
        return 0;
    }

    @Override
    public boolean canRead()
    {
        return true;
    }

    @Override
    public boolean canWrite()
    {
        return false;
    }

    @Override
    public String[] list()
    {
        return list;
    }

    @Override
    public boolean isSymLink()
    {
        return false;
    }

    @Override
    public FileStat stat(POSIX posix) {
        return fileStat;
    }

    @Override
    public FileStat lstat(POSIX posix) {
      // we don't have symbolic links here, so lstat is no different than regular stat
      return stat(posix);
    }

    @Override
    public JRubyFile hackyGetJRubyFile()
    {
        // TODO Auto-generated method stub
        return null;
    }

    @Override
    public InputStream openInputStream()
    {
        try {
            return getResourceURL(uri).openStream();
        } catch (IOException ioE) {
            return null;
        }
    }

    @Override
    public ChannelDescriptor openDescriptor( ModeFlags flags, POSIX posix,
                                             int perm )
            throws ResourceException
    {
        return null;
    }
    
}
