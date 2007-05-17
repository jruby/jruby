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

import java.util.Date;

import org.jruby.util.ByteList;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class TestBean {
    private ByteList name;
    private int age;
    private Date born;

    public TestBean() {
    }

    public TestBean(final ByteList name, final int age, final Date born) {
        this.name = name;
        this.age = age;
        this.born = born;
    }
    
    public ByteList getName() {
        return this.name;
    }

    public int getAge() {
        return age;
    }

    public Date getBorn() {
        return born;
    }

    public void setName(final ByteList name) {
        this.name = name;
    }

    public void setAge(final int age) {
        this.age = age;
    }

    public void setBorn(final Date born) {
        this.born = born;
    }

    public boolean equals(final Object other) {
        boolean ret = this == other;
        if(!ret && other instanceof TestBean) {
            TestBean o = (TestBean)other;
            ret = 
                this.name == null ? o.name == null : this.name.equals(o.name) &&
                this.age == o.age &&
                this.born == null ? o.born == null : this.born.equals(o.born);
        }
        return ret;
    }

    public int hashCode() {
        int val = 3;
        val += 3 * (name == null ? 0 : name.hashCode());
        val += 3 * age;
        val += 3 * (born == null ? 0 : born.hashCode());
        return val;
    }

    public String toString() {
        return "#<org.jvyamlb.TestBean name=\"" + name + "\" age=" + age + " born=\"" + born + "\">";
    }
}// TestBean
