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
 * 
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
/**
 * $Id: $
 */
package org.jruby.ext.posix.util;

import java.util.HashMap;
import java.util.Map;

public class Platform {
    public static final String OS_NAME = System.getProperty("os.name");
    public static final String OS_NAME_LC = OS_NAME.toLowerCase();

    // Generic Windows designation
    private static final String WINDOWS = "windows";
    // For Windows 95, 98... Would these platforms actually work?
    private static final String WINDOWS_9X = "windows 9";
    // TODO: Windows ME?
    private static final String WINDOWS_NT = "nt";
    private static final String WINDOWS_20X = "windows 2";
    private static final String WINDOWS_XP = "windows xp";
    // TODO:  For Windows Server 2003, 2008
    private static final String WINDOWS_VISTA = "vista";
    private static final String MAC_OS = "mac os";
    private static final String DARWIN = "darwin";
    private static final String FREEBSD = "freebsd";
    private static final String LINUX = "linux";
    private static final String SOLARIS = "sunos";
    // Ruby's designation for some platforms, minus version numbers in some cases
    private static final String RUBY_DARWIN = "darwin";
    private static final String RUBY_LINUX = "linux";
    private static final String RUBY_WIN32 = "mswin32";
    private static final String RUBY_SOLARIS = "solaris";
    private static final String RUBY_FREEBSD = "freebsd";
    private static final String RUBY_AIX = "aix";
    // TODO: investigate supported platforms for OpenJDK7?

    public static final boolean IS_WINDOWS = OS_NAME_LC.indexOf(WINDOWS) != -1;
    public static final boolean IS_WINDOWS_9X = OS_NAME_LC.indexOf(WINDOWS_9X) > -1;
    public static final boolean IS_WINDOWS_NT = IS_WINDOWS && OS_NAME_LC.indexOf(WINDOWS_NT) > -1;
    public static final boolean IS_WINDOWS_20X = OS_NAME_LC.indexOf(WINDOWS_20X) > -1;
    public static final boolean IS_WINDOWS_XP = OS_NAME_LC.indexOf(WINDOWS_XP) > -1;
    public static final boolean IS_WINDOWS_VISTA = IS_WINDOWS && OS_NAME_LC.indexOf(WINDOWS_VISTA) > -1;
    public static final boolean IS_MAC = OS_NAME_LC.startsWith(MAC_OS) || OS_NAME_LC.startsWith(DARWIN);
    public static final boolean IS_FREEBSD = OS_NAME_LC.startsWith(FREEBSD);
    public static final boolean IS_LINUX = OS_NAME_LC.startsWith(LINUX);   
    public static final boolean IS_SOLARIS = OS_NAME_LC.startsWith(SOLARIS);
    public static final boolean IS_BSD = IS_MAC || IS_FREEBSD;
    
    public static final String envCommand() {
        if (IS_WINDOWS) {
            if (IS_WINDOWS_9X) {
                return "command.com /c set";
            } else if (IS_WINDOWS_NT || IS_WINDOWS_20X || IS_WINDOWS_XP || IS_WINDOWS_VISTA) {
                return "cmd.exe /c set";
            }
        }
        return "env";
    }

    public static final boolean IS_32_BIT = "32".equals(getProperty("sun.arch.data.model", "32"));
    public static final boolean IS_64_BIT = "64".equals(getProperty("sun.arch.data.model", "64"));

    public static final String ARCH = System.getProperty("os.arch");
    
    /** This is a map from Java's "friendly" OS names to those used by Ruby */
    public static final Map<String, String> RUBY_OS_NAMES = new HashMap<String, String>();
    static {
        RUBY_OS_NAMES.put("Mac OS X", RUBY_DARWIN);
        RUBY_OS_NAMES.put("Darwin", RUBY_DARWIN);
        RUBY_OS_NAMES.put("Linux", RUBY_LINUX);
        RUBY_OS_NAMES.put("Windows 95", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows 98", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows Me", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows NT", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows XP", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows 2003", RUBY_WIN32);
        RUBY_OS_NAMES.put("Windows Vista", RUBY_WIN32);
        RUBY_OS_NAMES.put("Solaris", RUBY_SOLARIS);
        RUBY_OS_NAMES.put("FreeBSD", RUBY_FREEBSD);
        RUBY_OS_NAMES.put("AIX", RUBY_AIX);
    }
    
    public static String getOSName()  {
        String theOSName = RUBY_OS_NAMES.get(OS_NAME);
        
        return theOSName == null ? OS_NAME : theOSName;
    }
    
    /**
     * An extension over <code>System.getProperty</code> method.
     * Handles security restrictions, and returns the default
     * value if the access to the property is restricted.
     * @param property The system property name.
     * @param defValue The default value.
     * @return The value of the system property,
     *         or the default value.
     */
    public static String getProperty(String property, String defValue) {
        try {
            return System.getProperty(property, defValue);
        } catch (SecurityException se) {
            return defValue;
        }
    }
}
