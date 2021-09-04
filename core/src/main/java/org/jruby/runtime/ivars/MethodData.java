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

package org.jruby.runtime.ivars;

import java.util.Collection;
import java.util.Collections;

/**
 * Abstraction of all metadata about a method.
 */
public class MethodData {
    public static final MethodData NULL = new MethodData("", "", Collections.EMPTY_LIST);
    
    public MethodData(String name, String filename, Collection<String> ivarNames) {
        this.name = name;
        this.filename = filename;
        this.ivarNames = ivarNames;
    }

    public String getName() {
        return name;
    }

    public String getFilename() {
        return filename;
    }

    public Collection<String> getIvarNames() {
        return ivarNames;
    }
    
    private final String name;
    private final String filename;
    private final Collection<String> ivarNames;
}
