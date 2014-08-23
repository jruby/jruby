package org.jruby.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import jnr.posix.FileStat;
import jnr.posix.POSIX;

import org.jruby.Ruby;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

public class JRubyClassloaderResource implements FileResource {

    private final String uri;

    private final JarFileStat fileStat;

    private final InputStream is;

    // TODO maybe use URL from the JRubyClassLoader - jar: URI can be opened on openInputStream
    JRubyClassloaderResource(String uri, InputStream is)
    {
        this.uri = uri;
        this.is = is;
        this.fileStat = new JarFileStat(this);
    }

    public static FileResource create(Ruby runtime, String pathname) {
        if (pathname.contains("..")) {
            try
            {
                pathname = new File(pathname).getCanonicalPath().replace(new File("").getCanonicalPath(), "");
            }
            catch (IOException e) {}
        }
        URL url = runtime.getJRubyClassLoader().findResourceNoIndex(pathname);
        if (url == null) {
            if (pathname.startsWith( "/")) {
                pathname = pathname.substring(1);
            }
            else {
                pathname = "/" + pathname;
            }
            url = runtime.getJRubyClassLoader().findResourceNoIndex(pathname);
        }
        if (url != null) {
            try
            {
                return new JRubyClassloaderResource(pathname, url.openStream());
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
    public String canonicalPath() {
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
    public FileStat stat() {
        return fileStat;
    }

    @Override
    public FileStat lstat() {
      return stat(); // we don't have symbolic links here, so lstat == stat
    }

    @Override
    public JRubyFile hackyGetJRubyFile()
    {
        return JRubyNonExistentFile.NOT_EXIST;
    }

    @Override
    public InputStream openInputStream()
    {
        return is;
    }

    @Override
    public ChannelDescriptor openDescriptor(ModeFlags flags, int perm) throws ResourceException {
        return null;
    }
}
