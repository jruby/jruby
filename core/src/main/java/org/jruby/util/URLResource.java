package org.jruby.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.util.Enumeration;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import jnr.posix.FileStat;

import org.jruby.Ruby;
import org.jruby.util.io.ModeFlags;

public class URLResource extends AbstractFileResource {

    public static String URI = "uri:";
    public static String CLASSLOADER = "classloader:";
    public static String URI_CLASSLOADER = URI + CLASSLOADER + "/";

    private final String uri;

    private final String[] list;

    private final URL url;
    private final String pathname;

    private final JarFileStat fileStat;
    private final ClassLoader cl;

    URLResource(String uri, URL url, String[] files) {
        this(uri, url, null, null, files);
    }

    URLResource(String uri, ClassLoader cl, String pathname, String[] files) {
        this(uri, null, cl, pathname, files);
    }

    private URLResource(String uri, URL url, ClassLoader cl, String pathname, String[] files) {
        this.uri = uri;
        this.list = files;
        this.url = url;
        this.cl = cl;
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
        return exists();
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
    InputStream openInputStream() throws IOException
    {
    	if (pathname != null) {
            return cl.getResourceAsStream(pathname);
    	}
    	return url.openStream();
    }

    @Override
    public Channel openChannel( ModeFlags flags, int perm ) throws ResourceException {
        return Channels.newChannel(inputStream());
    }

    public static FileResource create(ClassLoader cl, String pathname) {
      try
      {
          pathname = new URI(pathname.replaceFirst("^/*", "/")).normalize().getPath().replaceAll("^/([.][.]/)*", "");
      } catch (URISyntaxException e) {
          pathname = pathname.replaceAll("^[.]?/*", "");
      }
      URL url = cl.getResource(pathname);
      String[] files = listClassLoaderFiles(cl, pathname);
      return new URLResource(URI_CLASSLOADER + pathname,
                             cl,
                             url == null ? null : pathname,
                             files);
    }

    public static FileResource createClassloaderURI(Ruby runtime, String pathname) {
        return create(runtime.getJRubyClassLoader(), pathname);
    }

    public static FileResource create(Ruby runtime, String pathname)
    {
        if (!pathname.startsWith(URI)) {
            return null;
        }
        pathname = pathname.substring(URI.length());
        if (pathname.startsWith(CLASSLOADER)) {
            return createClassloaderURI(runtime, pathname.substring(CLASSLOADER.length()));
        }
        return createRegularURI(pathname);
    }

    private static FileResource createRegularURI(String pathname) {
        URL url;
        try
        {
            // TODO NormalizedFile does too much - should leave uri: files as they are
            // and make file:/a protocol to be file:///a so the second replace does not apply
            pathname = pathname.replaceFirst( "file:/([^/])", "file:///$1" );
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
            InputStream is = url.openStream();
            // no inputstream happens with knoplerfish OSGI and osgi tests from /maven/jruby-complete
            if (is != null) {
                is.close();
            }
            else {
                // there is no input-stream from this url
                url = null;
            }
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

    private static String[] listClassLoaderFiles(ClassLoader classloader, String pathname) {
        if (pathname.endsWith(".rb") || pathname.endsWith(".class") || pathname.endsWith(".jar")) {
            return null;
        }
        try
        {
            pathname = pathname + (pathname.equals("") ? ".jrubydir" : "/.jrubydir");
            Enumeration<URL> urls = classloader.getResources(pathname);
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
        if (pathname.endsWith(".rb") || pathname.endsWith(".class") || pathname.endsWith(".jar")) {
            return null;
        }
        try
        {
            InputStream is = new URL(pathname + "/.jrubydir").openStream();
            // no inputstream happens with knoplerfish OSGI and osgi tests from /maven/jruby-complete
            if (is != null) {
                return listFilesFromInputStream(is);
            }
            else {
                return null;
            }
        }
        catch (IOException e)
        {
            return null;
        }
    }

    public static URL getResourceURL(Ruby runtime, String location)
    {
        if (location.startsWith(URI + CLASSLOADER)){
            return runtime.getJRubyClassLoader().getResource(location.substring(URI_CLASSLOADER.length()));
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
