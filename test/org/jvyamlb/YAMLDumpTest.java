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
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jvyamlb;

import java.util.Map;
import java.util.List;
import java.util.HashMap;
import java.util.ArrayList;

import junit.framework.TestCase;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class YAMLDumpTest extends TestCase {
    public YAMLDumpTest(final String name) {
        super(name);
    }

    public void testBasicStringDump() {
        assertEquals(ByteList.create("--- str\n"), YAML.dump(ByteList.create("str")));
    }

    public void testBasicHashDump() {
        Map ex = new HashMap();
        ex.put(ByteList.create("a"),ByteList.create("b"));
        assertEquals(ByteList.create("--- \na: b\n"), YAML.dump(ex));
    }

    public void testBasicListDump() {
        List ex = new ArrayList();
        ex.add(ByteList.create("a"));
        ex.add(ByteList.create("b"));
        ex.add(ByteList.create("c"));
        assertEquals(ByteList.create("--- \n- a\n- b\n- c\n"), YAML.dump(ex));
    }

    public void testVersionDumps() {
        assertEquals(ByteList.create("--- !!int 1\n"), YAML.dump(new Integer(1),YAML.config().explicitTypes(true)));
        assertEquals(ByteList.create("--- !int 1\n"), YAML.dump(new Integer(1),YAML.config().version("1.0").explicitTypes(true)));
    }

    public void testMoreScalars() {
        assertEquals(ByteList.create("--- !!str 1.0\n"), YAML.dump(ByteList.create("1.0")));
    }

    public void testDumpJavaBean() {
        final TestBean2 toDump = new TestBean2(ByteList.create("Ola Bini"), 24);
        Object v = YAML.dump(toDump);
        assertTrue("something is wrong with: \"" + v + "\"",
ByteList.create("--- !java/object:org.jvyamlb.TestBean2\nname: Ola Bini\nage: 24\n").equals(v) ||
ByteList.create("--- !java/object:org.jvyamlb.TestBean2\nage: 24\nname: Ola Bini\n").equals(v)
                   );
    }
}// YAMLDumpTest
