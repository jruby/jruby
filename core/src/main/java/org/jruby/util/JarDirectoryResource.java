package org.jruby.util;

import jnr.posix.POSIX;
import org.jruby.Ruby;
import org.jruby.util.io.ChannelDescriptor;
import org.jruby.util.io.ModeFlags;
import java.io.IOException;
import java.io.InputStream;

/**
 * Represents a directory in a jar.
 *
 * <p>Jars do not necessarily have to contain entries for a directory. However, from ruby's point of
 * view, if a jar contains entry /foo/bar, it already contains /foo directory. This resource permits
 * just that.</p>
 */
class JarDirectoryResource extends JarResource {
    private final String path;
    private final String[] contents;

    JarDirectoryResource(String jarPath, boolean rootSlashPrefix, String path, String[] contents) {
        super(jarPath, rootSlashPrefix);
        this.path = path;
        this.contents = contents;
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
      return contents;
    }

    public boolean isRoot() {
        return "/".equals(path);
    }

    @Override
    InputStream openInputStream() throws IOException {
        throw new ResourceException.FileIsDirectory(path);
    }

    @Override
    public ChannelDescriptor openDescriptor(ModeFlags flags, int perm) throws ResourceException {
        // opening a directory seems to blow up with EACCESS in jruby (although MRI allows instantiation but blows up on read).
        // So mimicking that for now.
        throw new ResourceException.PermissionDenied(absolutePath());
    }
}
