/*
 * Copyright (C) 2004 Charles O Nutter
 * Charles O Nutter <headius@headius.com>
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
package org.jruby.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.oro.io.GlobFilenameFilter;
import org.jruby.runtime.SelectorUtils;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class Glob {
    private File pattern;
    private String newPattern;
    private boolean patternEndsWithPathDelimeter = false;
    private boolean patternIsRelative = false;

    /**
     * Constructor for Glob.
     */
    public Glob(String pattern) {
    	// FIXME: don't use user.dir for cwd
    	String cwd = System.getProperty("user.dir");
    	
        // Java File will strip trailing path delimeter.
        // Make a boolean for this special case (how about multiple slashes?)
        if (pattern.endsWith("/")) {
            patternEndsWithPathDelimeter = true;
        }
        
       	this.pattern = new File(pattern); //.getAbsoluteFile();
       	
       	if (!this.pattern.getPath().equals(this.pattern.getAbsolutePath())) {
       		// pattern is relative, but we need to consider cwd; add cwd but remember it's relative so we chop it off later
       		patternIsRelative = true;
       		this.pattern = new File(cwd, pattern);
       	}
       	
        this.newPattern = pattern;
    }
    
    private String[] splitPattern() {
        ArrayList dirs = new ArrayList();
        String path = pattern.getPath();
        StringBuffer sb = new StringBuffer();
        
        for(int i = 0, size = path.length(); i < size; i++) {
            if (path.charAt(i) == '/' || path.charAt(i) == '\\') {
                if (sb.length() > 0) {
                    dirs.add(sb.toString());
                    sb = new StringBuffer();
                }
                // to handle /unix/ and \\windows-server\ files
                if (dirs.size() == 0) {
                    sb.append(path.charAt(i));
                }
            } else {
                sb.append(path.charAt(i));
            }
        }
        if (sb.length() > 0) {
            dirs.add(sb.toString());
        }
        return (String[])dirs.toArray(new String[dirs.size()]);
    }

    /**
     * Get file objects for glob; made private to prevent it being used directly in the future
     * 
     * @return
     */
    private File[] getFiles() {
        String[] dirs = splitPattern();
        File root = new File(dirs[0]);
        int idx = 1;
        if (dirs[0].indexOf('*') > -1 || dirs[0].indexOf('?') > -1) {
            root = new File(".");
            idx = 0;
        }
        for (int size = dirs.length; idx < size; idx++) {
            if (dirs[idx].indexOf('*') == -1 && dirs[idx].indexOf('?') == -1) {
                root = new File(root, dirs[idx]);
            } else {
                break;
            }
        }
        if (idx == dirs.length) {
            return new File[] {root};
        }
        ArrayList matchingFiles = new ArrayList();
        matchingFiles.add(root);
        for (int length = dirs.length; idx < length; idx++) {
            ArrayList currentMatchingFiles = new ArrayList();
            for (int i = 0, size = matchingFiles.size(); i < size; i++) {
                boolean isDirectory = idx + 1 != length;
                String pattern = dirs[idx];
                File parent = (File) matchingFiles.get(i);
                currentMatchingFiles.addAll(getMatchingFiles(parent, pattern, isDirectory));
            }
            matchingFiles = currentMatchingFiles;
        }
        return (File[])matchingFiles.toArray(new File[matchingFiles.size()]);
    }
    
    private static Collection getMatchingFiles(final File parent, final String pattern, final boolean isDirectory) {
    	FileFilter filter = new FileFilter() {
            FilenameFilter filter = new GlobFilenameFilter(pattern);
            /**
             * @see java.io.FileFilter#accept(File)
             */
            public boolean accept(File pathname) {
                return (pathname.isDirectory() || !isDirectory) && SelectorUtils.matchPath(pattern, pathname.getName());
            }
    	};
    	
    	File[] matchArray = parent.listFiles(filter);
        Collection matchingFiles = new ArrayList();
        
    	for (int i = 0; i < matchArray.length; i++) {
    		matchingFiles.add(matchArray[i]);
    		
            if (pattern.equals("**")) {
            	// recurse into dirs
	    		if (matchArray[i].isDirectory()) {
	    			matchingFiles.addAll(getMatchingFiles(matchArray[i], pattern, isDirectory));
	    		}
            }
    	}
        
        return matchingFiles;
    }
    
    public String[] getNames() {
        File[] files = getFiles();
        // FIXME: don't use user.dir for cwd
        String cwd = System.getProperty("user.dir");
        String[] names = new String[files.length];
        for (int i = 0, size = files.length; i < size; i++) {
        	if (patternIsRelative && files[i].getPath().startsWith(cwd)) {
        		// chop off cwd when returning results
        		names[i] = files[i].getPath().substring(cwd.length()) + (patternEndsWithPathDelimeter ? "/" : "");
        	} else {
        		names[i] = files[i].getPath() + (patternEndsWithPathDelimeter ? "/" : "");
        	}
        }
        return names;
    }
}
