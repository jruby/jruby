/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2005 David Corbin <dcorbin@users.sourceforge.net>
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

package org.jruby.runtime.load;

import org.jruby.exceptions.RaiseException;
import org.jruby.test.Base;
import org.jruby.test.BasicLibraryTestService;

import java.util.Collections;

import static org.jruby.api.Access.loadService;

public class TestLoadService extends Base {

    private LoadService loadService;
    public void setUp() throws Exception {
        super.setUp();
        loadService = loadService(context);
    }
    
    public void testRequireSocket() {
        context.runtime.evalScriptlet("require 'socket'");
    }

    public void testExtensionLoader() {
        BasicLibraryTestService.counter = 0;
        context.runtime.evalScriptlet("require 'org/jruby/test/basic_library_test'");
        assertEquals("The library should've have been loaded", BasicLibraryTestService.counter, 1);
    }
    
    public void testRequireEmpty(){
        try{
            context.runtime.evalScriptlet("require ''");
        } catch (RaiseException e){
            assertTrue("Empty library is not valid, exception should have been raised", RaiseException.class.isAssignableFrom(e.getClass()));
            assertNull("Empty library is not valid, exception should only be RaiseException with no root cause", e.getCause());
        }
    }
    
    public void testNonExistentRequire() {
        try{
            // presumably this require should fail
            context.runtime.evalScriptlet("require 'somethingthatdoesnotexist'");
        } catch (RaiseException e){
            assertTrue("Require of non-existent library should fail", RaiseException.class.isAssignableFrom(e.getClass()));
            assertNull("Require of non-existent library should , exception should only be RaiseException with no root cause", e.getCause());
        }
    }
    
    public void testNonExistentRequireAfterRubyGems() {
        try{
            // JRUBY-646
            // presumably this require should fail
            context.runtime.evalScriptlet("require 'rubygems'; require 'somethingthatdoesnotexist'");
        } catch (RaiseException e){
            assertTrue("Require of non-existent library should fail", RaiseException.class.isAssignableFrom(e.getClass()));
            assertNull("Require of non-existent library should , exception should only be RaiseException with no root cause", e.getCause());
        }
    }
    
    public void testRequireJavaClassFile() {
        try {
            // Test that requiring a normal Java class raises an error (JRUBY-3214
            loadService.load("target/test-classes/org/jruby/test/NormalJavaClass.class", false);
            fail("Exception should have been raised requiring a non-script .class file");
        } catch (Exception e) {
            // ok
        }
    }

    public void testClassExtensionLeftmostIdentifierSearch() {
        String path = "foo/bar/boo-baz/quux/widget";
        String[] elts = path.split("/");

        int leftmost = ClassExtensionLibrary.findLeftmostIdentifier(elts);

        assertEquals(3, leftmost);

        path = "foo/bar/baz/quux/widget";
        elts = path.split("/");
        leftmost = ClassExtensionLibrary.findLeftmostIdentifier(elts);

        assertEquals(0, leftmost);

        path = "foo//baz/quux/widget";
        elts = path.split("/");
        leftmost = ClassExtensionLibrary.findLeftmostIdentifier(elts);

        assertEquals(2, leftmost);
    }

    public void testNullStringWrapperToString() {
        LibrarySearcher.StringWrapper sw = new LibrarySearcher.StringWrapper("foo", 0, 3);

        sw.clear();

        assertEquals("StringWrapper()", sw.toString());
    }

    public void testLoadedFeatureWithPath() {
        // Test that a simple feature lock name does not trigger AIOOB (https://github.com/jruby/jruby/issues/8958)
        String lockedFeature = "previous_feature";
        String feature = "current_feature";
        assertEquals(null, LibrarySearcher.loadedFeatureWithPath(lockedFeature, feature, LibrarySearcher.Suffix.RUBY, Collections.EMPTY_LIST));
    }
}
