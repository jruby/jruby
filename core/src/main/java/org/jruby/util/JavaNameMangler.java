/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import org.jruby.platform.Platform;

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
        if (filename.length() == 2 && filename.charAt(0) == '-' && filename.charAt(1) == 'e') {
            return "ruby/__dash_e__"; // "-e"
        }

        return mangleFilenameForClasspath(filename, null, "", false, false);
    }

    public static String mangleFilenameForClasspath(String filename) {
        return mangleFilenameForClasspath(filename, null, "ruby");
    }

    public static String mangleFilenameForClasspath(String filename, String parent, String prefix) {
        return mangleFilenameForClasspath(filename, parent, prefix, true, false);
    }

    public static String mangleFilenameForClasspath(String filename, String parent, String prefix, boolean canonicalize,
          boolean preserveIdentifiers) {
        try {
            String classPath; final int idx = filename.indexOf('!');
            if (idx != -1) {
                String before = filename.substring(6, idx);
                if (canonicalize) {
                    classPath = new JRubyFile(before + filename.substring(idx + 1)).getCanonicalPath();
                } else {
                    classPath = new JRubyFile(before + filename.substring(idx + 1)).toString();
                }
            } else {
                try {
                    if (canonicalize) {
                        classPath = new JRubyFile(filename).getCanonicalPath();
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
                        parentPath = new JRubyFile(parent).getCanonicalPath();
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
                    newPath.append('/');
                }

                if (!Character.isJavaIdentifierStart(element.charAt(0))) {
                    newPath.append('$');
                }

                String pathId = element;
                if (!preserveIdentifiers) {
                    pathId = mangleStringForCleanJavaIdentifier(element);
                }
                newPath.append(pathId);
            }

            // strip off "_dot_rb" for .rb files
            int dotRbIndex = newPath.indexOf("_dot_rb");
            if (dotRbIndex != -1 && dotRbIndex == newPath.length() - 7) {
                newPath.delete(dotRbIndex, dotRbIndex + 7);
            }

            return newPath.toString();
        }catch (IOException ioe) {
            ioe.printStackTrace();
            throw new RuntimeException(ioe);
        }
    }

    public static String mangleStringForCleanJavaIdentifier(String name) {
        final char[] chars = name.toCharArray();
        final int len = chars.length;
        StringBuilder cleanBuffer = new StringBuilder(len * 2);
        boolean prevWasReplaced = false;
        for (int i = 0; i < len; i++) {
            if ((i == 0 && Character.isJavaIdentifierStart(chars[i]))
                    || Character.isJavaIdentifierPart(chars[i])) {
                cleanBuffer.append(chars[i]);
                prevWasReplaced = false;
            } else {
                if (!prevWasReplaced) {
                    cleanBuffer.append('_');
                }
                prevWasReplaced = true;
                switch (chars[i]) {
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
                    if ((i + 1) < len && chars[i + 1] == ']') {
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
                case '@':
                    cleanBuffer.append("at_");
                default:
                    cleanBuffer.append(Integer.toHexString(chars[i])).append('_');
                }
            }
        }
        return cleanBuffer.toString();
    }

    private static final String DANGEROUS_CHARS = "\\/.;:$[]<>";
    private static final String REPLACEMENT_CHARS = "-|,?!%{}^_";
    private static final char ESCAPE_C = '\\';
    private static final char NULL_ESCAPE_C = '=';
    private static final String NULL_ESCAPE = ESCAPE_C +""+ NULL_ESCAPE_C;

    public static String mangleMethodName(String name) {
        // scan for characters that need escaping
        StringBuilder builder = null; // lazy
        for (int i = 0; i < name.length(); i++) {
            char candidate = name.charAt(i);
            int escape = escapeChar(candidate);
            if (escape != -1) {
                if (builder == null) {
                    builder = new StringBuilder();
                    // start mangled with '='
                    builder.append(NULL_ESCAPE);
                    builder.append(name.substring(0, i));
                }
                builder.append(ESCAPE_C).append((char) escape);
            }
            else if (builder != null) builder.append(candidate);
        }

        if (builder != null) return builder.toString();

        return name;
    }

    public static String demangleMethodName(String name) {
        if (!name.startsWith(NULL_ESCAPE)) return name;
        final int len = name.length();
        StringBuilder builder = new StringBuilder(len);
        for (int i = 2; i < len; i++) {
            char candidate = name.charAt(i);
            if (candidate == ESCAPE_C) {
                i++;
                char escaped = name.charAt(i);
                char unescape = unescapeChar(escaped);
                builder.append(unescape);
            }
            else builder.append(candidate);
        }

        return builder.toString();
    }

    public static boolean willMethodMangleOk(String name) {
        if (Platform.IS_IBM) {
            // IBM's JVM is much less forgiving, so we disallow anythign with non-alphanumeric, _, and $
            for (char c : name.toCharArray()) {
                if (!Character.isJavaIdentifierPart(c)) return false;
            }
        }

        // other JVMs will accept our mangling algorithm
        return true;
    }

    private static int escapeChar(char character) {
        int index = DANGEROUS_CHARS.indexOf(character);
        if (index == -1) return -1;
        return REPLACEMENT_CHARS.charAt(index);
    }

    private static char unescapeChar(char character) {
        return DANGEROUS_CHARS.charAt(REPLACEMENT_CHARS.indexOf(character));
    }
}
