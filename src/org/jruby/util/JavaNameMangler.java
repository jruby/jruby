/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 * @author headius
 */
public class JavaNameMangler {
    public static final Pattern PATH_SPLIT = Pattern.compile("[/\\\\]");
    
    public static String mangledFilenameForStartupClasspath(String filename) {
        if (filename.equals("-e")) {
            return "ruby/__dash_e__";
        }
        
        return mangleFilenameForClasspath(filename, null, "", false);
    }
    
    public static String mangleFilenameForClasspath(String filename) {
        return mangleFilenameForClasspath(filename, null, "ruby");
    }
    
    public static String mangleFilenameForClasspath(String filename, String parent, String prefix) {
        return mangleFilenameForClasspath(filename, parent, prefix, true);
    }
    
    public static String mangleFilenameForClasspath(String filename, String parent, String prefix, boolean canonicalize) {
        try {
            String classPath = "";
            if(filename.indexOf("!") != -1) {
                String before = filename.substring(6, filename.indexOf("!"));
                if (canonicalize) {
                    classPath = new JRubyFile(before + filename.substring(filename.indexOf("!")+1)).getCanonicalPath().toString();
                } else {
                    classPath = new JRubyFile(before + filename.substring(filename.indexOf("!")+1)).toString();
                }
            } else {
                try {
                    if (canonicalize) {
                        classPath = new JRubyFile(filename).getCanonicalPath().toString();
                    } else {
                        classPath = new JRubyFile(filename).toString();
                    }
                } catch (IOException ioe) {
                    // could not get canonical path, just use given path
                    classPath = filename;
                }
            }

            if (parent != null && parent.length() > 0) {
                String parentPath;
                try {
                    if (canonicalize) {
                        parentPath = new JRubyFile(parent).getCanonicalPath().toString();
                    } else {
                        parentPath = new JRubyFile(parent).toString();
                    }
                } catch (IOException ioe) {
                    // could not get canonical path, just use given path
                    parentPath = parent;
                }
                if (!classPath.startsWith(parentPath)) {
                    throw new FileNotFoundException("File path " + classPath +
                            " does not start with parent path " + parentPath);
                }
                int parentLength = parentPath.length();
                classPath = classPath.substring(parentLength);
            }
            
            String[] pathElements = PATH_SPLIT.split(classPath);
            StringBuilder newPath = new StringBuilder(prefix);
            
            for (String element : pathElements) {
                if (element.length() <= 0) {
                    continue;
                }
                
                if (newPath.length() > 0) {
                    newPath.append("/");
                }
                
                if (!Character.isJavaIdentifierStart(element.charAt(0))) {
                    newPath.append("$");
                }
                newPath.append(mangleStringForCleanJavaIdentifier(element));
            }
            
            // strip off "_dot_rb" for .rb files
            int dotRbIndex = newPath.indexOf("_dot_rb");
            if (dotRbIndex != -1 && dotRbIndex == newPath.length() - 7) {
                newPath.delete(dotRbIndex, dotRbIndex + 7);
            }

            return newPath.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException(ioe);
        }
    }
    
    public static String mangleStringForCleanJavaIdentifier(String name) {
        char[] characters = name.toCharArray();
        StringBuilder cleanBuffer = new StringBuilder();
        boolean prevWasReplaced = false;
        for (int i = 0; i < characters.length; i++) {
            if ((i == 0 && Character.isJavaIdentifierStart(characters[i]))
                    || Character.isJavaIdentifierPart(characters[i])) {
                cleanBuffer.append(characters[i]);
                prevWasReplaced = false;
            } else {
                if (!prevWasReplaced) {
                    cleanBuffer.append("_");
                }
                prevWasReplaced = true;
                switch (characters[i]) {
                case '?':
                    cleanBuffer.append("p_");
                    continue;
                case '!':
                    cleanBuffer.append("b_");
                    continue;
                case '<':
                    cleanBuffer.append("lt_");
                    continue;
                case '>':
                    cleanBuffer.append("gt_");
                    continue;
                case '=':
                    cleanBuffer.append("equal_");
                    continue;
                case '[':
                    if ((i + 1) < characters.length && characters[i + 1] == ']') {
                        cleanBuffer.append("aref_");
                        i++;
                    } else {
                        cleanBuffer.append("lbracket_");
                    }
                    continue;
                case ']':
                    cleanBuffer.append("rbracket_");
                    continue;
                case '+':
                    cleanBuffer.append("plus_");
                    continue;
                case '-':
                    cleanBuffer.append("minus_");
                    continue;
                case '*':
                    cleanBuffer.append("times_");
                    continue;
                case '/':
                    cleanBuffer.append("div_");
                    continue;
                case '&':
                    cleanBuffer.append("and_");
                    continue;
                case '.':
                    cleanBuffer.append("dot_");
                    continue;
                default:
                    cleanBuffer.append(Integer.toHexString(characters[i])).append("_");
                }
            }
        }
        return cleanBuffer.toString();
    }
}
