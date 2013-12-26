package org.jruby.util;

import java.util.Enumeration;
import java.util.HashSet;
import java.util.jar.JarFile;
import java.util.jar.JarEntry;

/**
 * Represents a directory in a jar.
 *
 * <p>Jars do not necessarily have to contain entries for a directory. However, from ruby's point of
 * view, if a jar contains entry /foo/bar, it already contains /foo directory. This resource permits
 * just that.</p>
 */
class JarDirectoryResource extends JarResource {
    public static JarDirectoryResource create(JarFile jar, String path) {
        String dirPath = path.endsWith("/") ? path : path + "/";

        // We always should have something in the jar, so "/" always corresponds to a directory
        if ("/".equals(dirPath)) {
            return new JarDirectoryResource(jar, dirPath);
        }

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            if (entries.nextElement().getName().startsWith(dirPath)) {
                return new JarDirectoryResource(jar, dirPath);
            }
        }
        return null;
    }

    private final String path;

    private JarDirectoryResource(JarFile jar, String path) {
        super(jar);
        this.path = path;
    }

    @Override
    public String entryName() {
        return path;
    }

    @Override
    public boolean isDirectory() {
        return true;
    }

    @Override
    public boolean isFile() {
        return false;
    }

    @Override
    public long lastModified() {
        // Iterating over matching entries is expensive, so let's return that we've never been
        // modified
        return 0L;
    }

    @Override
    public long length() {
        // this pseudo-directory doesn't take up any space
        return 0L;
    }

    @Override
    public String[] list() {
        HashSet<String> dirs = new HashSet<String>();

        Enumeration<JarEntry> entries = jar.entries();
        while (entries.hasMoreElements()) {
            String entryPath = entries.nextElement().getName();

            String subPath;
            if (isRoot()) {
                subPath = entryPath;
            } else if (entryPath.startsWith(path)) {
                subPath = entryPath.substring(path.length());
            } else {
                // entry's path doesn't match the directory
                continue;
            }

            // trim '/' from jar entry directories
            if (subPath.endsWith("/") && subPath.length() > 1) {
                subPath = subPath.substring(0, subPath.length() - 1);
            }

            dirs.add(subPath);
        }

        return dirs.toArray(new String[0]);
    }

    public boolean isRoot() {
        return "/".equals(path);
    }
}
