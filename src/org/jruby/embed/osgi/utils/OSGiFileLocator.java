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
package org.jruby.embed.osgi.utils;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLConnection;

import org.osgi.framework.Bundle;
import org.osgi.framework.BundleContext;
import org.osgi.framework.BundleException;
import org.osgi.framework.FrameworkUtil;

/**
 * Helper methods for the Ruby Runtime.
 * 
 * @author hmalphettes
 */
public class OSGiFileLocator {
	
	public static final String JRUBY_SYMBOLIC_NAME = "org.jruby.jruby";
	
	/**
	 * @return The home for gems and other files as provided by jruby.
	 */
	public static File getJRubyHomeFolder() throws IOException {
		//TODO: system property to override this?
	    //TODO: add some clutches to support jarred up jruby bundles?
	    return getFileInBundle(JRUBY_SYMBOLIC_NAME, "/META-INF/jruby.home");
	}
	
	public static File getFileInBundle(String symbolicName, String path) throws IOException {
		Bundle bundle = getBundle(symbolicName);
		if (bundle == null) {
			throw new IOException("Unable to find the bundle " + symbolicName);
		}
		return getFileInBundle(bundle, path);
	}
	public static File getFileInBundle(Bundle bundle, String path) throws IOException {
		URL url = null;
		try {
			url = getFileURL(bundle.getEntry(path));
			return new File(url.getFile());
		} catch (NullPointerException ne) {
			throw new IOException("Unable to find the " + path + " folder in the bundle '" 
					+ bundle.getSymbolicName() + "'; is the org.jruby.jruby bundle unzipped? ");
		} catch (Exception e) {
            IOException exception = new IOException("Unable to find the " + path + 
                    " folder in the bundle '" + bundle.getSymbolicName() + "'");
            exception.initCause(e); // Should be safe since IOException(String) should call super(String).
            throw exception;
		}
	}

	/**
	 * @param symbolicName
	 * @return The bundle with this symbolic name
	 */
	public static Bundle getBundle(String symbolicName) {
	    BundleContext bc = FrameworkUtil.getBundle(OSGiFileLocator.class).getBundleContext();
	    if (bc == null) {
	        //if the bundle was not activatged then let's activate it.
            try {
                FrameworkUtil.getBundle(OSGiFileLocator.class).start();
            } catch (BundleException e) {
                throw new IllegalStateException("Could not start the bundle "
                    + FrameworkUtil.getBundle(OSGiFileLocator.class).getSymbolicName());
            }
            bc = FrameworkUtil.getBundle(OSGiFileLocator.class).getBundleContext();
            if (bc == null) {
                //this should never happen
                throw new IllegalStateException("The bundle "
	                + FrameworkUtil.getBundle(OSGiFileLocator.class).getSymbolicName()
	                + " is not activated.");
            }
	    }
		for (Bundle b : FrameworkUtil.getBundle(OSGiFileLocator.class).getBundleContext().getBundles()) {
			if (b.getSymbolicName().equals(symbolicName)) {
				return b;
			}
		}
		return null;
	}
	
	//introspection on equinox to invoke the getLocalURL method on BundleURLConnection
	private static Method BUNDLE_URL_CONNECTION_getLocalURL = null;
	private static Method BUNDLE_URL_CONNECTION_getFileURL = null;
	/**
	 * Only useful for equinox: on felix we get the file:// or jar:// url already.
	 * Other OSGi implementations have not been tested
	 * <p>
	 * Get a URL to the bundle entry that uses a common protocol (i.e. file:
	 * jar: or http: etc.).  
	 * </p>
	 * @return a URL to the bundle entry that uses a common protocol
	 */
	public static URL getLocalURL(URL url) {
		if ("bundleresource".equals(url.getProtocol()) || "bundleentry".equals(url.getProtocol())) {
			try {
				URLConnection conn = url.openConnection();
				if (BUNDLE_URL_CONNECTION_getLocalURL == null && 
						conn.getClass().getName().equals(
								"org.eclipse.osgi.framework.internal.core.BundleURLConnection")) {
					BUNDLE_URL_CONNECTION_getLocalURL = conn.getClass().getMethod("getLocalURL");
					BUNDLE_URL_CONNECTION_getLocalURL.setAccessible(true);
				}
				if (BUNDLE_URL_CONNECTION_getLocalURL != null) {
					return (URL)BUNDLE_URL_CONNECTION_getLocalURL.invoke(conn);
				}
			} catch (Throwable t) {
				t.printStackTrace();
			}
		}
		return url;
	}
	/**
	 * Only useful for equinox: on felix we get the file:// url already.
	 * Other OSGi implementations have not been tested
	 * <p>
	 * Get a URL to the content of the bundle entry that uses the file: protocol.
	 * The content of the bundle entry may be downloaded or extracted to the local
	 * file system in order to create a file: URL.
	 * @return a URL to the content of the bundle entry that uses the file: protocol
	 * </p>
	 */
	public static URL getFileURL(URL url)
	{
		if ("bundleresource".equals(url.getProtocol()) || "bundleentry".equals(url.getProtocol()))
		{
			try
			{
				URLConnection conn = url.openConnection();
				if (BUNDLE_URL_CONNECTION_getFileURL == null && 
						conn.getClass().getName().equals(
								"org.eclipse.osgi.framework.internal.core.BundleURLConnection"))
				{
					BUNDLE_URL_CONNECTION_getFileURL = conn.getClass().getMethod("getFileURL");
					BUNDLE_URL_CONNECTION_getFileURL.setAccessible(true);
				}
				if (BUNDLE_URL_CONNECTION_getFileURL != null)
				{
					return (URL)BUNDLE_URL_CONNECTION_getFileURL.invoke(conn);
				}
			}
			catch (Throwable t)
			{
				t.printStackTrace();
			}
		}
		return url;
	}
	
}
