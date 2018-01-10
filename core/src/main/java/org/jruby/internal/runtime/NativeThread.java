/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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
package org.jruby.internal.runtime;

import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import org.jruby.RubyThread;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author cnutter
 */
public class NativeThread implements ThreadLike {
    private final Reference<Thread> nativeThread;
    public final RubyThread rubyThread;
    public String rubyName;
    
    public NativeThread(RubyThread rubyThread, Thread nativeThread) {
        this.rubyThread = rubyThread;
        this.nativeThread = new WeakReference<>(nativeThread);
        this.rubyName = null;
    }
    
    public void start() {
        Thread thread = getThread();
        if (thread != null) thread.start();
    }
    
    public void interrupt() {
        Thread thread = getThread();
        if (thread != null) thread.interrupt();
    }
    
    public boolean isAlive() {
        Thread thread = getThread();
        if (thread != null) return thread.isAlive();
        return false;
    }
    
    public void join() throws InterruptedException {
        Thread thread = getThread();
        if (thread != null) thread.join();
    }
    
    public void join(long timeoutMillis) throws InterruptedException {
        Thread thread = getThread();
        if (thread != null) thread.join(timeoutMillis);
    }
    
    public int getPriority() {
        Thread thread = getThread();
        if (thread != null) return thread.getPriority();
        return 0;
    }
    
    public void setPriority(int priority) {
        Thread thread = getThread();
        if (thread != null) thread.setPriority(priority);
    }
    
    public boolean isCurrent() {
        return getThread() == Thread.currentThread();
    }
    
    public boolean isInterrupted() {
        Thread thread = getThread();
        if (thread != null) {
            return thread.isInterrupted();
        }
        return false;
    }

    public final Thread getThread() {
        return nativeThread.get();
    }

    public String toString() {
        return String.valueOf(getThread());
    }

    public Thread nativeThread() {
        return nativeThread.get();
    }

    @Override
    public void setRubyName(String name) {
        this.rubyName = name;
        updateName();
    }

    @Override
    public String getRubyName() {
        return rubyName;
    }

    @Override
    public String getReportName() {
        String nativeName = "";

        Thread thread = getThread();
        if (thread != null) nativeName = thread.getName();

        if (rubyName == null || rubyName.length() == 0) {
            if (nativeName.equals("")) {
                return "(unnamed)";
            } else {
                return nativeName;
            }
        } else {
            if (nativeName.equals("")) {
                return rubyName.toString();
            }
            return rubyName + " (" + nativeName + ")";
        }
    }

    private static final String RUBY_THREAD_PREFIX = "Ruby-";

    void updateName() {
        // "Ruby-0-Thread-16: (irb):21"
        // "Ruby-0-Thread-17@worker#1: (irb):21"
        String newName;
        String setName = rubyName;
        Thread thread = getThread();

        if (thread == null) return;

        final String currentName = thread.getName();
        if (currentName != null && currentName.startsWith(RUBY_THREAD_PREFIX)) {
            final int i = currentName.indexOf('@'); // Thread#name separator
            if (i == -1) { // name not set yet: "Ruby-0-Thread-42: FILE:LINE"
                int end = currentName.indexOf(':');
                if (end == -1) end = currentName.length();
                final String prefix = currentName.substring(0, end);
                newName = currentName.replace(prefix, prefix + '@' + setName);

            } else { // name previously set: "Ruby-0-Thread-42@foo: FILE:LINE"
                final String prefix = currentName.substring(0, i); // Ruby-0-Thread-42
                int end = currentName.indexOf(':', i);
                if (end == -1) end = currentName.length();
                final String prefixWithName = currentName.substring(0, end); // Ruby-0-Thread-42@foo:
                newName = currentName.replace(prefixWithName, setName == null ? prefix : (prefix + '@' + setName));
            }
        } else return; // not a new-thread that and does not match out Ruby- prefix
        // ... very likely user-code set the java thread name - thus do not mess!
        try { thread.setName(newName); } catch (SecurityException ignore) { } // current thread can not modify
    }
}
