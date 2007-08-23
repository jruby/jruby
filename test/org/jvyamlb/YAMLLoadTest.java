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
import java.util.HashMap;

import junit.framework.TestCase;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class YAMLLoadTest extends TestCase {
    public YAMLLoadTest(final String name) {
        super(name);
    }

    private static ByteList s(String st) throws Exception {
        return new ByteList(st.getBytes("UTF-8"));
    }

    public void testBasicStringScalarLoad() throws Exception {
        ByteList str = s("str");
        assertEquals(str,YAML.load(s("--- str")));
        assertEquals(str,YAML.load(s("---\nstr")));
        assertEquals(str,YAML.load(s("--- \nstr")));
        assertEquals(str,YAML.load(s("--- \n str")));
        assertEquals(str,YAML.load(s("str")));
        assertEquals(str,YAML.load(s(" str")));
        assertEquals(str,YAML.load(s("\nstr")));
        assertEquals(str,YAML.load(s("\n str")));
        assertEquals(str,YAML.load(s("\"str\"")));
        assertEquals(str,YAML.load(s("'str'")));
        assertEquals(s("\u00fc"),YAML.load(s("---\n\"\\xC3\\xBC\"")));
    }
    
    public void testBasicIntegerScalarLoad() throws Exception {
        assertEquals(new Long(47),YAML.load(s("47")));
        assertEquals(new Long(0),YAML.load(s("0")));
        assertEquals(new Long(-1),YAML.load(s("-1")));
    }

    public void testBlockMappingLoad() throws Exception {
        Map expected = new HashMap();
        expected.put(s("a"),s("b"));
        expected.put(s("c"),s("d"));
        assertEquals(expected,YAML.load(s("a: b\nc: d")));
        assertEquals(expected,YAML.load(s("c: d\na: b\n")));
    }

    public void testFlowMappingLoad() throws Exception {
        Map expected = new HashMap();
        expected.put(s("a"),s("b"));
        expected.put(s("c"),s("d"));
        assertEquals(expected,YAML.load(s("{a: b, c: d}")));
        assertEquals(expected,YAML.load(s("{c: d,\na: b}")));
    }

    public void testInternalChar() throws Exception {
        Map expected = new HashMap();
        expected.put(s("bad_sample"),s("something:("));
        assertEquals(expected,YAML.load(s("--- \nbad_sample: something:(\n")));
    }

    public void testBuiltinTag() throws Exception {
        assertEquals(s("str"),YAML.load(s("!!str str")));
        assertEquals(s("str"),YAML.load(s("%YAML 1.1\n---\n!!str str")));
        assertEquals(s("str"),YAML.load(s("%YAML 1.0\n---\n!str str")));
        assertEquals(s("str"),YAML.load(s("---\n!str str"),YAML.config().version("1.0")));
        assertEquals(new Long(123),YAML.load(s("---\n!int 123"),YAML.config().version("1.0")));
        assertEquals(new Long(123),YAML.load(s("%YAML 1.1\n---\n!!int 123"),YAML.config().version("1.0")));
    }

    public void testDirectives() throws Exception {
        assertEquals(s("str"),YAML.load(s("%YAML 1.1\n--- !!str str")));
        assertEquals(s("str"),YAML.load(s("%YAML 1.1\n%TAG !yaml! tag:yaml.org,2002:\n--- !yaml!str str")));
        try {
            YAML.load(s("%YAML 1.1\n%YAML 1.1\n--- !!str str"));
            fail("should throw exception when repeating directive");
        } catch(final ParserException e) {
            assertTrue(true);
        }
    }

    public void testJavaBeanLoad() throws Exception {
        final java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.clear();
        cal.set(1982,5-1,3); // Java's months are zero-based...
        
        final TestBean expected = new TestBean(s("Ola Bini"), 24, cal.getTime());
        assertEquals(expected, YAML.load(s("--- !java/object:org.jvyamlb.TestBean\nname: Ola Bini\nage: 24\nborn: 1982-05-03\n")));
    }
}// YAMLLoadTest
