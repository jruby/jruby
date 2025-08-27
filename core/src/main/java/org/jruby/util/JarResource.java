package org.jruby.util;

import jnr.posix.FileStat;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarEntry;

abstract class JarResource implements FileResource, DummyResourceStat.FileResourceExt {

    private static final JarCache jarCache = new JarCache();

    public static JarResource create(String pathname) {
        int bang = pathname.indexOf('!');
        if (bang == -1) return null;  // no ! no jar!

        if (pathname.startsWith("jar:")) {
            if (pathname.startsWith("file:", 4)) {
                pathname = pathname.substring(9); bang -= 9; // 4 + 5
            }
            else {
                pathname = pathname.substring(4); bang -= 4;
            }
        }
        else if (pathname.startsWith("file:")) {
            pathname = pathname.substring(5); bang -= 5;
        }

        String jarPath = pathname.substring(0, bang);
        String entryPath = pathname.substring(bang + 1);
        // normalize path -- issue #2017
        if (StringSupport.startsWith(entryPath, '/', '/')) entryPath = entryPath.substring(1);

        // special case: "jar:file:blah.jar!." is just "jar:file:blah.jar!"
        if (entryPath.equals(".")) entryPath = "";

        // TODO: Do we really need to support both test.jar!foo/bar.rb and test.jar!/foo/bar.rb cases?
        JarResource resource = createJarResource(jarPath, entryPath, false);

        if (resource == null && StringSupport.startsWith(entryPath, '/')) {
            resource = createJarResource(jarPath, entryPath.substring(1), true);
        }

        return resource;
    }

    private static JarResource createJarResource(String jarPath, String entryPath, boolean rootSlashPrefix) {
        JarCache.JarIndex index = jarCache.getIndex(jarPath);

        if (index == null) { // Jar doesn't exist
            try {
                jarPath = URLDecoder.decode(jarPath, "UTF-8");
                entryPath = URLDecoder.decode(entryPath, "UTF-8");
            }
            catch (IllegalArgumentException e) {
                // something in the path did not decode, so it's probably not a URI
                // See jruby/jruby#2264.
                return null;
            }
            catch (UnsupportedEncodingException e) {
                throw new AssertionError(e);
            }
            index = jarCache.getIndex(jarPath);

            if (index == null) return null; // Jar doesn't exist
        }

        // Try it as directory first, because jars tend to have foo/ entries
        // and it's not really possible disambiguate between files and directories.
        String[] entries = index.getDirEntries(entryPath);
        if (entries != null) {
            return new JarDirectoryResource(jarPath, rootSlashPrefix, entryPath, entries);
        }
        if (entryPath.length() > 1 && entryPath.endsWith("/")) {  // in case 'foo/' passed
            entries = index.getDirEntries(entryPath.substring(0, entryPath.length() - 1));

            if (entries != null) {
                return new JarDirectoryResource(jarPath, rootSlashPrefix, entryPath, entries);
            }
        }

        JarEntry jarEntry = index.getJarEntry(entryPath);
        if (jarEntry != null) {
            return new JarFileResource(jarPath, rootSlashPrefix, index, jarEntry);
        }

        return null;
    }

    public static void removeJarResource(String jarPath){
        jarCache.remove(jarPath);
    }

    private final CharSequence jarPrefix;

    JarResource(String jarPath, boolean rootSlashPrefix) {
        StringBuilder prefix = new StringBuilder(jarPath.length() + 2);
        prefix.append(jarPath).append('!');
        this.jarPrefix = rootSlashPrefix ? prefix.append('/') : prefix;
    }

    private transient String absolutePath;

    @Override
    public final String absolutePath() {
        String path = this.absolutePath;
        if (path != null) return path;
        return this.absolutePath = jarPrefix + entryName();
    }

    @Override
    public String canonicalPath() {
        return absolutePath();
    }

    @Override
    public String path() {
        return absolutePath();
    }

    @Override
    public boolean exists() {
        // If a jar resource got created, then it always corresponds to some kind of resource
        return true;
    }

    @Override
    public boolean canRead() {
        // Can always read from a jar
        return true;
    }

    @Override
    public boolean canWrite() {
        return false;
    }

    @Override
    public boolean canExecute() {
        return false;
    }

    @Override
    public boolean isSymLink() {
        // Jar archives shouldn't contain symbolic links, or it would break portability. `jar`
        // command behavior seems to comform to that (it unwraps syumbolic links when creating a jar
        // and replaces symbolic links with regular file when extracting from a zip that contains
        // symbolic links). Also see:
        // http://www.linuxquestions.org/questions/linux-general-1/how-to-create-jar-files-with-symbolic-links-639381/
        return false;
    }

    private transient FileStat fileStat;

    @Override
    public FileStat stat() {
        FileStat fileStat = this.fileStat;
        if (fileStat != null) return fileStat;
        return this.fileStat = new DummyResourceStat(this);
    }

    @Override
    public FileStat lstat() {
        return stat(); // jars don't have symbolic links, so lstat == stat
    }

    @Override
    public int errno() {
        return 0; // stat() never fails
    }

    @Override
    public boolean isNull() {
        return false;
    }

    public abstract FileTime creationTime() throws IOException;
    public abstract FileTime lastAccessTime() throws IOException;
    public abstract FileTime lastModifiedTime() throws IOException;

    @Override
    public long lastModified() {
        FileTime mod = null;
        try {
            mod = lastModifiedTime();
        }
        catch (IOException ex) { /* -1 invalid? */ }
        return mod == null ? 0L : mod.toMillis();
    }

    abstract String entryName();

    @Override
    public String toString() {
        return getClass().getName() + '{' + absolutePath() + '}';
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof JarResource) {
            JarResource that = (JarResource) obj;
            return this.absolutePath().equals(that.absolutePath());
        }
        return false;
    }

    @Override
    public int hashCode() {
        return 11 * entryName().hashCode();
    }

}
