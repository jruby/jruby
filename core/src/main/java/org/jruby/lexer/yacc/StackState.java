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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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
package org.jruby.lexer.yacc;

/**
 * 
 * @author jpetersen
 */
public class StackState implements Cloneable {
    private long stack = 0;

    public void reset() {
        reset(0);
    }

    public void reset(long backup) {
        stack = backup;
    }

    // PUSH(1)
    public long begin() {
        long old = stack;
        stack <<= 1;
        stack |= 1;
        return old;
    }

    // POP
    public void end() {
        stack >>= 1;
    }

    // PUSH(0).  If you look at original macro: stack |= (n&1) => stack |= 0 => no-change.
    public void stop() {
        stack <<= 1;
    }

    // LEXPOP
    public void restart() {
        stack |= (stack & 1) << 1;
        stack >>= 1;
    }
    
    // SET_P
    public boolean isInState() {
        return (stack & 1) != 0;
    }
}
