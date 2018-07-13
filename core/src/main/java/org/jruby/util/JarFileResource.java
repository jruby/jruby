package org.jruby.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
import java.nio.file.attribute.FileTime;
import java.util.jar.JarEntry;

/**
 * Represents a file in a jar.
 *
 * <p>
 * Note: while directories can be contained within a jar, they're still represented by
 * JarDirectoryResource, since Ruby expects a directory to exist as long as any files in that
 * directory do, or Dir.glob would break.
 * </p>
 */
class JarFileResource extends JarResource {

    private final JarCache.JarIndex index;
    private final JarEntry entry;

    JarFileResource(String jarPath, boolean rootSlashPrefix, JarCache.JarIndex index, JarEntry entry) {
        super(jarPath, rootSlashPrefix);
        this.index = index;
        this.entry = entry;
    }

    @Override
    public String entryName() {
        return entry.getName();
    }

    @Override
    public boolean isDirectory() {
        return false;
    }

    @Override
    public boolean isFile() {
        return true;
    }

    @Override
    public long length() {
        return entry.getSize();
    }

    public FileTime creationTime() {
        return entry.getCreationTime();
    }

    public FileTime lastAccessTime() {
        return entry.getLastAccessTime();
    }

    public FileTime lastModifiedTime() {
        return entry.getLastModifiedTime();
    }

    @Override
    public String[] list() {
        return null; // Files cannot be listed
    }

    @Override
    public InputStream openInputStream() throws IOException {
        return index.getInputStream(entry);
    }

    @Override
    public Channel openChannel(int flags, int perm) throws IOException {
        return Channels.newChannel(openInputStream());
    }

    @Override
    public <T> T unwrap(Class<T> type) {
        if (type == JarEntry.class) return (T) entry;
        throw new UnsupportedOperationException("unwrap: " + type.getName());
    }

}
