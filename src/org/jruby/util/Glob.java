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
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
        	} else if (i == j) {
                if (i == 0) {
                    dirs.add("");
                }
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
