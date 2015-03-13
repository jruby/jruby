/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.jruby.util;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
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
        if (filename.equals("-e")) {
            return "ruby/__dash_e__";
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
                case '@':
                    cleanBuffer.append("at_");
                default:
                    cleanBuffer.append(Integer.toHexString(characters[i])).append("_");
                }
            }
        }
        return cleanBuffer.toString();
    }

    private static final String DANGEROUS_CHARS = "\\/.;:$[]<>";
    private static final String REPLACEMENT_CHARS = "-|,?!%{}^_";
    private static final char ESCAPE_C = '\\';
    private static final char NULL_ESCAPE_C = '=';
    private static final String NULL_ESCAPE = ESCAPE_C+""+NULL_ESCAPE_C;

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
                builder.append(ESCAPE_C).append((char)escape);
            } else if (builder != null) builder.append(candidate);
        }

        if (builder != null) return builder.toString();

        return name;
    }

    public static String demangleMethodName(String name) {
        if (!name.startsWith(NULL_ESCAPE)) return name;

        StringBuilder builder = new StringBuilder();
        for (int i = 2; i < name.length(); i++) {
            char candidate = name.charAt(i);
            if (candidate == ESCAPE_C) {
                i++;
                char escaped = name.charAt(i);
                char unescape = unescapeChar(escaped);
                builder.append(unescape);
            } else builder.append(candidate);
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

    public static String encodeScopeForBacktrace(IRScope scope) {
        if (scope instanceof IRMethod) {
            return "RUBY$method$" + mangleMethodName(scope.getName());
        } else if (scope instanceof IRClosure) {
            return "RUBY$block$" + mangleMethodName(scope.getNearestTopLocalVariableScope().getName());
        } else if (scope instanceof IRMetaClassBody) {
            return "RUBY$metaclass";
        } else if (scope instanceof IRClassBody) {
            return "RUBY$class$" + mangleMethodName(scope.getName());
        } else if (scope instanceof IRModuleBody) {
            return "RUBY$module$" + mangleMethodName(scope.getName());
        } else if (scope instanceof IRScriptBody) {
            return "RUBY$script";
        }
        throw new RuntimeException("unknown scope type for backtrace encoding: " + scope.getClass());
    }

    public static String decodeMethodForBacktrace(String methodName) {
        if (!methodName.startsWith("RUBY$")) return null;

        String[] elts = methodName.split("\\$");
        String type = elts[1];
        String name;

        // root body gets named (root)
        switch (type) {
            case "script":
                return "<top>";
            case "metaclass":
                return "singleton class";
        }

        // remaining cases have an encoded name
        name = demangleMethodName(elts[2]);
        switch (type) {
            case "method":  return name;
            case "block":   return "block in " + name;
            case "class":   // fall through
            case "module":  return "<" + type + ":" + name + ">";
            default:
                throw new RuntimeException("unknown encoded method type '" + type + "' from '" + methodName);
        }
    }
}
