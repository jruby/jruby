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

import java.util.concurrent.ExecutionException;

public interface ThreadLike {
    public void start();
    
    public void interrupt();
    
    public boolean isAlive();
    
    public void join() throws InterruptedException, ExecutionException;
    
    public void join(long millis) throws InterruptedException, ExecutionException;
    
    public int getPriority();
    
    public void setPriority(int priority);
    
    public boolean isCurrent();
    
    public boolean isInterrupted();

    public Thread nativeThread();

    public void setRubyName(String name);

    public String getRubyName();

    public String getReportName();

    ThreadLike DUMMY = new ThreadLike() {
        @Override
        public void start() {}

        @Override
        public void interrupt() {}

        @Override
        public boolean isAlive() {
            return false;
        }

        @Override
        public void join() throws InterruptedException, ExecutionException {}

        @Override
        public void join(long millis) throws InterruptedException, ExecutionException {}

        @Override
        public int getPriority() {
            return 0;
        }

        @Override
        public void setPriority(int priority) {}

        @Override
        public boolean isCurrent() {
            return false;
        }

        @Override
        public boolean isInterrupted() {
            return false;
        }

        @Override
        public Thread nativeThread() {
            return null;
        }

        @Override
        public void setRubyName(String name) {}

        @Override
        public String getRubyName() {
            return "uninitialized thread";
        }

        @Override
        public String getReportName() {
            return "uninitialized thread";
        }
    };
}
