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
 * Copyright (C) 2007 Nick Sieger <nicksieger@gmail.com>
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

package org.jruby.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FilenameFilter;
import java.io.IOException;

@Deprecated // Replaced now with EmptyFileResource
public class JRubyNonExistentFile extends JRubyFile {
    static final JRubyNonExistentFile NOT_EXIST = new JRubyNonExistentFile();
    private JRubyNonExistentFile() {
        super("");
    }

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

    public static File[] listRoots() {
        return new File[0];
    }

    public static File createTempFile(String prefix, String suffix, File directory) throws IOException {
        return createTempFile(prefix, suffix);
    }

    public static File createTempFile(String prefix, String suffix) throws IOException {
        throw new FileNotFoundException("File does not exist");
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
}
