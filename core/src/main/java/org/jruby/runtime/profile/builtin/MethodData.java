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

package org.jruby.runtime.profile.builtin;

import org.jruby.util.collections.IntList;

import java.util.ArrayList;

class MethodData extends InvocationSet {
    
    final int serialNumber;

    MethodData(int serial) {
        super(new ArrayList<Invocation>(4));
        this.serialNumber = serial;
    }

    public int[] parents() {
        IntList p = new IntList();
        for (Invocation inv : invocations) {
            if (inv.getParent() != null) {
                int serial = inv.getParent().getMethodSerialNumber();
                if (!p.contains(serial)) {
                    p.add(serial);
                }
            }
        }
        return p.toIntArray();
    }

    public int[] children() {
        IntList p = new IntList();
        for (Invocation inv : invocations) {
            for (Integer childSerial : inv.getChildren().keySet()) {
                if (!p.contains(childSerial)) {
                    p.add(childSerial);
                }
            }
        }
        return p.toIntArray();
    }

    public InvocationSet invocationsForParent(int parentSerial) {
        ArrayList<Invocation> p = new ArrayList<Invocation>(4);
        for (Invocation inv : invocations) {
            int serial = inv.getParent().getMethodSerialNumber();
            if (serial == parentSerial) {
                p.add(inv.getParent());
            }
        }
        return new InvocationSet(p);
    }

    public InvocationSet rootInvocationsFromParent(int parentSerial) {
        ArrayList<Invocation> p = new ArrayList<Invocation>(4);
        for (Invocation inv : invocations) {
            int serial = inv.getParent().getMethodSerialNumber();
            if (serial == parentSerial && inv.getRecursiveDepth() == 1) {
                p.add(inv);
            }
        }
        return new InvocationSet(p);
    }

    public InvocationSet invocationsFromParent(int parentSerial) {
        ArrayList<Invocation> p = new ArrayList<Invocation>(4);
        for (Invocation inv : invocations) {
            int serial = inv.getParent().getMethodSerialNumber();
            if (serial == parentSerial) {
                p.add(inv);
            }
        }
        return new InvocationSet(p);
    }

    public InvocationSet rootInvocationsOfChild(int childSerial) {
        ArrayList<Invocation> p = new ArrayList<Invocation>(4);
        for (Invocation inv : invocations) {
            Invocation childInv = inv.getChildren().get(childSerial);
            if (childInv != null && childInv.getRecursiveDepth() == 1) {
                p.add(childInv);
            }
        }
        return new InvocationSet(p);
    }

    public InvocationSet invocationsOfChild(int childSerial) {
        ArrayList<Invocation> p = new ArrayList<Invocation>(4);
        for (Invocation inv : invocations) {
            Invocation childInv = inv.getChildren().get(childSerial);
            if (childInv != null) {
                p.add(childInv);
            }
        }
        return new InvocationSet(p);
    }

    @Override
    public long totalTime() {
        long t = 0;
        for (Invocation inv : invocations) {
            if (inv.getRecursiveDepth() == 1) {
                t += inv.getDuration();
            }
        }
        return t;
    }

    @Override
    public long childTime() {
        long t = 0;
        for (Invocation inv : invocations) {
            if (inv.getRecursiveDepth() == 1) {
                t += inv.childTime();
            }
        }
        return t;
    }
    
}
