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

import org.jruby.runtime.ThreadContext;
import org.jruby.Ruby;
import org.jruby.ThreadClass;

public class ThreadService {
    private Ruby runtime;
    private ThreadContext mainContext = new ThreadContext(runtime);
    private ThreadContextLocal threadContext = new ThreadContextLocal(mainContext);

    public ThreadService(Ruby runtime) {
        this.runtime = runtime;
        this.mainContext = new ThreadContext(runtime);
        this.threadContext = new ThreadContextLocal(mainContext);
    }

    public void dispose() {
        threadContext.dereferenceMainContext();
    }

    public ThreadContext getCurrentContext() {
        return (ThreadContext) threadContext.get();
    }

    public ThreadContext getMainContext() {
        return mainContext;
    }

    public void registerNewContext(ThreadClass thread) {
        threadContext.set(new ThreadContext(runtime));
        getCurrentContext().setCurrentThread(thread);
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

        public void dereferenceMainContext() {
            this.mainContext = null;
            set(null);
        }
    }

}
