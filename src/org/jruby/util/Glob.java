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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

/**
 * 
 * @author jpetersen, sma
 * @version $Revision$
 */
public class Glob {
	private final String cwd;
    private final String pattern;
    private final boolean patternEndsWithPathDelimeter;
    private final boolean patternIsRelative;

    /**
     * Constructor for Glob.
     */
    public Glob(String cwd, String pattern) {
    	this.cwd = canonicalize(cwd);
    	
        // Java File will strip trailing path delimeter.
        // Make a boolean for this special case (how about multiple slashes?)
    	this.patternEndsWithPathDelimeter = pattern.endsWith("/") || pattern.endsWith("\\");
        
    	if (new File(pattern).isAbsolute()) {
    		this.patternIsRelative = false;
    		this.pattern = canonicalize(pattern);
       	} else {
       		// pattern is relative, but we need to consider cwd; add cwd but remember it's relative so we chop it off later
       		this.patternIsRelative = true;
       		this.pattern = canonicalize(new File(cwd, pattern).getAbsolutePath());
       	}
    }
    
    private static String canonicalize(String path) {
    	try {
    		return new File(path).getCanonicalPath();
    	} catch (IOException e) {
    		return path;
    	}
    }
    
    /**
     * Splits path into components, leaves out empty components.
     */
    private String[] splitPattern() {
        ArrayList dirs = new ArrayList();
        int i = 0;
        while (true) {
        	int j = pattern.indexOf(File.separatorChar, i);
        	if (j == -1) {
        		if (i < pattern.length()) {
        			dirs.add(pattern.substring(i));
        		}
        		break;
        	}
        	if (i < j) {
        		dirs.add(pattern.substring(i, j));
        		i = j + 1;
        	}
        }
        return (String[]) dirs.toArray(new String[dirs.size()]);
    }

    /**
     * Get file objects for glob; made private to prevent it being used directly in the future
     */
    private File[] getFiles() {
        String[] dirs = splitPattern();
        File root = new File(dirs[0]);
        int idx = 1;
        if (glob2Regexp(dirs[0]) != null) {
            root = new File(".");
            idx = 0;
        }
        for (int size = dirs.length; idx < size; idx++) {
            if (glob2Regexp(dirs[idx]) == null) {
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
            Pattern p = Pattern.compile(glob2Regexp(pattern));

            public boolean accept(File pathname) {
                return (pathname.isDirectory() || !isDirectory) && p.matcher(pathname.getName()).matches();
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
        String[] names = new String[files.length];
        int offset = cwd.endsWith(File.separator) ? 0 : 1;
        for (int i = 0, size = files.length; i < size; i++) {
        	String path = files[i].getPath();
        	if (patternIsRelative && path.startsWith(cwd)) {
        		// chop off cwd when returning results
        		names[i] = path.substring(cwd.length() + offset);
        	} else {
        		names[i] = path;
        	}
        	if (patternEndsWithPathDelimeter) {
        		names[i] += "/";
        	}
        	names[i] = names[i].replace('\\', '/');
        }
        return names;
    }
    
    /**
     * Converts a glob pattern into a normal regexp pattern.
     * <pre>*         =&gt; .*
     * ?         =&gt; .
     * [...]     =&gt; [...]
     * {...,...} =&gt; (...|...) (no subexpression)
     * . + ( )   =&gt; \. \+ \( \)</pre>
     */
    private static String glob2Regexp(String s) {
    	StringBuffer t = new StringBuffer(s.length());
    	boolean pattern = false;
    	int mode = 0;
    	boolean escape = false;
    	for (int i = 0; i < s.length(); i++) {
    		char c = s.charAt(i);
    		if (c == '\\') {
    			escape = true;
    			continue;
    		}
    		if (escape) {
    			t.append(c);
    			escape = false;
    			continue;
    		}
    		switch (mode) {
    		case 0: //normal
	    		switch (c) {
	    		case '*':
	    			pattern = true;
	    			t.append(".*");
	    			break;
	    		case '?':
	    			pattern = true;
	    			t.append('.');
	    			break;
	    		case '[':
	    			pattern = true;
	    			t.append(c);
	    			mode = 1;
	    			break;
	    		case '{':
	    			pattern = true;
	    			t.append('(');
	    			mode = 2;
	    			break;
	    		case '.':
	    		case '(':
	    		case ')':
	    		case '+':
	    		case '|':
	    		case '^':
	    		case '$':
	    			t.append('\\');
	    			// fall through
	    		default:
	    			t.append(c);
	    		}
	    		break;
    		case 1: //inside []
    			t.append(c);
    			if (c == ']') {
    				mode = 0;
    			}
    			break;
    		case 2: //inside {}
    			switch (c) {
    			case ',':
    				t.append('|');
    				break;
    			case '}':
    				t.append(')');
    				mode = 0;
    				break;
    			case '.':
	    		case '(':
	    		case ')':
	    		case '+':
	    		case '|':
	    		case '^':
	    		case '$':
	    			t.append('\\');
	    			// fall through
	    		default:
	    			t.append(c);
    			}
    			break;
    		default:
    			throw new Error();//illegal state
    		}
    	}
    	return pattern ? t.toString() : null;
    }
}
