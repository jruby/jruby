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
package org.jruby.runtime.profile;

import org.jruby.util.collections.IntHashMap;

public class Invocation {
    
    private final int methodSerialNumber;
    private int recursiveDepth;
    private Invocation parent;
    private final IntHashMap<Invocation> children;
    
    private long duration     = 0;
    private int count         = 0;
    
    public Invocation(int serial) {
        this(null, serial);
    }
    
    public Invocation(Invocation parent, int serial) {
        this.parent             = parent;
        this.methodSerialNumber = serial;
        this.children           = new IntHashMap<Invocation>();
    }

    public Invocation(Invocation parent, int serial, IntHashMap<Invocation> children) {
        this.parent             = parent;
        this.methodSerialNumber = serial;
        this.children           = children;
    }

    public int getMethodSerialNumber() {
        return methodSerialNumber;
    }

    public int getRecursiveDepth() {
        return recursiveDepth;
    }
    
    public void setRecursiveDepth(int d) {
        recursiveDepth = d;
    }

    public Invocation getParent() {
        return parent;
    }
    
    public void setParent(Invocation p) {
        parent = p;
    }

    public IntHashMap<Invocation> getChildren() {
        return children;
    }

    public long getDuration() {
        return duration;
    }

    public void setDuration(long d) {
        duration = d;
    }

    public void addDuration(long d) {
        duration += d;
    }
    
    public int getCount() {
        return count;
    }

    public void setCount(int c) {
        count = c;
    }

    public void incrementCount() {
        count++;
    }

    public Invocation childInvocationFor(int serial) {
        Invocation child;
        if ((child = children.get(serial)) == null) {
            child = new Invocation(this, serial);
            children.put(serial, child);
        }
        return child;
    }
    
    Invocation copyWithNewSerialAndParent(int serial, Invocation newParent) {
        Invocation newInv = new Invocation(newParent, serial, children);
        newInv.setDuration(duration);
        newInv.setCount(count);
        newInv.setRecursiveDepth(recursiveDepth);
        for (Invocation child : children.values()) {
            child.setParent(newInv);
        }
        return newInv;
    }

    public void addChild(Invocation child) {
        children.put(child.getMethodSerialNumber(), child);
    }
    
    public long childTime() {
        long t = 0;
        for (Invocation inv : children.values()) {
            t += inv.getDuration();
        }
        return t;
    }
    
    public long selfTime() {
        return duration - childTime();
    }
    
    @Override
    public String toString() {
        return "Invocation(#" + methodSerialNumber + " count="+ count + " duration="+ duration + 
               " parent="+ ( parent == null ? null : "#" + parent.methodSerialNumber ) + 
               " children.size=" + children.size() + ")";
    }
    
}