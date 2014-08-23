package org.jruby.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jnr.posix.FileStat;
import jnr.posix.POSIX;

import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;

public class URLResource implements FileResource {

    public static String URI = "uri:";
    public static String CLASSLOADER = "classloader:/";
    public static String URI_CLASSLOADER = URI + CLASSLOADER;

    private final String uri;

    private final String[] list;

    private final URL url;
    private final String pathname;

    private final JarFileStat fileStat;

    URLResource(String uri, URL url, String[] files) {
        this(uri, url, null, files);
    }
    
    URLResource(String uri, String pathname, String[] files) {
        this(uri, null, pathname, files);
    }
    
    private URLResource(String uri, URL url, String pathname, String[] files) {
        this.uri = uri;
        this.list = files;
        this.url = url;
        this.pathname = pathname;
        this.fileStat = new JarFileStat(this);
    }
    
    @Override
    public String absolutePath()
    {
        return uri;
    }

    public String canonicalPath() {
        return uri;
    }

    @Override
    public boolean exists()
    {
        return url != null || pathname != null || list != null;
    }

    @Override
    public boolean isDirectory()
    {
        return list != null;
    }

    @Override
    public boolean isFile()
    {
        return list == null && (url != null || pathname != null);
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
    public FileStat stat() {
        return fileStat;
    }

    @Override
    public FileStat lstat() {
      return stat(); // URLs don't have symbolic links, so lstat == stat
    }
 
    @Override
    public JRubyFile hackyGetJRubyFile() {
        return JRubyNonExistentFile.NOT_EXIST;
    }

    @Override
    public InputStream openInputStream()
    {
        try
        {
            if (pathname != null) {
                return Thread.currentThread().getContextClassLoader().getResourceAsStream(pathname);
            }
            return url.openStream();
        }
        catch (IOException e)
        {
            return null;
        }
    }

    @Override
    public ChannelDescriptor openDescriptor(ModeFlags flags, int perm) throws ResourceException {
        if (!exists()) {
            throw new ResourceException.NotFound(absolutePath());
        }
        return new ChannelDescriptor(openInputStream(), flags);
    }

    public static FileResource createClassloaderURI(String pathname) {
        if (pathname.contains("..")) {
            try
            {
                pathname = new File(pathname).getCanonicalPath().replace( new File("").getCanonicalPath(), "" );
            }
            catch (IOException e) {}
        }
        if (pathname.startsWith("/")) {
            pathname = pathname.substring(1);
        }
        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream(pathname);
        if (is != null) {
            try
            {
                is.close();
            }
            // need Exception here due to strange NPE in some cases
            catch (Exception e) {}
        }
        String[] files = listClassLoaderFiles(pathname);
        return new URLResource(URI_CLASSLOADER + pathname,
                               is == null ? null : pathname,
                               files);
    }

    public static FileResource create(String pathname)
    {
        if (!pathname.startsWith(URI)) {
            return null;
        }
        pathname = pathname.substring(URI.length());
        if (pathname.startsWith(CLASSLOADER)) {
            return createClassloaderURI(pathname.substring(CLASSLOADER.length()));
        }
        return createRegularURI(pathname);
    }
    
    private static FileResource createRegularURI(String pathname) {
        URL url;
        try
        {
            // TODO NormalizedFile does too much - should leave uri: files as they are
            pathname = pathname.replaceFirst( ":/([^/])", "://$1" );
            url = new URL(pathname);
            // we do not want to deal with those url here like this though they are valid url/uri
            if (url.getProtocol().startsWith("http")){
                return null;
            }
        }
        catch (MalformedURLException e)
        {
            // file does not exists
            return new URLResource(URI + pathname, (URL)null, null);
        }
        String[] files = listFiles(pathname);
        if (files != null) {
            return new URLResource(URI + pathname, (URL)null, files);
        }
        try {
            url.openStream().close();
            return new URLResource(URI + pathname, url, null);
        }
        catch (IOException e)
        {
            // can not open stream - treat it as not existing file
            return new URLResource(URI + pathname, (URL)null, null);
        }
    }

    private static String[] listFilesFromInputStream(InputStream is) {
        BufferedReader reader = null;
        try {
            List<String> files = new LinkedList<String>();
            reader = new BufferedReader(new InputStreamReader(is));
            String line = reader.readLine();
            while (line != null) {
                files.add(line);
                line = reader.readLine();
            }
            return files.toArray(new String[files.size()]);
        }
        catch (IOException e) {
            return null;
        }
        finally {
            if (reader != null) {
                try
                {
                    reader.close();
                }
                catch (IOException ignored)
                {
                }
            }
        }
    }
    private static String[] listClassLoaderFiles(String pathname) {
        try
        {
            Enumeration<URL> urls = Thread.currentThread().getContextClassLoader().getResources(pathname + "/.jrubydir");
            if (!urls.hasMoreElements()) {
                return null;
            }
            Set<String> result = new LinkedHashSet<String>();
            while( urls.hasMoreElements() ) {
                URL url = urls.nextElement();
                for( String entry: listFilesFromInputStream(url.openStream())) {
                    if (!result.contains(entry)) {
                        result.add(entry);
                    }
                }
            }
            return result.toArray(new String[result.size()]);
        }
        catch (IOException e)
        {
            return null;
        }
    }

    private static String[] listFiles(String pathname) {
        try
        {
            return listFilesFromInputStream(new URL(pathname.replace("file://", "file:/") + "/.jrubydir").openStream());
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public static URL getResourceURL(String location)
    {
        if (location.startsWith(URI + CLASSLOADER)){
            return Thread.currentThread().getContextClassLoader().getResource(location.substring(URI_CLASSLOADER.length()));
        }
        try
        {
            return new URL(location.replaceFirst("^" + URI, ""));
        }
        catch (MalformedURLException e)
        {
            throw new RuntimeException("BUG in " + URLResource.class);
        }
    }
    
}
