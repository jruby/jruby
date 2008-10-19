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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.platform;

import com.kenai.constantine.Constant;
import com.kenai.constantine.ConstantSet;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.jruby.IErrno;

/**
 * Holds the platform specific errno values.
 */
public final class Errno {
    private static final Collection<Constant> constants = getConstants();
    
    private static final Collection<Constant> getConstants() {
        ConstantSet c = ConstantSet.getConstantSet("Errno");
        if (c != null) {
            return c;
        }
        return getConstantsFromFields(IErrno.class);
    }
    public static Collection<Constant> values() {
        return constants;
    }

    private static final class FakeConstant implements Constant {
        private final String name;
        private final int value;
        FakeConstant(String name, int value) {
            this.name = name;
            this.value = value;
        }
        public int value() {
            return value;
        }

        public String name() {
            return name;
        }
        
    }
    
    /**
     * Loads the errno values from a static field called 'CONSTANTS' in the class.
     * @param errnoClass The class to load errno constants from.
     * @return A map of errno name to errno value.
     */
    private static Collection<Constant> getConstantsFromFields(Class errnoClass) {
        Field[] fields = errnoClass.getFields();
        List<Constant> c = new ArrayList<Constant>(fields.length);
        for (int i = 0; i < fields.length; ++i) {
            try {
                c.add(new FakeConstant(fields[i].getName(), fields[i].getInt(errnoClass)));
            } catch (IllegalAccessException ex) {
                throw new RuntimeException("Non public constant in " + errnoClass.getName(), ex);
            }
        }
        return Collections.unmodifiableCollection(c);
    }
}
