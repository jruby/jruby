/*
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 *
 * JRuby - http://jruby.sourceforge.net
 *
 * This file is part of JRuby
 *
 * JRuby is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License or
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License and GNU Lesser General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public
 * License and GNU Lesser General Public License along with JRuby;
 * if not, write to
 * the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 * Boston, MA  02111-1307 USA
 *
 */
package org.jruby.internal.runtime;

import org.jruby.Ruby;
import org.jruby.ThreadClass;
import org.jruby.runtime.ThreadContext;

public class ThreadService {
    private Ruby runtime;
    private ThreadContext mainContext = new ThreadContext(runtime);
    private ThreadContextLocal localContext = new ThreadContextLocal(mainContext);

    public ThreadService(Ruby runtime) {
        this.runtime = runtime;
        this.mainContext = new ThreadContext(runtime);
        this.localContext = new ThreadContextLocal(mainContext);
    }

    public void disposeCurrentThread() {
        localContext.dispose();
    }

    public ThreadContext getCurrentContext() {
        return (ThreadContext) localContext.get();
    }

    public ThreadClass getMainThread() {
        return mainContext.getThread();
    }

    public void setMainThread(ThreadClass thread) {
        mainContext.setThread(thread);
    }

    public void registerNewThread(ThreadClass thread) {
        localContext.set(new ThreadContext(runtime));
        getCurrentContext().setThread(thread);
    }

    private static class ThreadContextLocal extends ThreadLocal {
        private ThreadContext mainContext;

        public ThreadContextLocal(ThreadContext mainContext) {
            this.mainContext = mainContext;
        }

        /**
         * @see java.lang.ThreadLocal#initialValue()
         */
        protected Object initialValue() {
            return this.mainContext;
        }

        public void dispose() {
            this.mainContext = null;
            set(null);
        }
    }

}
