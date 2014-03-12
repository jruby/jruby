package org.jruby.util;

import jnr.posix.FileStat;
import jnr.posix.POSIX;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;

public abstract class JarResource implements FileResource {
    private static Pattern PREFIX_MATCH = Pattern.compile("^(?:jar:)?(?:file:)?(.*)$");

    private static final JarCache jarCache = new JarCache();

    public static JarResource create(String pathname) {
        Matcher matcher = PREFIX_MATCH.matcher(pathname);
        String sanitized = matcher.matches() ? matcher.group(1) : pathname;

        int bang = sanitized.indexOf('!');
        if (bang < 0) {
            return null;
        }

        String jarPath = sanitized.substring(0, bang);
        String slashPath = sanitized.substring(bang + 1);
        if (!slashPath.startsWith("/")) {
            slashPath = "/" + slashPath;
        }

        // TODO: Do we really need to support both test.jar!foo/bar.rb and test.jar!/foo/bar.rb cases?
        JarResource resource = createJarResource(jarPath, slashPath);

        if (resource == null) {
            resource = createJarResource(jarPath, slashPath.substring(1));
        }
        
        return resource;
    }

    private static JarResource createJarResource(String jarPath, String path) {
        JarCache.JarIndex index = jarCache.getIndex(jarPath);

        if (index == null) {
            // Jar doesn't exist
            return null;
        }

        // Try it as directory first, because jars tend to have foo/ entries
        // and it's not really possible disambiguate between files and directories.
        String[] entries = index.cachedDirEntries.get(path);
        if (entries != null) {
            return new JarDirectoryResource(jarPath, path, entries);
        }

        JarEntry jarEntry = index.getJarEntry(path);
        if (jarEntry != null) {
            return new JarFileResource(jarPath, jarEntry);
        }

        return null;
    }

    private final String jarPath;
    private final JarFileStat fileStat;

    protected JarResource(String jarPath) {
        this.jarPath = jarPath;
        this.fileStat = new JarFileStat(this);
    }

    @Override
    public String absolutePath() {
        return jarPath + "!" + entryName();
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
    public boolean isSymLink() {
        // Jar archives shouldn't contain symbolic links, or it would break portability. `jar`
        // command behavior seems to comform to that (it unwraps syumbolic links when creating a jar
        // and replaces symbolic links with regular file when extracting from a zip that contains
        // symbolic links). Also see:
        // http://www.linuxquestions.org/questions/linux-general-1/how-to-create-jar-files-with-symbolic-links-639381/
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
    public JRubyFile hackyGetJRubyFile() {
      return JRubyNonExistentFile.NOT_EXIST;
    }

    abstract protected String entryName();
}
