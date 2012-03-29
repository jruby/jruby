/*
 ***** BEGIN LICENSE BLOCK *****
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

package org.jruby.runtime.invokedynamic;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.invoke.MutableCallSite;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.jruby.runtime.CallType;
import org.jruby.runtime.callsite.CacheEntry;

public class JRubyCallSite extends MutableCallSite {
    private final Lookup lookup;
    private final CallType callType;
    public CacheEntry entry = CacheEntry.NULL_CACHE;
    private final Set<Integer> seenTypes = new HashSet<Integer>();
    private final boolean attrAssign;
    private final boolean iterator;
    private final boolean expression;
    private final String name;
    private int clearCount;

    public JRubyCallSite(Lookup lookup, MethodType type, CallType callType, String name, boolean attrAssign, boolean iterator, boolean expression) {
        super(type);
        this.lookup = lookup;
        this.callType = callType;
        this.attrAssign = attrAssign;
        this.iterator = iterator;
        this.expression = expression;
        this.name = name;
    }
    
    public Lookup lookup() {
        return lookup;
    }

    public CallType callType() {
        return callType;
    }

    public boolean isAttrAssign() {
        return attrAssign;
    }
    
    public boolean isIterator() {
        return iterator;
    }
    
    public boolean isExpression() {
        return expression;
    }
    
    public String name() {
        return name;
    }
    
    public synchronized boolean hasSeenType(int typeCode) {
        return seenTypes.contains(typeCode);
    }
    
    public synchronized void addType(int typeCode) {
        seenTypes.add(typeCode);
    }
    
    public synchronized int seenTypesCount() {
        return seenTypes.size();
    }
    
    public synchronized void clearTypes() {
        seenTypes.clear();
        clearCount++;
    }
    
    public int clearCount() {
        return clearCount;
    }
}
