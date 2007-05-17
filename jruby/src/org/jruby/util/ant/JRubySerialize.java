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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2005 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.util.ant;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.jruby.main.ASTSerializer;

/**
 * 
 * @author jpetersen
 */
public class JRubySerialize extends Task {
    private File destdir = null;
    private boolean verbose = false;

    private List fileSets = new ArrayList();
    private Mapper mapperElement = null;

    public void execute() throws BuildException {
        Map fileMap = new HashMap();

        FileNameMapper mapper = null;
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        } else {
            mapper = new GlobPatternMapper();
            mapper.setFrom("*.rb");
            mapper.setTo("*.rb.ast.ser");
        }

        SourceFileScanner sfs = new SourceFileScanner(this);

        for (int i = 0, size = fileSets.size(); i < size; i++) {
            FileSet fs = (FileSet) fileSets.get(i);
            DirectoryScanner ds = fs.getDirectoryScanner(getProject());

            File dir = fs.getDir(getProject());
            String[] files = ds.getIncludedFiles();

            files = sfs.restrict(files, dir, destdir, mapper);

            for (int j = 0; j < files.length; j++) {
                File src = new File(dir, files[j]);
                File dest = new File(destdir, mapper.mapFileName(files[j])[0]);
                fileMap.put(src, dest);
            }
        }

        if (fileMap.size() > 0) {
            log(
                "Serializing "
                    + fileMap.size()
                    + " file"
                    + (fileMap.size() == 1 ? "" : "s")
                    + " to "
                    + destdir.getAbsolutePath());

            Iterator iter = fileMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
                try {
                    ((File) entry.getValue()).getParentFile().mkdirs();
                    if (verbose) System.out.println(entry.getKey());
                    ASTSerializer.serialize((File) entry.getKey(), (File) entry.getValue());
                } catch (Exception e) {
                	e.printStackTrace();
                }
            }
        }
    }

    public void setDestdir(File destdir) {
        this.destdir = destdir;
    }

    /**
     * Adds a set of files to copy.
     */
    public void addFileset(FileSet set) {
        fileSets.add(set);
    }
    
    public void setVerbose(boolean verbose) {
    	this.verbose = verbose;
    }

    /**
     * Defines the mapper to map source to destination files.
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper", getLocation());
        }
        mapperElement = new Mapper(getProject());
        return mapperElement;
    }
}
