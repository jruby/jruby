/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

/**
 *
 * @author headius
 */
public class JavaNameMangler {
    public static String mangleFilenameForClasspath(String filename) {
        String classPath = filename;
        if (classPath.startsWith("./")) classPath = classPath.substring(2);

        classPath = classPath.substring(0, classPath.lastIndexOf(".")).replace('-', '_').replace('.', '_');
        int lastSlashIndex = classPath.lastIndexOf('/');
        if (!Character.isJavaIdentifierStart(classPath.charAt(lastSlashIndex + 1))) {
            if (lastSlashIndex == -1) {
                classPath = "_" + classPath;
            } else {
                classPath = classPath.substring(0, lastSlashIndex + 1) + "_" + classPath.substring(lastSlashIndex + 1);
            }
        }
        
        return classPath;
    }
    
    public static String mangledFilenameForStartupClasspath(String filename) {
        String classname;
        if (filename.equals("-e")) {
            classname = "__dash_e__";
        } else {
            classname = filename.replace('\\', '/').replaceAll(".rb", "");
        }
        // remove leading / or ./ from classname, since it will muck up the dotted name
        if (classname.startsWith("/")) classname = classname.substring(1);
        if (classname.startsWith("./")) classname = classname.substring(2);
        
        return classname;
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
                default:
                    cleanBuffer.append(Integer.toHexString(characters[i])).append("_");
                }
            }
        }
        return cleanBuffer.toString();
    }
}
