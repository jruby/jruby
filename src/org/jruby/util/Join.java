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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.concurrent.Executor;

public final class Join {
    public static final Executor TRIVIAL_EXECUTOR = new Executor() {
        public void execute(Runnable command) {
            (new Thread(command)).start();
        }
    };

    private final Executor executor;
    private final LinkedList[] writes;
    private final long asyncMask;
    private long mask = 0;
    private final Reaction[][] reactionsPerChannel;

    public static class Spec {
        private ArrayList<ArrayList<Reaction>> reactionsPerChannel = new ArrayList<ArrayList<Reaction>>();
        private long asyncMask = 0;
        private long mask = 0;
        private volatile Reaction[][] cachedReactionsPerChannel = null;

        public Spec() {}

        public void addReaction(Reaction reaction) {
            if ( ( mask & ~asyncMask & reaction.asyncMask ) != 0 ) {
                throw new IllegalArgumentException("Cannot use a synchronous channel in a non-head position");
            }
            if ( ( reaction.mask & ~reaction.asyncMask & asyncMask ) != 0 ) {
                throw new IllegalArgumentException("Cannot use an asynchronous channel in the head position of a synchronous reaction");
            }
            cachedReactionsPerChannel = null;
            final int[] indices = reaction.indices;
            for ( int i = 0 ; i < indices.length ; i++ ) {
                final int index = indices[i];
                if ( reactionsPerChannel.size() <= index ) {
                    reactionsPerChannel.ensureCapacity(index+1);
                    while ( reactionsPerChannel.size() <= index ) {
                        reactionsPerChannel.add(null);
                    }
                }
                ArrayList<Reaction> reactions = reactionsPerChannel.get(index);
                if ( reactions == null ) {
                    reactions = new ArrayList<Reaction>();
                    reactionsPerChannel.set(index, reactions);
                }
                reactions.add(reaction);
            }
            asyncMask |= reaction.asyncMask;
            mask |= reaction.mask;
        }

        public Join createJoin() {
            return createJoin(TRIVIAL_EXECUTOR);
        }

        private static final Reaction[] EMPTY_REACTIONS = new Reaction[0];
        public Join createJoin(final Executor executor) {
            if (cachedReactionsPerChannel == null) {
                final int length = reactionsPerChannel.size();
                final Reaction[][] localReactionsPerChannel = new Reaction[length][];
                for ( int i = 0 ; i < length ; ++i ) {
                    final ArrayList<Reaction> reactions = reactionsPerChannel.get(i);
                    if ( reactions != null ) {
                        localReactionsPerChannel[i] = reactions.toArray(EMPTY_REACTIONS);
                    }
                }
                cachedReactionsPerChannel = localReactionsPerChannel;
            }
            return new Join(asyncMask, cachedReactionsPerChannel, executor);
        }
    }

    public static abstract class Reaction {
        private final int[] indices;
        private final long mask;
        private final long asyncMask;

        private static int[] toIndices(Enum<?> head, Enum<?>[] channels) {
            final int[] indices = new int[channels.length+1];
            indices[0] = head.ordinal();
            for ( int i = 0 ; i < channels.length ; ++i ) {
                indices[i+1] = channels[i].ordinal();
            }
            return indices;
        }

        Reaction(Enum<?> head, Enum<?>[] channels, boolean isAsync) {
            this(toIndices(head, channels), isAsync);
        }

        Reaction(int[] indices, boolean isAsync) {
            long mask = 0;
            for ( int i = 0 ; i < indices.length ; ++i ) {
                final int index = indices[i];
                if ( index < 0 || index > 63 ) {
                    throw new IndexOutOfBoundsException();
                }
                if ( ( mask & ( 1L << index ) ) != 0 ) {
                    throw new IllegalArgumentException("Duplicate channels in reaction");
                }
                mask |= 1L << index;
            }
            this.indices = indices;
            this.mask = mask;
            if (isAsync) {
                this.asyncMask = mask;
            } else {
                this.asyncMask = mask & ~( 1L << indices[0] );
            }
        }

        abstract void dispatch(Join join, Object[] args);
    }

    public static abstract class FastReaction extends Reaction {
        public FastReaction(int[] indices) {
            super(indices.clone(), true);
        }

        public FastReaction(Enum<?> head, Enum<?> ... channels) {
            super(head, channels, true);
        }

        @Override
        void dispatch(final Join join, final Object[] args) {
            try {
                react(join, args);
            } catch (Exception e) {
            }
        }

        public abstract void react(Join join, Object[] args);
    }

    public static abstract class AsyncReaction extends Reaction {
        public AsyncReaction(int[] indices) {
            super(indices.clone(), true);
        }

        public AsyncReaction(Enum<?> head, Enum<?> ... channels) {
            super(head, channels, true);
        }

        @Override
        void dispatch(final Join join, final Object[] args) {
            final AsyncReaction reaction = this;
            join.executor.execute(new Runnable() {
                public void run() {
                    reaction.react(join, args);
                }
            });
        }

        public abstract void react(Join join, Object[] args);
    }

    public static abstract class SyncReaction extends Reaction {
        public SyncReaction(int[] indices) {
            super(indices.clone(), false);
        }

        public SyncReaction(Enum<?> head, Enum<?> ... channels) {
            super(head, channels, false);
        }

        @Override
        void dispatch(Join join, final Object[] args) {
            final Call call = (Call)args[0];
            args[0] = call.getMessage();
            call.activate(join, this, args); 
        }

        public abstract Object react(Join join, Object[] args);
    }

    private Join(final long asyncMask, final Reaction[][] reactionsPerChannel, Executor executor) {
        final LinkedList[] writes = new LinkedList[reactionsPerChannel.length];
        for ( int i = 0 ; i < writes.length ; ++i ) {
            if ( reactionsPerChannel[i] != null ) {
                writes[i] = new LinkedList();
            }
        }
        this.asyncMask = asyncMask;
        this.reactionsPerChannel = reactionsPerChannel;
        this.writes = writes;
        this.executor = executor;
    }

    private void sendRaw(int index, Object message) {
        Reaction selectedReaction = null;
        Object[] args = null;
        synchronized (this) {
            final LinkedList writing = writes[index];
            if ( writing == null ) {
                throw new IndexOutOfBoundsException();
            }
            writing.addLast(message);
            mask |= 1L << index;
            final Reaction[] reactions = reactionsPerChannel[index];
            for (Reaction reaction: reactions) {
                if ( ( reaction.mask & mask ) == reaction.mask ) {
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
                    selectedReaction = reaction;
                    break;
                }
            }
        }
        if ( selectedReaction != null ) {
            selectedReaction.dispatch(this, args);
        }
    }

    public boolean isAsync(int channel) {
        return ( ( 1L << channel ) & asyncMask ) != 0;
    }

    public void send(int channel, Object message) {
        if (isAsync(channel)) {
            sendRaw(channel, message);
        } else {
            sendRaw(channel, new AsyncCall(message));
        }
    }

    public void send(Enum<?> channel, Object message) {
        send(channel.ordinal(), message);
    }

    public Object call(int channel, Object message) {
        if (isAsync(channel)) {
            sendRaw(channel, message);
            return null;
        } else {
            SyncCall request = new SyncCall(message);
            sendRaw(channel, request);
            return request.call();
        }
    }

    public Object call(Enum<?> channel, Object message) {
        return call(channel.ordinal(), message);
    }

    private static abstract class Call {
        private final Object message;

        public Call(Object message) {
            this.message = message;
        }

        public Object getMessage() {
            return message;
        }

        public abstract void activate(Join join, SyncReaction reaction, Object[] args);
    }

    private static class AsyncCall extends Call {
        public AsyncCall(Object message) {
            super(message);
        }
        
        public void activate(final Join join, final SyncReaction reaction, final Object[] args) {
            join.executor.execute(new Runnable() {
                public void run() {
                    reaction.react(join, args);
                }
            }); 
        }
    }

    private static class SyncCall extends Call {
        private Join join = null;
        private SyncReaction reaction = null;
        private Object[] args = null;

        public SyncCall(Object message) {
            super(message);
        }
        
        public synchronized void activate(Join join, SyncReaction reaction, Object[] args) {
            this.join = join;
            this.reaction = reaction;
            this.args = args;
            notifyAll();
        }

        public synchronized Object call() {
            boolean interrupted = false;
            try {
                while ( reaction == null ) {
                    try {
                        wait();
                    } catch (InterruptedException e) {
                        interrupted = true;
                    }
                }
            } finally {
                if (interrupted) {
                    Thread.currentThread().interrupt();
                }
            }
            return reaction.react(join, args);
        }
    }
}

