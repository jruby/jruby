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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
 * Copyright (C) 2006 Miguel Covarrubias <mlcovarrubias@gmail.com>
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
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

// TODO: Literal escaping does not work because on windows (See bug #1280905)
public class Glob {
    private final List patterns;
	// TODO: If '{' or '}' is just a literal this is broken.
	private static final Pattern BRACE_PATTERN = Pattern.compile("(.*)\\{([^\\{\\}]*)\\}(.*)");

    public Glob(String cwd, String pattern) {
		List expansion = new ArrayList();
		expansion.add(pattern);

		// We pre-expand the pattern list into multiple patterns (see GlobPattern for more info).
        expansion = splitPatternBraces(expansion);
		
		cwd = canonicalize(cwd);
        
		int size = expansion.size();
		patterns = new ArrayList(size); 
		for (int i = 0; i < size; i++) {
			String newPattern = (String) expansion.get(i);
			
			patterns.add(i, new GlobPattern(cwd, newPattern));
		}
    }
    
    private static String canonicalize(String path) {
    	try {
    		return new NormalizedFile(path).getCanonicalPath();
    	} catch (IOException e) {
    		return path;
    	}
    }
	
	private static List splitPatternBraces(List dirs) {
		// Remove current dir and add all expanded to bottom of list.
		// Our test condition is dynamic (dirs.size() each iteration), so this is ok. 
        for (int i = 0; i < dirs.size(); i++) {
            String fragment = (String) dirs.get(i);
	        Matcher matcher = BRACE_PATTERN.matcher(fragment);

			// Found a set of braces
			if (matcher.find()) {
				dirs.remove(i);
				String beforeBrace = matcher.group(1);
				String[] subElementList = matcher.group(2).split(",");
				String afterBrace = matcher.group(3);

				for (int j = 0; j < subElementList.length; j++) {
					dirs.add(beforeBrace + subElementList[j] + afterBrace);
				}
			}
        }
		
		return dirs;
	}
	
    /**
     * Get file objects for glob; made private to prevent it being used directly in the future
     */
    private void getFiles() {
        // we always use / to normalize all file paths internally
    	String pathSplitter = "/";
    	
    	for (Iterator iter = patterns.iterator(); iter.hasNext();) {
			GlobPattern globPattern = (GlobPattern) iter.next();
	        String[] dirs = globPattern.getPattern().split(pathSplitter);
        	NormalizedFile root = new NormalizedFile(dirs[0]);
        	int idx = 1;
        	if (glob2Regexp(dirs[0]) != null) {
            	root = new NormalizedFile(".");
            	idx = 0;
        	}
        	for (int size = dirs.length; idx < size; idx++) {
            	if (glob2Regexp(dirs[idx]) == null) {
                	root = new NormalizedFile(root, dirs[idx]);
            	} else {
	                break;
    	        }
	        }
	    	ArrayList matchingFiles = new ArrayList();

			if (idx == dirs.length) {
				if (root.exists()) {
	        		matchingFiles.add(root);
				}

				globPattern.setMatchedFiles(matchingFiles);
				continue;
        	}
			
        	matchingFiles.add(root);
        	for (int length = dirs.length; idx < length; idx++) {
            	ArrayList currentMatchingFiles = new ArrayList();
            	for (int i = 0, size = matchingFiles.size(); i < size; i++) {
                	boolean isDirectory = idx + 1 != length;
                	String pattern = dirs[idx];
                	NormalizedFile parent = (NormalizedFile) matchingFiles.get(i);
                	currentMatchingFiles.addAll(getMatchingFiles(parent, pattern, isDirectory));
            	}
            	matchingFiles = currentMatchingFiles;
        	}
			globPattern.setMatchedFiles(matchingFiles);
		}
    }
    
    private static Collection getMatchingFiles(final NormalizedFile parent, final String pattern, final boolean isDirectory) {
        String expandedPattern = glob2Regexp(pattern);
        if (expandedPattern == null) expandedPattern = pattern;
        
        final Pattern p = Pattern.compile(expandedPattern);
        
    	FileFilter filter = new FileFilter() {
            public boolean accept(File pathname) {
                return (pathname.isDirectory() || !isDirectory) && p.matcher(pathname.getName()).matches();
            }
    	};
    	
    	NormalizedFile[] matchArray = (NormalizedFile[])parent.listFiles(filter);
        Collection matchingFiles = new ArrayList();

        if (matchArray != null) {
            for (int i = 0; i < matchArray.length; i++) {
    	        matchingFiles.add(matchArray[i]);
    		
                if (pattern.equals("**")) {
            	    // recurse into dirs
	    	    if (matchArray[i].isDirectory()) {
                        matchingFiles.addAll(getMatchingFiles(matchArray[i], pattern, isDirectory));
                    }
                }
            }
        }
        
        if("**".equals(pattern)) {
            matchingFiles.add(parent);
        }
        
        return matchingFiles;
    }
    
    public String[] getNames() {
        try {
            getFiles();
        } catch (PatternSyntaxException e) {
        	// This can happen if someone does Dir.glob("{") or similiar.
            return new String[] {};
        }
		
		Collection allMatchedNames = new TreeSet();
		for (Iterator iter = patterns.iterator(); iter.hasNext();) {
			GlobPattern pattern = (GlobPattern) iter.next();
			
			allMatchedNames.addAll(pattern.getMatchedFiles());
		}
		
        return (String[]) allMatchedNames.toArray(new String[allMatchedNames.size()]);
    }
    
    /**
     * Converts a glob pattern into a normal regexp pattern.
     * <pre>*         =&gt; .*
     * ?         =&gt; .
     * [...]     =&gt; [...]
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
	    		case '.': case '(': case ')': case '+': case '|': case '^': case '$':
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
    		default:
    			throw new Error();//illegal state
    		}
    	}
    	return pattern ? t.toString() : null;
    }
	
	/*
	 * Glob breaks up a glob expression into multiple glob patterns.  This is needed when dealing
	 * with glob patterns like '{/home,foo}/enebo/*.rb' or '/home/enebo/{foo,bar/}'  So for every
	 * embedded '{}' pair we create a new glob pattern and then determine whether that pattern
	 * ends with a delimeter or is an absolute pattern.  Pathological globs could lead to many
	 * many patterns (e.g. '{a{b,{d,e},{g,h}}}') where each nested set of curlies will double the
	 * amount of patterns.
	 * 
	 * Glob and GlobPattern do a lot of list copying and transforming.  This could be done better.
	 */
	private class GlobPattern {
		private String pattern;
		private String cwd;
		private boolean endsWithDelimeter;
		private boolean patternIsRelative;
		private ArrayList files = null;
		
		public GlobPattern(String cwd, String pattern) {
	    	this.cwd = cwd;
			
	        // Java File will strip trailing path delimeter.
	        // Make a boolean for this special case (how about multiple slashes?)
	    	this.endsWithDelimeter = pattern.endsWith("/") || pattern.endsWith("\\");
			
	    	if (new NormalizedFile(pattern).isAbsolute()) {
    			this.patternIsRelative = false;
    			this.pattern = canonicalize(pattern);
	       	} else {
    	   		// pattern is relative, but we need to consider cwd; add cwd but remember it's relative so we chop it off later
       			this.patternIsRelative = true;
       			this.pattern = canonicalize(new NormalizedFile(cwd, pattern).getAbsolutePath());
	       	}
		}

		public ArrayList getMatchedFiles() {
			ArrayList fileNames = new ArrayList();
			int size = files.size();
			int offset = cwd.endsWith("/") ? 0 : 1;
			
	        for (int i = 0; i < size; i++) {
				String path = ((NormalizedFile) files.get(i)).getPath();
				String name;

	        	if (patternIsRelative && !path.equals(cwd) && path.startsWith(cwd)) {
	        		// chop off cwd when returning results
	        		name = path.substring(cwd.length() + offset);
	        	} else {
	        		name = path;
	        	}
	        	if (endsWithDelimeter) {
	        		name += "/";
	        	}

				
				fileNames.add(name.replace('\\', '/'));
	        }
			
	        return fileNames;
		}

		public void setMatchedFiles(ArrayList files) {
            this.files = files;			
		}

		public String getPattern() {
			return pattern;
		}
	}
}
