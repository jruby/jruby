/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import java.io.IOException;
import java.util.regex.Pattern;

/**
 *
 * @author headius
 */
public class JavaNameMangler {
    public static final Pattern PATH_SPLIT = Pattern.compile("[/\\\\]");
    
    public static String mangleFilenameForClasspath(String filename) {
        try {
            String classPath = new JRubyFile(filename).getCanonicalPath().toString();
            String[] pathElements = PATH_SPLIT.split(classPath);
            StringBuffer newPath = new StringBuffer("ruby");
            
            for (String element : pathElements) {
                if (element.length() <= 0) {
                    continue;
                }
                
                newPath.append("/");
                if (!Character.isJavaIdentifierStart(element.charAt(0))) {
                    newPath.append("$");
                }
                newPath.append(mangleMethodForCleanJavaIdentifier(element));
            }

            return newPath.toString();
        } catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException(ioe);
        }
    }
    
    public static String mangledFilenameForStartupClasspath(String filename) {
        if (filename.equals("-e")) {
            return "__dash_e__";
        }
        
        return mangleFilenameForClasspath(filename);
    }
    
    public static String mangleMethodForCleanJavaIdentifier(String name) {
        char[] characters = name.toCharArray();
        StringBuffer cleanBuffer = new StringBuffer();
        boolean prevWasReplaced = false;
        for (int i = 0; i < characters.length; i++) {
            if (Character.isJavaIdentifierStart(characters[i])) {
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
                        // can this ever happen?
                        cleanBuffer.append("lbracket_");
                    }
                    continue;
                case ']':
                    // given [ logic above, can this ever happen?
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
