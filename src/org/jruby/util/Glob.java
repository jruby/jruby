package org.jruby.util;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import org.apache.oro.io.GlobFilenameFilter;

/**
 * 
 * @author jpetersen
 * @version $Revision$
 */
public class Glob {
    private File pattern;
    private boolean patternEndsWithPathDelimeter = false;

    /**
     * Constructor for Glob.
     */
    public Glob(String pattern) {
        // Java File will strip trailing path delimeter.
        // Make a boolean for this special case (how about multiple slashes?)
        if (pattern.endsWith("/")) {
            patternEndsWithPathDelimeter = true;
        }
        
        this.pattern = new File(pattern); //.getAbsoluteFile();
    }
    
    private String[] splitPattern() {
        ArrayList dirs = new ArrayList();
        String path = pattern.getPath();
        StringBuffer sb = new StringBuffer();
        for(int i = 0, size = path.length(); i < size; i++) {
            if (path.charAt(i) == File.separatorChar) {
                if (sb.length() > 0) {
                    dirs.add(sb.toString());
                    sb = new StringBuffer();
                }
                // to handle /unix/ and \\windows-server\ files
                if (dirs.size() == 0) {
                    sb.append(File.separator);
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

    public File[] getFiles() {
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
                boolean isDirectory = (idx + 1 != length);
                String pattern = dirs[idx];
                File parent = (File) matchingFiles.get(i);
                currentMatchingFiles.addAll(getMatchingFiles(parent, pattern, isDirectory));
            }
            matchingFiles = currentMatchingFiles;
        }
        return (File[])matchingFiles.toArray(new File[matchingFiles.size()]);
    }
    
    private Collection getMatchingFiles(final File parent, final String pattern, final boolean isDirectory) {
        if (pattern.equals("**")) {
            // TODO: This kind of recursive globbing pattern needs to be implemented!
            return Collections.EMPTY_LIST;
        }
        return Arrays.asList(parent.listFiles(new FileFilter() {
            FilenameFilter filter = new GlobFilenameFilter(pattern);
            /**
             * @see java.io.FileFilter#accept(File)
             */
            public boolean accept(File pathname) {
                return (pathname.isDirectory() || !isDirectory) && filter.accept(parent, pathname.getName());
            }
        }));
    }
    
    public String[] getNames() {
        File[] files = getFiles();
        String[] names = new String[files.length];
        for (int i = 0, size = files.length; i < size; i++) {
            names[i] = files[i].getPath() + (patternEndsWithPathDelimeter ? "/" : "");
        }
        return names;
    }
}
