/*
 * Copyright (C) 2002 Thomas E. Enebo <enebo@acm.org>
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
package org.jruby.util.ant.methodgenerator;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.stream.StreamSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerConfigurationException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.DirectoryScanner;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.types.FileSet;
import org.apache.tools.ant.types.Mapper;
import org.apache.tools.ant.util.FileNameMapper;
import org.apache.tools.ant.util.RegexpPatternMapper;
import org.apache.tools.ant.util.SourceFileScanner;

/**
 *
 * @author enebo
 * @version $Revision$
 */
public class JRubyMethodGenerator extends Task {
    // The XSLT stylesheet which actually knows how to make XML into java
    private static final String GENERATOR_STYLESHEET = 
	"tools/methodGenerator.xsl";

    private File destdir = null;

    private List fileSets = new ArrayList();
    private Mapper mapperElement = null;
    private Transformer transformer = null;

    // This should rebuild all definitions if GENERATOR_STYLESHEET is
    // out of date.
    public void execute() throws BuildException {
        Map fileMap = new HashMap();

        FileNameMapper mapper = null;
        if (mapperElement != null) {
            mapper = mapperElement.getImplementation();
        } else {
            mapper = new RegexpPatternMapper();
            mapper.setFrom("(.*[\\\\/])([^\\\\/]+).xml");
            mapper.setTo("\\1/internal/runtime/builtin/definitions/\\2Definition.java");
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
	    // No bother loading transformer unless we actually find
	    // some definitions out of date.
	    if (transformer == null) {
		try {
		    TransformerFactory factory = 
			TransformerFactory.newInstance();
		    StreamSource sheet= new StreamSource(GENERATOR_STYLESHEET);

		    transformer = factory.newTransformer(sheet);
		} catch (TransformerConfigurationException e) {
		    throw new BuildException(e);
		}
	    }

            log(
                "Generating "
                    + fileMap.size()
                    + " definition file"
                    + (fileMap.size() == 1 ? "" : "s")
                    + " to "
                    + destdir.getAbsolutePath());

            Iterator iter = fileMap.entrySet().iterator();
            while (iter.hasNext()) {
                Map.Entry entry = (Map.Entry) iter.next();
		((File) entry.getValue()).getParentFile().mkdirs();
		
		try {
		    generateMethods((File) entry.getKey(), 
				    (File) entry.getValue());
		} catch (Exception e) {
		    throw new BuildException(e);
		}
            }
        }
    }

    // Transforms
    private void generateMethods(File source, File destination) 
	throws TransformerException, FileNotFoundException, IOException {
	StreamSource in = new StreamSource(new FileInputStream(source));
	StreamResult out = new StreamResult(new FileOutputStream(destination));

	transformer.transform(in, out);
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
