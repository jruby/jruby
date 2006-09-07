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
 * Copyright (C) 2006 Ola Bini <ola@ologix.com>
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
package org.jruby.runtime.load;

import java.io.IOException;
import org.jruby.IRuby;

/**
 * The ClassExtensionLibrary wraps a class which implements BasicLibraryService,
 * and when asked to load the service, does a basicLoad of the BasicLibraryService.
 * 
 * When the time comes to add other loading mechanisms for loading a class, this
 * is the place where they will be added. The load method will check interface
 * you can load a class with, and do the right thing.
 */
public class ClassExtensionLibrary implements Library {
    private Class theClass;
    public ClassExtensionLibrary(Class extension) {
        theClass = extension;
    }

    public void load(IRuby runtime) throws IOException {
        if(BasicLibraryService.class.isAssignableFrom(theClass)) {
            try {
                ((BasicLibraryService)theClass.newInstance()).basicLoad(runtime);
            } catch(final Exception ee) {
                throw new RuntimeException(ee.getMessage());
            }
        }
    }
}
