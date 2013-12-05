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
 * Copyright (C) 2002-2011 JRuby Community
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

package org.jruby.embed.osgi;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import org.jruby.embed.EmbedEvalUnit;
import org.jruby.embed.EvalFailedException;
import org.jruby.embed.LocalContextScope;
import org.jruby.embed.LocalVariableBehavior;
import org.jruby.embed.ScriptingContainer;
import org.jruby.embed.osgi.internal.JRubyOSGiBundleClassLoader;
import org.jruby.embed.osgi.internal.OSGiLoadService;
import org.jruby.embed.osgi.utils.OSGiFileLocator;
import org.osgi.framework.Bundle;

/**
 * Helpers to create a ScriptingContainer and set it up so it lives as well
 * as possible in the OSGi world.
 * <p>
 * Currently:
 * <ol>
 * <li>Access to the java classes and resources provided by the osgi bundle.</li>
 * <li>Setup of jruby home pointing at the jruby bundle by default. Supporting unzipped jruby bundle for now.</li>
 * </ol>
 * </p>
 * <p>
 * TODO: look into using the LoadService of jruby.
 * Look if it would be possible to reuse the base runtime and minimize the cost of new
 * jruby runtimes. 
 * </p>
 * @author hmalphettes
 *
 */
public class OSGiScriptingContainer extends ScriptingContainer {

    /**
     * @return A scripting container where the classloader can find classes
     * in the osgi creator bundle and where the jruby home is set to point to
     * the one in the jruby's bundle home folder.
     * scope: LocalContextScope.SINGLETHREAD; behavior: LocalVariableBehavior.TRANSIENT
     */
    public OSGiScriptingContainer(Bundle creator) {
        this(creator, LocalContextScope.SINGLETHREAD, LocalVariableBehavior.TRANSIENT);
    }
    /**
     * @param scope if null, LocalContextScope.SINGLETHREAD
     * @param behavior if null, LocalVariableBehavior.TRANSIENT
     * @return A scripting container where the classloader can find classes
     * in the osgi creator bundle and where the jruby home is set to point to
     * the one in the jruby's bundle home folder.
     */
    public OSGiScriptingContainer(Bundle creator,
            LocalContextScope scope, LocalVariableBehavior behavior) {
        super(scope, behavior);
        if (creator != null) {
            super.setClassLoader(new JRubyOSGiBundleClassLoader(creator));
        } else {
            super.setClassLoader(new JRubyOSGiBundleClassLoader());
        }
        super.setLoadServiceCreator(OSGiLoadService.OSGI_DEFAULT);
        try {
            super.setHomeDirectory(OSGiFileLocator.getJRubyHomeFolder().getAbsolutePath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param bundleSymbolicName The bundle where the script is located. Lazily added to the
     * loader of the OSGiScriptingContainer. (require bundle:/... is implicitly done here)
     * @param path The entry in the bundle
     * @return
     */
    public Object runScriptlet(String bundleSymbolicName, String path) {
        Bundle bundle = OSGiFileLocator.getBundle(bundleSymbolicName);
        if (bundle == null) {
            throw new IllegalArgumentException("Unable to find the bundle '" + bundleSymbolicName + "'");
        }
        return runScriptlet(bundle, path);
    }

    /**
     * @param bundle The bundle where the script is located. Lazily added to the
     * loader of the OSGiScriptingContainer. (require bundle:/... is implicitly done here)
     * @param path The entry in the bundle
     * @return
     */
    public Object runScriptlet(Bundle bundle, String path) {
        URL url = bundle.getEntry(path);
        if (url == null) {
            throw new IllegalArgumentException("Unable to find the entry '" + path
                    + "' in the bundle " + bundle.getSymbolicName());
        }
        addToClassPath(bundle);
        InputStream istream = null;
        try {
            istream = new BufferedInputStream(url.openStream());
            return this.runScriptlet(istream, getFilename(bundle, path));
        } catch (IOException ioe) {
            throw new EvalFailedException(ioe);
        } finally {
            if (istream != null) try { istream.close(); } catch (IOException ioe) {};
        }
    }

    /**
     * Parses a script given by a input stream and return an object which can be run().
     * This allows the script to be parsed once and evaluated many times.
     * 
     * @param bundle is where the script is located
     * @param path is the entry in the bundle.
     * @param lines are linenumbers to display for parse errors and backtraces.
     *        This field is optional. Only the first argument is used for parsing.
     *        When no line number is specified, 0 is applied to.
     * @return an object which can be run
     */
    public EmbedEvalUnit parse(Bundle bundle, String path, int... lines) throws IOException {
        URL url = bundle.getEntry(path);
        InputStream istream = null;
        try {
            istream = new BufferedInputStream(url.openStream());
            return super.parse(istream, getFilename(bundle, path));
        } catch (IOException ioe) {
            throw new EvalFailedException(ioe);
        } finally {
            if (istream != null) try { istream.close(); } catch (IOException ioe) {};
        }
    }
    
    /**
     * @param bundle
     * @param path
     * @return a nice debugging string for the stack traces that is passed as the 'filename'
     * of this script to jruby.
     */
    private String getFilename(Bundle bundle, String path) {
        return "bundle:/" + bundle.getSymbolicName()
            + (path.charAt(0) == '/' ? path : ("/" + path));
    }
    
    /**
     * @param bundle Add a bundle to the jruby classloader.
     * Equivalent to require <code>"bundle:/#{bundle.symbolic.name}"</code>
     */
    public void addToClassPath(Bundle bundle) {
        getOSGiBundleClassLoader().addBundle(bundle);
    }
    
    /**
     * @return The ScriptingContainer's classloader casted to a JRubyOSGiBundleClassLoader.
     * It is the parent classloader of the actual's runtime's JRubyClassLoader.
     * It enables finding classes and resources in the OSGi environment.
     */
    public JRubyOSGiBundleClassLoader getOSGiBundleClassLoader() {
        return (JRubyOSGiBundleClassLoader)super.getClassLoader();
    }
    
}
