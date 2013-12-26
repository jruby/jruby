package org.jruby.util;

import org.jruby.RubyFile;
import org.jruby.Ruby;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.*;
import java.util.zip.ZipEntry;
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
  public static JarFileResource create(JarFile jar, String path) {
    JarEntry entry = jar.getJarEntry(path);
    return ((entry != null) && !entry.isDirectory()) ? new JarFileResource(jar, entry) : null;
  }

  private final ZipEntry entry;

  private JarFileResource(JarFile jar, ZipEntry entry) {
    super(jar);
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
