/***** BEGIN LICENSE BLOCK *****
 * Version: CPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Common Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/cpl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2006 Charles O Nutter <headius@headius.com>
 * 
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the CPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the CPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/
package org.jruby.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.URI;

/**
 * This class provides a File implementation that normalizes all path separators to forward slashes.
 * This mimics the behavior of C Ruby, where all paths are internally made UNIX-style, even on Windows.
 */
public class NormalizedFile extends File {
    private static final long serialVersionUID = 7630618150344842227L;

    public NormalizedFile(String pathname) {
        super(pathname);
    }

    public NormalizedFile(URI uri) {
        super(uri);
    }

    public NormalizedFile(File parent, String child) {
        super(parent, child);
    }

    public NormalizedFile(String parent, String child) {
        super(parent, child);
    }

    public String getAbsolutePath() {
        return super.getAbsolutePath().replace(File.separatorChar, '/');
    }

    public String getCanonicalPath() throws IOException {
        return super.getCanonicalPath().replace(File.separatorChar, '/');
    }

    public String getPath() {
        return super.getPath().replace(File.separatorChar, '/');
    }

    public String toString() {
        return super.toString().replace(File.separatorChar, '/');
    }

    public File getAbsoluteFile() {
        return new NormalizedFile(getAbsolutePath());
    }

    public File getCanonicalFile() throws IOException {
        return new NormalizedFile(getCanonicalPath());
    }

    public String getParent() {
        return super.getParent().replace(File.separatorChar, '/');
    }

    public File getParentFile() {
        return new NormalizedFile(getParent());
    }
    
    public static File[] listRoots() {
        File[] roots = File.listRoots();
        NormalizedFile[] smartRoots = new NormalizedFile[roots.length];
        for (int i = 0; i < roots.length; i++) {
            smartRoots[i] = new NormalizedFile(roots[i].getPath());
        }
        
        return smartRoots;
    }
    
    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        File file = File.createTempFile(prefix, suffix, directory);
        return new NormalizedFile(file.getPath());
    }
    
    public static File createTempFile(String prefix, String suffix) throws IOException {
        File file = File.createTempFile(prefix, suffix);
        return new NormalizedFile(file.getPath());
    }

    public String[] list() {
        return super.list();
    }

    public String[] list(FilenameFilter filter) {
        String[] files = super.list(filter);
        
        if (files == null) {
            return null;
        } else {
            String[] smartFiles = new String[files.length];
            for (int i = 0; i < files.length; i++) {
                smartFiles[i] = files[i].replace(File.separatorChar, '/');
            }
            return smartFiles;
        }
    }

    public File[] listFiles() {
        File[] files = super.listFiles();
        
        if (files == null) {
            return null;
        } else {
            NormalizedFile[] smartFiles = new NormalizedFile[files.length];
            for (int i = 0; i < files.length; i++) {
                smartFiles[i] = new NormalizedFile(files[i].getPath());
            }
            return smartFiles;
        }
    }

    public File[] listFiles(FileFilter filter) {
        File[] files = super.listFiles(filter);
        
        if (files == null) {
            return null;
        } else {
            NormalizedFile[] smartFiles = new NormalizedFile[files.length];
            for (int i = 0; i < files.length; i++) {
                smartFiles[i] = new NormalizedFile(files[i].getPath());
            }
            return smartFiles;
        }
    }

    public File[] listFiles(FilenameFilter filter) {
        File[] files = super.listFiles(filter);
        
        if (files == null) {
            return null;
        } else {
            NormalizedFile[] smartFiles = new NormalizedFile[files.length];
            for (int i = 0; i < files.length; i++) {
                smartFiles[i] = new NormalizedFile(files[i].getPath());
            }
            return smartFiles;
        }
    }
    
    public static String getFileProperty(String property) {
        String value = System.getProperty(property);
        
        return value.replace(File.separatorChar, '/');
    }
}
