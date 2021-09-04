/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Alternatively, the contents of this file may be used under the terms of
 * either of the GNU General Public License Version 2 or later (the "GPL"),
 * or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
 * in which case the provisions of the GPL or the LGPL are applicable instead
 * of those above. If you wish to allow use of your version of this file only
 * under the terms of either the GPL or the LGPL, and not to allow others to
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby.util;

import org.jruby.ir.IRClassBody;
import org.jruby.ir.IRClosure;
import org.jruby.ir.IRMetaClassBody;
import org.jruby.ir.IRMethod;
import org.jruby.ir.IRModuleBody;
import org.jruby.ir.IRScope;
import org.jruby.ir.IRScriptBody;
import org.jruby.ir.interpreter.Interpreter;
import org.jruby.platform.Platform;
import org.jruby.runtime.backtrace.FrameType;

import java.io.IOException;
import java.util.List;
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
        String classPath; final int idx = filename.indexOf('!');
        if (idx != -1) {
            String before = filename.substring(6, idx);
            try {
                if (canonicalize) {
                    classPath = new JRubyFile(before + filename.substring(idx + 1)).getCanonicalPath();
                } else {
                    classPath = new JRubyFile(before + filename.substring(idx + 1)).toString();
                }
            } catch (IOException ioe) {
                throw new RuntimeException(ioe);
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
                throw new RuntimeException("File path " + classPath +
                        " does not start with parent path " + parentPath);
            }
            int parentLength = parentPath.length();
            classPath = classPath.substring(parentLength);
        }

        String[] pathElements = PATH_SPLIT.split(classPath);
        StringBuilder newPath = new StringBuilder(classPath.length() + 16).append(prefix);

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

            if (!preserveIdentifiers) {
                mangleStringForCleanJavaIdentifier(newPath, element);
            }
            else {
                newPath.append(element);
            }
        }

        // strip off "_dot_rb" for .rb files
        int dotRbIndex = newPath.indexOf("_dot_rb");
        if (dotRbIndex != -1 && dotRbIndex == newPath.length() - 7) {
            newPath.delete(dotRbIndex, dotRbIndex + 7);
        }

        return newPath.toString();
    }

    public static String mangleStringForCleanJavaIdentifier(final String name) {
        StringBuilder cleanBuffer = new StringBuilder(name.length() * 3);
        mangleStringForCleanJavaIdentifier(cleanBuffer, name);
        return cleanBuffer.toString();
    }

    private static void mangleStringForCleanJavaIdentifier(final StringBuilder buffer,
        final String name) {
        final char[] chars = name.toCharArray();
        final int len = chars.length;
        buffer.ensureCapacity(buffer.length() + len * 2);
        boolean prevWasReplaced = false;
        for (int i = 0; i < len; i++) {
            if ((i == 0 && Character.isJavaIdentifierStart(chars[i]))
                    || Character.isJavaIdentifierPart(chars[i])) {
                buffer.append(chars[i]);
                prevWasReplaced = false;
                continue;
            }

            if (!prevWasReplaced) buffer.append('_');
            prevWasReplaced = true;

            switch (chars[i]) {
            case '?':
                buffer.append("p_");
                continue;
            case '!':
                buffer.append("b_");
                continue;
            case '<':
                buffer.append("lt_");
                continue;
            case '>':
                buffer.append("gt_");
                continue;
            case '=':
                buffer.append("equal_");
                continue;
            case '[':
                if ((i + 1) < len && chars[i + 1] == ']') {
                    buffer.append("aref_");
                    i++;
                } else {
                    buffer.append("lbracket_");
                }
                continue;
            case ']':
                buffer.append("rbracket_");
                continue;
            case '+':
                buffer.append("plus_");
                continue;
            case '-':
                buffer.append("minus_");
                continue;
            case '*':
                buffer.append("times_");
                continue;
            case '/':
                buffer.append("div_");
                continue;
            case '&':
                buffer.append("and_");
                continue;
            case '.':
                buffer.append("dot_");
                continue;
            case '@':
                buffer.append("at_");
            default:
                buffer.append(Integer.toHexString(chars[i])).append('_');
            }
        }
    }

    private static final String DANGEROUS_CHARS = "\\/.;:$[]<>";
    private static final String REPLACEMENT_CHARS = "-|,?!%{}^_";
    private static final char ESCAPE_C = '\\';
    private static final char NULL_ESCAPE_C = '=';
    private static final String NULL_ESCAPE = ESCAPE_C +""+ NULL_ESCAPE_C;

    public static String mangleMethodName(final String name) {
        return mangleMethodNameInternal(name).toString();
    }

    private static CharSequence mangleMethodNameInternal(final String name) {
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

        return builder != null ? builder : name;
    }

    public static String demangleMethodName(String name) {
        return demangleMethodNameInternal(name).toString();
    }

    private static CharSequence demangleMethodNameInternal(String name) {
        if (!name.startsWith(NULL_ESCAPE)) return name;

        final int len = name.length();
        StringBuilder builder = new StringBuilder(len);
        for (int i = 2; i < len; i++) { // 2 == NULL_ESCAPE.length
            final char c = name.charAt(i);
            if (c == ESCAPE_C) {
                i++;
                builder.append( unescapeChar(name.charAt(i)) );
            }
            else builder.append(c);
        }
        return builder;
    }

    private static int escapeChar(char character) {
        int index = DANGEROUS_CHARS.indexOf(character);
        if (index == -1) return -1;
        return REPLACEMENT_CHARS.charAt(index);
    }

    private static char unescapeChar(char character) {
        return DANGEROUS_CHARS.charAt(REPLACEMENT_CHARS.indexOf(character));
    }

    // FIXME: bytelist_love - if we want these mangled names to display properly we should be building this up with encoded data.
    public static String encodeScopeForBacktrace(IRScope scope) {
        if (scope instanceof IRMethod) {
            return "RUBY$method$" + mangleMethodNameInternal(scope.getId());
        }
        if (scope instanceof IRClosure) {
            IRScope ancestorScope = scope.getNearestTopLocalVariableScope();
            String name;
            if (ancestorScope instanceof IRScriptBody) {
                name = Interpreter.ROOT;
            } else {
                name = ancestorScope.getId();
            }
            return "RUBY$block$" + mangleMethodNameInternal(name);
        }
        if (scope instanceof IRMetaClassBody) {
            return "RUBY$metaclass";
        }
        if (scope instanceof IRClassBody) {
            return "RUBY$class$" + mangleMethodNameInternal(scope.getId());
        }
        if (scope instanceof IRModuleBody) {
            return "RUBY$module$" + mangleMethodNameInternal(scope.getId());
        }
        if (scope instanceof IRScriptBody) {
            return "RUBY$script";
        }
        throw new IllegalStateException("unknown scope type for backtrace encoding: " + scope.getClass());
    }

    public static final String VARARGS_MARKER = "$__VARARGS__";

    // returns location $ type $ methodName as 3 elements or null if this is an invalid mangled name
    public static List<String> decodeMethodTuple(String methodName) {
        if (!methodName.startsWith("RUBY$")) return null;

        return StringSupport.split(methodName, '$');
    }

    public static String decodeMethodName(FrameType type, List<String> mangledTuple) {
        switch (type) {
            case ROOT:    return "<main>";
            case METACLASS: return "singleton class";
            case VARARGS_WRAPPER:
            case METHOD:    return demangleMethodName(mangledTuple.get(2));
            case BLOCK:     return ""+demangleMethodNameInternal(mangledTuple.get(2));
            case CLASS:     return "<class:" + demangleMethodNameInternal(mangledTuple.get(2)) + '>';
            case MODULE:    return "<module:" + demangleMethodNameInternal(mangledTuple.get(2)) + '>';
        }

        return null; // not-reached
    }

    public static FrameType decodeFrameTypeFromMangledName(String type) {
        switch (type) {
            case "script":    return FrameType.ROOT;
            case "metaclass": return FrameType.METACLASS;
            case "method":    return FrameType.METHOD;
            case "block":     return FrameType.BLOCK;
            case "class":     return FrameType.MODULE;
            case "module":    return FrameType.CLASS;
        }
        throw new IllegalStateException("unknown encoded method type '" + type);

    }

    @Deprecated
    public static boolean willMethodMangleOk(CharSequence name) {
        if (false && Platform.IS_IBM) {
            // IBM's JVM is much less forgiving, so we disallow anything with non-alphanumeric, _, and $
            for ( int i = 0; i < name.length(); i++ ) {
                if (!Character.isJavaIdentifierPart(name.charAt(i))) return false;
            }
        }
        // other JVMs will accept our mangling algorithm
        return true;
    }
}
