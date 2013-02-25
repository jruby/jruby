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
package org.jruby.embed.osgi.internal;
import java.net.URL;
import java.util.StringTokenizer;

import org.jruby.Ruby;
import org.jruby.RubyInstanceConfig.LoadServiceCreator;
import org.jruby.embed.osgi.utils.OSGiFileLocator;
import org.jruby.platform.Platform;
import org.jruby.runtime.load.Library;
import org.jruby.runtime.load.LoadService;
import org.jruby.runtime.load.LoadServiceResource;
import org.osgi.framework.Bundle;

/**
 * @author hmalphettes
 * 
 * Load scripts and java classes directly from the OSGi bundles.
 * bundle:/symbolic.name/
 */
public class OSGiLoadService extends LoadService {
    
    public static final String OSGI_BUNDLE_CLASSPATH_SCHEME = "osgibundle:/";
    
    public static LoadServiceCreator OSGI_DEFAULT = new LoadServiceCreator() {
        public LoadService create(Ruby runtime) {
            if (runtime.is1_9()) {
                return new OSGiLoadService19(runtime);
            }
            return new OSGiLoadService(runtime);
        }
    };

    /**
     * Default constructor
     * Optional constructor (why?)
     * @param runtime
     */
    public OSGiLoadService(Ruby runtime) {
        super(runtime);
//        super.searchers.add(new OSGiBundlesSearcher());
    }

    /**
     * Support for 'bundle:/' to look for libraries in osgi bundles
     * or classes or ruby files.
     *
     * @mri rb_find_file
     * @param name the file to find, this is a path name
     * @return the correct file
     */
    @Override
    protected LoadServiceResource findFileInClasspath(String name) {
        if (name.startsWith(OSGI_BUNDLE_CLASSPATH_SCHEME)) {
            name = cleanupFindName(name);
            StringTokenizer tokenizer = new StringTokenizer(name, "/", false);
            tokenizer.nextToken();
            String symname = tokenizer.nextToken();
            StringBuilder sb = new StringBuilder();
            if (!tokenizer.hasMoreTokens()) {
                sb.append('/');
            } else {
                while (tokenizer.hasMoreTokens()) {
                    sb.append('/');
                    sb.append(tokenizer.nextToken());
                }
            }
            Bundle bundle = OSGiFileLocator.getBundle(symname);
            if (bundle != null) {
                URL url = bundle.getEntry(sb.toString());
                if (url != null) {
                    return new LoadServiceResource(
                            OSGiFileLocator.getLocalURL(url), name);
                }
            }
        }
        return super.findFileInClasspath(name);
    }
    
    /**
     * Support for 'bundle:/' to look for libraries in osgi bundles.
     */
    @Override
    protected Library createLibrary(SearchState state, LoadServiceResource resource) {
        if (resource == null) {
            return null;
        }
        String file = state.loadName;
        if (file.startsWith(OSGI_BUNDLE_CLASSPATH_SCHEME)) {
            file = cleanupFindName(file);
            StringTokenizer tokenizer = new StringTokenizer(file, "/", false);
            tokenizer.nextToken();
            String symname = tokenizer.nextToken();
            Bundle bundle = OSGiFileLocator.getBundle(symname);
            if (bundle != null) {
                return new OSGiBundleLibrary(bundle);
            }
        }
        return super.createLibrary(state, resource);
    }
    
    /**
     * Remove the extension when they are misleading.
     * @param name
     * @return
     */
    private String cleanupFindName(String name) {
        if (name.endsWith(".jar")) {
            return name.substring(0, name.length()-".jar".length());
        } else if (name.endsWith(".class")) {
            return name.substring(0, name.length()-".class".length());
        } else {
            return name;
        }
    }

}
class OSGiLoadService19 extends OSGiLoadService {
    public OSGiLoadService19(Ruby runtime) {
        super(runtime);
    }

    @Override
    protected String resolveLoadName(LoadServiceResource foundResource, String previousPath) {
        String path = foundResource.getAbsolutePath();
        if (Platform.IS_WINDOWS) {
            path = path.replace('\\', '/');
        }
        return path;
    }

}
