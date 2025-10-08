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
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
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
 ***** END LICENSE BLOCK ****
 */
package org.jruby.main;

/**
 * A specialized "main" entry point that assumes it will run a specific file
 * (jar-bootstrap.rb) when launching. This allows modifying only the manifest's
 * Main-Class and adding this file to create a self-contained executable JRuby
 * application.
 *
 * Example usage:
 *
 * <pre>
 * ~/projects/jruby $ cp lib/jruby.jar myapp.jar
 *
 * ~/projects/jruby $ cat jar-bootstrap.rb
 * puts "hello"
 *
 * ~/projects/jruby $ jar ufe myapp.jar org.jruby.JarBootstrapMain jar-bootstrap.rb
 *
 * ~/projects/jruby $ java -jar myapp.jar
 * hello
 * </pre>
 */
public class JarBootstrapMain {
    public static final String JAR_BOOTSTRAP = "classpath:/jar-bootstrap.rb";
    public static void main(String[] args) {
        String[] newArgs = new String[args.length + 1];
        newArgs[0] = JAR_BOOTSTRAP;
        System.arraycopy(args, 0, newArgs, 1, args.length);
        Main.main(newArgs);
    }
}
