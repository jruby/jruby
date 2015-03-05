/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2001 Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Copyright (C) 2001-2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
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
package org.jruby.runtime;

public final class Constants {
    public static final String PLATFORM = "java";

    public static final int MARSHAL_MAJOR = 4;
    public static final int MARSHAL_MINOR = 8;

    public static final String RUBY_MAJOR_VERSION = "@version.ruby.major@";
    public static final String RUBY_VERSION = "@version.ruby@";
    public static final int    RUBY_REVISION = Integer.parseInt("@version.ruby.revision@");

    public static final String COMPILE_DATE = "@build.date@";
    public static final String VERSION = "@version.jruby@";
    public static final String BUILD = "java@base.java.version@";
    public static final String TARGET = "java@base.javac.version@";
    public static final String REVISION;
    public static final String ENGINE = "jruby";
    
    public static final String JODA_TIME_VERSION = "@joda.time.version@";
    public static final String TZDATA_VERSION = "@tzdata.version@";
    
    public static final String DEFAULT_RUBY_VERSION = "2.1";
    
    /**
     * Default size for chained compilation.
     */
    public static final int CHAINED_COMPILE_LINE_COUNT_DEFAULT = 500;
    
    /**
     * The max count of active methods eligible for JIT-compilation.
     */
    public static final int JIT_MAX_METHODS_LIMIT = 4096;

    /**
     * The max size of JIT-compiled methods (full class size) allowed.
     */
    public static final int JIT_MAX_SIZE_LIMIT = 30000;

    /**
     * The JIT threshold to the specified method invocation count.
     */
    public static final int JIT_THRESHOLD = 50;
    
    private static String jruby_revision = "@jruby.revision@";

    @Deprecated
    public static final String JRUBY_PROPERTIES = "/org/jruby/jruby.properties";

    static {
        // This is populated here to avoid javac propagating the value to consumers
        // Broken apart like this to prevent substitution
        if (jruby_revision.equals("@" + "jruby.revision" + "@")) {
            // use a bogus revision
            REVISION = "fffffff";
        } else {
            REVISION = jruby_revision;
        }
    }

    private Constants() {}

    @Deprecated
    public static final int    RUBY_PATCHLEVEL = 0;
}
