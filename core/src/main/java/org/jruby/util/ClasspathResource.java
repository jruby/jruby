package org.jruby.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.nio.channels.Channel;
import java.util.LinkedList;
import java.util.List;

import jnr.posix.FileStat;
import jnr.posix.POSIX;

import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

public class ClasspathResource implements FileResource {

    public static final String CLASSPATH = "classpath:/";

    private final String uri;

    private final InputStream is;
    
    private final String[] list;
    
    private final JarFileStat fileStat;

    ClasspathResource(String uri, InputStream is, String[] files)
    {
        this.uri = uri;
        this.is = is;
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
        
        String[] files = listFiles(pathname);
        if (files != null) {
            return new ClasspathResource(pathname, null, files);
        }
        URL url = getResourceURL(pathname);
        if (url != null) {
            try
            {
                return new ClasspathResource(pathname, url.openStream(), null);
            }
            catch (IOException e)
            {
               return null;
            }
        }
        return null;
    }

    private static String[] listFiles(String pathname) {
        pathname = pathname + ( pathname.endsWith( "/" ) ? "" : "/" ) + ".jrubydir";
        URL url = getResourceURL(pathname);
        if (url == null) {
            return null;
        }
        return URLResource.listFilesFromURL( url );
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
    public InputStream getInputStream()
    {
        return is;
    }

    @Override
    public ChannelDescriptor openDescriptor( ModeFlags flags, POSIX posix,
                                             int perm )
            throws ResourceException
    {
        return null;
    }

    @Override
    public Channel openChannel( ModeFlags flags, POSIX posix, int perm )
            throws ResourceException
    {
        return null;
    }
    
}