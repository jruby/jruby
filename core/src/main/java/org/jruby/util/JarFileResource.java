package org.jruby.util;

import java.util.jar.JarFile;
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
  private final JarEntry entry;

  JarFileResource(String jarPath, JarEntry entry) {
    super(jarPath);
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
}
