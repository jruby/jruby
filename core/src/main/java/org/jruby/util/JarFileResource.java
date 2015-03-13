package org.jruby.util;

import jnr.posix.POSIX;
import org.jruby.util.io.ModeFlags;

import java.io.IOException;
import java.io.InputStream;
import java.nio.channels.Channel;
import java.nio.channels.Channels;
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

  @Override
  public long lastModified() {
    return entry.getTime();
  }

  @Override
  public String[] list() {
    // Files cannot be listed
    return null;
  }

  @Override
  InputStream openInputStream() {
    return index.getInputStream(entry);
  }

  @Override
  public Channel openChannel(ModeFlags flags, int perm) throws ResourceException {
    return Channels.newChannel(inputStream());
  }
}
