package org.jruby.util;

/**
 * This is a shared interface for files loaded as {@link java.io.File} and {@link java.util.zip.ZipEntry}.
 */
public interface FileResource { 
  boolean exists();

  boolean isDirectory();
  boolean isFile();

  long lastModified();
  long length();
}
