/*
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with JRuby; if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 */
package org.jruby.util.ant;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.FilterChain;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.GlobPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;
import org.jruby.main.ASTSerializer;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class JRubySerialize extends Task {
    private File destdir = null;

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
            DirectoryScanner ds = fs.getDirectoryScanner(project);

            File dir = fs.getDir(project);
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
                    ((File) entry.getValue()).mkdirs();
                    ASTSerializer.serialize((File) entry.getKey(), (File) entry.getValue());
                } catch (IOException ioExcpn) {
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

    /**
     * Defines the mapper to map source to destination files.
     */
    public Mapper createMapper() throws BuildException {
        if (mapperElement != null) {
            throw new BuildException("Cannot define more than one mapper", location);
        }
        mapperElement = new Mapper(project);
        return mapperElement;
    }
}