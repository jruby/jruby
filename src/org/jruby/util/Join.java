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
 * Copyright (C) 2008 MenTaLguY <mental@rydia.net>
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
package org.jruby.util;

import java.util.LinkedList;

public final class Join {
    private final LinkedList[] writes;
    private long mask = 0;
    private final Reaction[] reactions;

    public static abstract class Reaction {
        private final int[] indices;
        private final long mask;

        public Reaction(int ... indices) {
            long mask = 0;
            for ( int i = 0 ; i < indices.length ; ++i ) {
                final int index = indices[i];
                if ( index > 63 ) {
                    throw new IndexOutOfBoundsException();
                }
                mask |= 1L << index;
            }
            this.indices = indices.clone();
            this.mask = mask;
        }

        public abstract void react(Join join, Object[] args);
    }

    public Join(final Reaction ... reactions) {
        final LinkedList[] writes = new LinkedList[64];
        long mask = 0;
        for (Reaction reaction: reactions) {
            mask |= reaction.mask;
        }
        int i;
        for ( i = 0 ; mask != 0 && i < 64 ; ++i, mask >>= 1 ) {
            if ( ( mask & 1 ) != 0 ) {
                writes[i] = new LinkedList();
            } else {
                writes[i] = null;
            }
        }
        this.reactions = reactions.clone();
        this.writes = new LinkedList[i];
        System.arraycopy(writes, 0, this.writes, 0, i);
    }

    public void send(int index, Object o) {
        Reaction selectedReaction = null;
        Object[] args = null;
        synchronized (this) {
            final LinkedList writing = writes[index];
            if ( writing == null ) {
                throw new IndexOutOfBoundsException();
            }
            writing.addLast(o);
            mask |= 1L << index;
            for (Reaction reaction: reactions) {
                if ( ( reaction.mask & mask ) == reaction.mask ) {
                    selectedReaction = reaction;
                    final int[] indices = reaction.indices;
                    args = new Object[indices.length];
                    for ( int i = 0 ; i < indices.length ; ++i ) {
                        final int readIndex = indices[i];
                        final LinkedList reading = writes[readIndex];
                        args[i] = reading.removeFirst();
                        if (reading.isEmpty()) {
                            mask &= ~(1L << readIndex);
                        }
                    }
                    break;
                }
            }
        }
        if ( selectedReaction != null ) {
            selectedReaction.react(this, args);
        }
    }

    public static class SyncRequest {
        private Object reply = null;

        public SyncRequest() {}

        public synchronized void sendReply(Object o) {
            if ( o == null ) {
                throw new NullPointerException();
            } else if ( reply != null ) {
                throw new IllegalStateException();
            }
            reply = o;
            notifyAll();
        }
        public synchronized Object waitForReply() throws InterruptedException {
            while ( reply == null ) {
                wait();
            }
            return reply;
        }
    }
}

