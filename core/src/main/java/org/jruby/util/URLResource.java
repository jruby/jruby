package org.jruby.util;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.file.attribute.FileTime;
import java.util.*;

import jnr.constants.platform.Errno;
import jnr.posix.FileStat;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig;

public class URLResource implements FileResource, DummyResourceStat.FileResourceExt {

    public static final String URI = "uri:";
    public static final String CLASSLOADER = "classloader:";
    public static final String URI_CLASSLOADER = URI + CLASSLOADER;

    private final String uri;

    private final String[] list;

    private final URL url;
    private final String pathname;

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
    }

    @Override
    public String absolutePath() {
        return uri;
    }

    @Override
    public String canonicalPath() {
        return uri;
    }

    @Override
    public String path() {
        return uri;
    }

    @Override
    public boolean exists() {
        return url != null || pathname != null || list != null;
    }

    @Override
    public boolean isDirectory() {
        return list != null;
    }

    @Override
    public boolean isFile() {
        return list == null && (url != null || pathname != null);
    }

    @Override
    public long length() {
        long totalRead = 0;
        InputStream is = null;

        try {
            is = openInputStream();
            byte[] buf = new byte[8096];
            int amountRead;

            while ((amountRead = is.read(buf)) != -1) {
                totalRead += amountRead;
            }

            is.close();
        }
        catch (IOException e) { close(is); }

        return totalRead;
    }

    @Override
    public boolean canRead() { return exists(); }

    @Override
    public boolean canWrite() { return false; }

    @Override
    public boolean canExecute() { return false; }

    @Override
    public String[] list() { return list; }

    @Override
    public boolean isSymLink() { return false; }

    @Override
    public long lastModified() {
        return 0L; // not implemented - problematic for a classpath resource
    }

    public FileTime lastModifiedTime() {
        return null; // not implemented - problematic for a classpath resource
    }

    public FileTime lastAccessTime() {
        return null; // not implemented - problematic for a classpath resource
    }

    public FileTime creationTime() {
        return null; // not implemented - problematic for a classpath resource
    }

    @Override
    public int errno() {
        return Errno.ENOENT.intValue();
    }

    @Override
    public FileStat stat() {
        return new DummyResourceStat(this);
    }

    @Override
    public FileStat lstat() {
        return stat(); // URLs don't have symbolic links, so lstat == stat
    }

    @Override
    public <T> T unwrap(Class<T> type) throws UnsupportedOperationException {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream openInputStream() throws IOException {
    	if (pathname != null) {
            return cl.getResourceAsStream(pathname);
    	}
    	if (url == null) {
            throw new ResourceException.NotFound(absolutePath());
        }
    	return url.openStream();
    }

    @Override
    public Channel openChannel( int flags, int perm ) throws IOException {
        return Channels.newChannel(openInputStream());
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof URLResource) {
            return uri.equals(((URLResource) obj).uri);
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 17 * uri.hashCode();
    }

    @Override
    public String toString() {
        return getClass().getName() + '{' + absolutePath() + '}';
    }

    public static FileResource createClassloaderURI(Ruby runtime, String pathname, boolean asFile) {
        ClassLoader cl = runtime != null ? runtime.getJRubyClassLoader() : RubyInstanceConfig.defaultClassLoader();
        try {
            pathname = new URI(pathname.replaceFirst("^/*", "/"))
                    .normalize().getPath().replaceAll("^/([.][.]/)*", "");
        }
        catch (URISyntaxException e) {
            pathname = pathname.replaceAll("^[.]?/*", "");
        }
        final URL url = cl.getResource(pathname);
        String[] files = null;
        if (!asFile) {
            files = listClassLoaderFiles(cl, pathname);
            if (files == null) { // no .jrubydir found
                boolean isDirectory = false;
                // we do not want double entries
                Set<String> list = new LinkedHashSet<>();
                list.add("."); list.add("..");
                try {
                    // first look at the enum from the classloader
                    // may or may not contain directory entries
                    Enumeration<URL> urls = cl.getResources(pathname);
                    while (urls.hasMoreElements()){
                        isDirectory = addDirectoryEntries(list, urls.nextElement(), isDirectory);
                    }
                    if (runtime != null) {
                        // we have a runtime, so look at the JRubyClassLoader
                        // and its parent classloader
                        isDirectory = addDirectoriesFromClassloader(cl, list, pathname, isDirectory);
                        isDirectory = addDirectoriesFromClassloader(cl.getParent(), list, pathname, isDirectory);
                    }
                    else {
                        // just look at what we have
                        isDirectory = addDirectoriesFromClassloader(cl, list, pathname, isDirectory);
                    }
                    if (isDirectory) files = list.toArray(new String[list.size()]);
                }
                catch (IOException e) { /* we tried */ }
            }
        }
        return new URLResource(URI_CLASSLOADER + '/' + pathname, cl, url == null ? null : pathname, files);
    }

    private static boolean addDirectoriesFromClassloader(ClassLoader cl, Set<String> list, String pathname, boolean isDirectory) throws IOException {
        if (cl instanceof URLClassLoader ) {
            for (URL u : ((URLClassLoader) cl).getURLs()) {
                if (u.getFile().endsWith(".jar") && u.getProtocol().equals("file")) {
                    u = new URL("jar:" + u + "!/" + pathname);
                    isDirectory = addDirectoryEntries(list, u, isDirectory);
                }
            }
        }
        return isDirectory;
    }

    private static boolean addDirectoryEntries(Set<String> entries, URL url, boolean isDirectory) {
        switch (url.getProtocol()) {
        case "jar":
            // maybe the jar itself contains directory entries (which are actually optional)
            FileResource jar = JarResource.create(url.toString());
            if (jar != null && jar.isDirectory()) {
                if (!isDirectory) isDirectory = true;
                entries.addAll(Arrays.asList(jar.list()));
            }
            break;
        case "file":
            // let's look on the filesystem
            File file = new File(url.getPath());
            if (file.isDirectory()) {
                if (!isDirectory) isDirectory = true;
                entries.addAll(Arrays.asList(file.list()));
            }
            break;
        default:
        }
        return isDirectory;
    }

    public static FileResource create(Ruby runtime, String pathname, boolean asFile) {
        if (!pathname.startsWith(URI)) {
            return null;
        }
        // GH-2005 needs the replace
        pathname = pathname.substring(URI.length()).replace('\\', '/');
        if (pathname.startsWith(CLASSLOADER)) {
            return createClassloaderURI(runtime, pathname.substring(CLASSLOADER.length()), asFile);
        }
        return createRegularURI(pathname, asFile);
    }

    private static FileResource createRegularURI(String pathname, boolean asFile) {
        URL url;
        try {
            // TODO NormalizedFile does too much - should leave uri: files as they are
            // and make file:/a protocol to be file:///a so the second replace does not apply
            pathname = pathname.replaceFirst( "file:/([^/])", "file:///$1" );
            pathname = pathname.replaceFirst( ":/([^/])", "://$1" );

            url = new URL(pathname);
            // we do not want to deal with those url here like this though they are valid url/uri
            if (url.getProtocol().startsWith("http")) return null;
        }
        catch (MalformedURLException e) { // file does not exists
            return new URLResource(URI + pathname, null, null);
        }
        String[] files = asFile ? null : listFiles(pathname);
        if (files != null) {
            return new URLResource(URI + pathname, null, files);
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
        catch (IOException e) { // can not open stream - treat it as not existing file
            return new URLResource(URI + pathname, null, null);
        }
    }

    private static String[] listFilesFromInputStream(InputStream is) {
        BufferedReader reader = null;
        try {
            List<String> files = new ArrayList<>();
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
            close(reader);
        }
    }

    private static String[] listClassLoaderFiles(ClassLoader classloader, String pathname) {
        try {
            String path = pathname + (pathname.isEmpty() || pathname.endsWith("/") ? ".jrubydir" : "/.jrubydir");
            Enumeration<URL> urls = classloader.getResources(path);
            if (!urls.hasMoreElements()) return null;

            Set<String> result = new LinkedHashSet<>();
            while (urls.hasMoreElements()) {
                URL url = urls.nextElement();
                for (String entry: listFilesFromInputStream(url.openStream())) {
                    if (!result.contains(entry)) result.add(entry);
                }
            }
            return result.toArray(new String[result.size()]);
        }
        catch (IOException e) {
            return null;
        }
    }

    private static String[] listFiles(String pathname) {
        try {
            InputStream is = new URL(pathname + "/.jrubydir").openStream();
            // no inputstream happens with knoplerfish OSGI and osgi tests from /maven/jruby-complete
            if (is != null) {
                return listFilesFromInputStream(is);
            }
            return null;
        }
        catch (IOException e) {
            return null;
        }
    }

    private static void close(final Closeable resource) {
        if (resource != null) {
            try { resource.close(); } catch (IOException ex) {}
        }
    }

    public static URL getResourceURL(Ruby runtime, String location) {
        if (location.startsWith(URI_CLASSLOADER)) {
            return runtime.getJRubyClassLoader().getResource(location.substring(URI_CLASSLOADER.length() + 1));
        }
        try {
            return new URL(location.replaceFirst("^" + URI, ""));
        }
        catch (MalformedURLException e) {
            throw new AssertionError("BUG in " + URLResource.class, e);
        }
    }

}
