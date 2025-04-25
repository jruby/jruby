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

package org.jruby.runtime.opto;

import java.lang.invoke.SwitchPoint;
import java.util.List;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;
import java.util.concurrent.atomic.AtomicReferenceFieldUpdater;
import java.util.concurrent.atomic.AtomicStampedReference;

public class FailoverSwitchPointInvalidator implements Invalidator {
    // a dummy switchpoint to use until we actually need a real one
    private static final SwitchPoint DUMMY = new SwitchPoint();
    static {SwitchPoint.invalidateAll(new SwitchPoint[]{DUMMY});}

    private final AtomicStampedReference<SwitchPoint> switchPoint = new AtomicStampedReference<>(DUMMY, 0);

    private final int maxFailures;
    
    public FailoverSwitchPointInvalidator(int maxFailures) {
        this.maxFailures = maxFailures;
    }
    
    public void invalidate() {
        SwitchPoint switchPoint = this.switchPoint.getReference();
        if (switchPoint == DUMMY) return;

        int failures = this.switchPoint.getStamp();
        SwitchPoint newSwitch = DUMMY;

        if (failures < maxFailures) {
            newSwitch = new SwitchPoint();
        }

        // Not important if we are successful, since update after read means another thread got to it first
        this.switchPoint.compareAndSet(switchPoint, newSwitch, failures, failures + 1);

        // Safe to invalidate now that we've updated
        SwitchPoint.invalidateAll(new SwitchPoint[]{switchPoint});
    }

    public void invalidateAll(List<Invalidator> invalidators) {
        if (invalidators.isEmpty()) return;

        SwitchPoint[] switchPoints = new SwitchPoint[invalidators.size()];
        
        for (int i = 0; i < invalidators.size(); i++) {
            Invalidator invalidator = invalidators.get(i);
            assert invalidator instanceof FailoverSwitchPointInvalidator;
            switchPoints[i] = ((FailoverSwitchPointInvalidator)invalidator).replaceSwitchPoint();
        }
        
        SwitchPoint.invalidateAll(switchPoints);
    }
    
    public synchronized Object getData() {
        while (true) {
            SwitchPoint switchPoint = this.switchPoint.getReference();
            int failures = this.switchPoint.getStamp();
            if (switchPoint == DUMMY && failures <= maxFailures) {
                SwitchPoint newSwitch = new SwitchPoint();
                if (this.switchPoint.compareAndSet(DUMMY, newSwitch, failures, failures)) {
                    return newSwitch;
                }
            } else {
                return switchPoint;
            }
        }
    }
    
    public synchronized SwitchPoint replaceSwitchPoint() {
        while (true) {
            SwitchPoint switchPoint = this.switchPoint.getReference();
            int failures = this.switchPoint.getStamp();
            if (switchPoint == DUMMY || failures > maxFailures) return DUMMY;
            SwitchPoint newSwitch = new SwitchPoint();
            if (this.switchPoint.compareAndSet(switchPoint, newSwitch, failures, failures + 1)) {
                return newSwitch;
            }
        }
    }
}
