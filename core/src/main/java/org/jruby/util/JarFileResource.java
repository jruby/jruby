package org.jruby.util;

import org.jruby.RubyFile;

import java.io.IOException;
import java.util.zip.ZipEntry;

/**
 * A file resource that is contained within a jar.
 */
public class JarFileResource extends ZipEntry implements FileResource {
  public static JarFileResource load(String path) {
    String sanitized = path.startsWith("file:") ? path.substring(5) : path;

    int bang = sanitized.indexOf('!');

    if (bang == -1) {
      throw new IllegalArgumentException("Expecting a jar containing path, but got: " + sanitized);
    }

    String jar = sanitized.substring(0, bang);
    String after = sanitized.substring(bang + 2);

    try {
      return new JarFileResource(RubyFile.getDirOrFileEntry(jar, after));
    } catch (IOException ioError) {
      // Failed to get the file, so returning null, similar to like ZipFile#getEntry does
      return null;
    }
  }

  private JarFileResource(ZipEntry entry) {
    super(entry);
  }

  @Override
  public boolean exists() {
    // ZipEntry always exists
    return true;
  }

  @Override
  public boolean isFile() {
    return !isDirectory();
  }

  @Override
  public long length() {
    return getSize();
  }

  @Override
  public long lastModified() {
    return getTime();
  }
}
