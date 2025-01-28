/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
/**
 * $Id$
 */
package org.jruby.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.regex.Pattern;

import jnr.posix.JavaSecuredFile;

import org.jruby.Ruby;
import org.jruby.platform.Platform;
import org.jruby.runtime.ThreadContext;

/**
 * <p>
 * This file acts as an alternative to NormalizedFile, due to the problems with
 * current working directory.
 * </p>
 */
public class JRubyFile extends JavaSecuredFile {
    private static final long serialVersionUID = 435364547567567L;

    public static JRubyFile create(String cwd, String pathname) {
        if (pathname == null || pathname.length() == 0 || Ruby.isSecurityRestricted()) {
            return JRubyFile.DUMMY;
        }
        if (pathname.startsWith("file:")) {
            pathname = pathname.substring(5);
        }
        return createNoUnicodeConversion(cwd, pathname, new File(pathname));
    }

    public static FileResource createResource(ThreadContext context, String pathname) {
      return createResource(context.runtime, pathname);
    }

    public static FileResource createResourceAsFile(Ruby runtime, String pathname) {
        return createResource(runtime, runtime.getCurrentDirectory(), pathname, true);
    }

    public static FileResource createRestrictedResource(String cwd, String pathname) {
      return createResource(null, cwd, pathname);
    }

    public static FileResource createResource(Ruby runtime, String pathname) {
        return createResource(runtime, runtime.getCurrentDirectory(), pathname, false);
    }

    public static FileResource createResource(Ruby runtime, String cwd, String pathname) {
        return createResource(runtime, cwd, pathname, false);
    }

    private static FileResource createResource(Ruby runtime, String cwd, String pathname, boolean isFile) {
        FileResource resource = EmptyFileResource.create(pathname);
        if (resource != null) return resource;

        // This will work against anything potentially containing a '!' in it and does not require a scheme.
        // (see test/test_java_on_load_path.rb: $LOAD_PATH << "test/test_jruby_1332.jar!"; require 'test_jruby_1332.rb'
        resource = JarResource.create(pathname);
        if (resource != null) return resource;

        if (Platform.IS_WINDOWS &&
                (pathname.equalsIgnoreCase("nul") || pathname.equalsIgnoreCase("nul:"))) {
            return new NullDeviceResource();
        }

        if (pathname.indexOf(':') > 0) { // scheme-oriented resources
            if (pathname.startsWith("classpath:")) {
                String subpath = pathname.substring(10);
                String slash = subpath.charAt(0) == '/' ? "" : "/";
                pathname = "uri:classloader:" + slash + subpath;
            }

            if (pathname.startsWith("uri:file:")) {
                // treat uri:file: as file:
                pathname = pathname.substring(4);
            }

            // replace is needed for maven/jruby-complete/src/it/app_using_classpath_uri to work
            if (pathname.startsWith("uri:")) return URLResource.create(runtime, pathname, isFile);

            if (pathname.startsWith("file:")) {
                pathname = pathname.substring(5);

                // Mostly for Windows to work as "//" is not a valid existing dir but this is more that the dir name should be.
                if (pathname.equals("//")) pathname = "/";
                if (pathname.length() == 0) return EmptyFileResource.INSTANCE;
            }
        }

        if (cwd != null && (cwd.startsWith("uri:") || cwd.startsWith("file:")) && !new File(pathname).isAbsolute()) {
            return createResource(runtime, null, cwd + '/' + pathname);
        }

        // If any other special resource types fail, count it as a filesystem backed resource.
        return new RegularFileResource(runtime != null ? runtime.getPosix() : null, create(cwd, pathname), pathname);
    }
    public static boolean isResourceRegularFile(FileResource res) {
        return (res instanceof RegularFileResource);
    }

    public static String normalizeSeps(String path) {
        return Platform.IS_WINDOWS ? path.replace(File.separatorChar, '/') : path;
    }

    private static JRubyFile createNoUnicodeConversion(String cwd, String pathname, File path) {
        if (path.isAbsolute()) {
            return new JRubyFile(path);
        } else if (Platform.IS_WINDOWS) {
            // File and company do not seem to recognize COM ports on Windows as absolute.  Cheat!
            if (JRubyFile.isComPort(pathname)) {
                return new JRubyFile(pathname); // use raw path, not absolute path
            // Nor do they seem to recognize bare \ and / on Windows as absolute.  Cheat!
            } else if (pathname.startsWith("/") || pathname.startsWith("\\")) {
                return new JRubyFile(path);
            }
        }
        if (cwd != null && cwd.startsWith("uri:") && !pathname.startsWith("uri:") && !pathname.contains("!/")) {
            return new JRubyFile(cwd + '/' + pathname);
        }
        path = new File(cwd, pathname);
        if (!path.isAbsolute()) {
            throw new IllegalArgumentException("Neither current working directory ("+cwd+") nor pathname ("+pathname+") led to an absolute path");
        }
        return new JRubyFile(path);
    }

    public static String getFileProperty(String property) {
        return normalizeSeps(SafePropertyAccessor.getProperty(property, "/"));
    }

    private static final Pattern windowsComPattern = Pattern.compile("(?:\\/\\/\\.\\/|\\\\\\\\\\.\\\\)?COM\\d\\d?", Pattern.CASE_INSENSITIVE);

    // Checks to see if it's a windows com port path
    static boolean isComPort(String path) {
        if (!Platform.IS_WINDOWS) return false;
        int len = path.length();

        // Look for both \\.\ and //./, but avoid COMxx (len != 5) as that isn't valid
        return len < 10 && len > 3 && len != 5 && windowsComPattern.matcher(path).matches();
    }

    private JRubyFile(File file) {
        this(file.getAbsolutePath());
    }

    public JRubyFile(String filename) {
        super(filename);
    }

    public JRubyFile(String parent, String child) {
        super(parent, child);
    }

    @Override
    public String getAbsolutePath() {
        final String path = super.getPath();
        if (path.startsWith("uri:")) {
            // TODO better do not collapse // to / for uri: files
            return normalizeSeps(path.replaceFirst(":/([^/])", "://$1" ));
        }
        return normalizeSeps(new File(path).getAbsolutePath());
    }

    @Override
    public String getCanonicalPath() throws IOException {
        try {
            return normalizeSeps(super.getCanonicalPath());
        }
        catch (IOException e) {
            // usually IOExceptions don't tell us anything about the path,
            // so add an extra wrapper to give more debugging help.
            throw new IOException("Unable to canonicalize path: " + getAbsolutePath(), e);
        }
    }

    @Override
    public String getPath() {
        return normalizeSeps(super.getPath());
    }

    final String getPathDefault() {
        return super.getPath();
    }

    @Override
    public String toString() {
        return normalizeSeps(super.toString());
    }

    @Override
    public File getAbsoluteFile() {
        return new JRubyFile(getAbsolutePath());
    }

    @Override
    public File getCanonicalFile() throws IOException {
        return new JRubyFile(getCanonicalPath());
    }

    @Override
    public String getParent() {
        String parent = super.getParent();
        if (parent != null) {
            parent = normalizeSeps(parent);
        }
        return parent;
    }

    @Override
    public File getParentFile() {
        String parent = getParent();
        if (parent == null) return this;
        return new JRubyFile(parent);
    }

    public static File[] listRoots() {
        File[] roots = File.listRoots();
        JRubyFile[] smartRoots = new JRubyFile[roots.length];
        for(int i = 0, j = roots.length; i < j; i++) {
            smartRoots[i] = new JRubyFile(roots[i].getPath());
        }
        return smartRoots;
    }

    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        return new JRubyFile(File.createTempFile(prefix, suffix,directory));
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        return new JRubyFile(File.createTempFile(prefix, suffix));
    }

    @Override
    public String[] list(FilenameFilter filter) {
        String[] files = super.list(filter);
        if (files == null) {
            return null;
        }

        String[] smartFiles = new String[files.length];
        for (int i = 0; i < files.length; i++) {
            smartFiles[i] = normalizeSeps(files[i]);
        }
        return smartFiles;
    }

    @Override
    public File[] listFiles() {
        return convertFiles(super.listFiles());
    }

    @Override
    public File[] listFiles(final FileFilter filter) {
        return convertFiles(super.listFiles(filter));
    }

    @Override
    public File[] listFiles(final FilenameFilter filter) {
        return convertFiles(super.listFiles(filter));
    }

    private JRubyFile[] convertFiles(final File[] files) {
        if (files == null) return null; // non-existent directory

        final String absolutePath = super.getAbsolutePath();
        JRubyFile[] smartFiles = new JRubyFile[files.length];
        for (int i = 0; i < files.length; i++) {
            final File file = files[i];
            smartFiles[i] = createNoUnicodeConversion(absolutePath, file.getPath(), file);
        }
        return smartFiles;
    }

    public static final JRubyFile DUMMY = new JRubyFile("") {
        @Override
        public String getAbsolutePath() {
            return "";
        }

        @Override
        public boolean isDirectory() {
            return false;
        }

        @Override
        public boolean exists() {
            return false;
        }

        @Override
        public String getCanonicalPath() throws IOException {
            throw new FileNotFoundException("File does not exist");
        }

        @Override
        public String getPath() {
            return "";
        }

        @Override
        public String toString() {
            return "";
        }

        @Override
        public File getAbsoluteFile() {
            return this;
        }

        @Override
        public File getCanonicalFile() throws IOException {
            throw new FileNotFoundException("File does not exist");
        }

        @Override
        public String getParent() {
            return "";
        }

        @Override
        public File getParentFile() {
            return this;
        }

        @Override
        public String[] list(FilenameFilter filter) {
            return new String[0];
        }

        @Override
        public File[] listFiles() {
            return new File[0];
        }

        @Override
        public File[] listFiles(final FileFilter filter) {
            return new File[0];
        }

        @Override
        public File[] listFiles(final FilenameFilter filter) {
            return new File[0];
        }
    };
}
