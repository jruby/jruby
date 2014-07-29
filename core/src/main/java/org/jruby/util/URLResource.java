package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.channels.Channel;

import jnr.posix.FileStat;
import jnr.posix.POSIX;

import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

class URLResource implements FileResource {

    private final URL url;

    private final JarFileStat fileStat;

    URLResource(URL url) {
        this.url = url;
        this.fileStat = new JarFileStat(this);
    }
    
    @Override
    public String absolutePath()
    {
        return url.toExternalForm();
    }

    @Override
    public boolean exists()
    {
        return true;
    }

    @Override
    public boolean isDirectory()
    {
        return false;
    }

    @Override
    public boolean isFile()
    {
        return true;
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
        return null;
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
      // jars don't have symbolic links, so lstat is no different than regular stat
      return stat(posix);
    }
 
    @Override
    public JRubyFile hackyGetJRubyFile()
    {
        return null;
    }

    @Override
    public InputStream getInputStream()
    {
        try
        {
            return url.openStream();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    @Override
    public ChannelDescriptor openDescriptor(ModeFlags flags, POSIX posix, int perm)
            throws ResourceException
    {
        return null;
    }

    @Override
    public Channel openChannel( ModeFlags flags, POSIX posix, int perm )
            throws ResourceException
    {
        // TODO Auto-generated method stub
        return null;
    }

    public static FileResource create(String pathname)
    {
        URL url;
        try
        {
            url = new URL(pathname.replaceAll("([^:])//", "$1/"));
            // we do not want to deal with those url here like this though they are valid url/uri
            if (url.getProtocol().startsWith("http") || url.getProtocol().equals("file")|| url.getProtocol().equals("jar")){
                return null;
            }   
            // make sure we can also open the stream
            url.openStream();
            return new URLResource(url);
        }
        catch (IOException e)
        {
            return null;
        }
    }
}
