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
 * Copyright (C) 2007 Ola Bini <ola.bini@gmail.com>
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
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.WeakHashMap;

import org.jruby.runtime.Frame;
import org.jruby.runtime.ThreadContext;

/**
 * @author <a href="mailto:ola.bini@gmail.com">Ola Bini</a>
 */
public class SimpleSampler {
    private final static Map<ThreadContext, Object> CURRENT = new WeakHashMap<ThreadContext, Object>();
    private final static Map<String, Integer> SAMPLES = new HashMap<String, Integer>();
    private final static List<List<String>> TRACES = new ArrayList<List<String>>();
    
    private static boolean reported = false;

    public static void registerThreadContext(ThreadContext tc) {
        synchronized(CURRENT) {
            CURRENT.put(tc, null);
        }
    }

    public static void startSampleThread() {
        new Thread(new Runnable() {
                public void run() {
                    SimpleSampler.runSampling();
                }
            }).start();
    }

    public static void report() {
        if(!reported) {
            System.err.println();
            System.err.println("Samples - ");
            List<String> samples = new ArrayList<String>();
            samples.addAll(SAMPLES.keySet());
            Collections.sort(samples, new Comparator<String>(){
                    public int compare(String o1, String o2) {
                        return SAMPLES.get(o2) - SAMPLES.get(o1);
                    }
                });
            for(List<String> ls : TRACES) {
                if(ls.size() > 1) {
                    System.err.println("Trace #" + System.identityHashCode(ls));
                    for(String ss : ls) {
                        System.err.println("  " + ss);
                    }
                    System.err.println();
                }
            }
            String BLANKS = "                                                            ";
            for(String ss : samples) {
                int len = Math.max(60 - ss.length(), 0);
                System.err.println(" " + ss + BLANKS.substring(0,len) +  "==> " + SAMPLES.get(ss));
            }
            reported = true;
        }
    }

    private static void runSampling() {
        long interval = Long.parseLong(System.getProperty("jruby.sampling.interval", "10"));
        int depth = Integer.parseInt(System.getProperty("jruby.sampling.depth", "5"));
        System.err.println("[Sampling with");
        System.err.println(" - interval: " + interval);
        System.err.println(" - depth: " + depth + "]");
        synchronized(CURRENT) {
            while(!reported) {
                try {
                    CURRENT.wait(interval);
                } catch(InterruptedException e) {}

                try {
                    for(ThreadContext tc : CURRENT.keySet()) {
                        if(tc != null) {
                            Frame[] frames = tc.createBacktrace(1, false);
                            if(frames != null) {
                                List<String> trace = new ArrayList<String>(depth);
                                for(int i = (Math.max(frames.length - depth,0)); i<frames.length; i++) {
                                    Frame f = frames[i];
                                    String name = f.getKlazz() + "#" + f.getName();
                                    if(!f.isBindingFrame() && !name.equals("null#null")) {
                                        trace.add(name);
                                        Integer v = SAMPLES.get(name);
                                        if(v == null) {
                                            v = 1;
                                        } else {
                                            v++;
                                        }
                                        SAMPLES.put(name, v);
                                    }
                                }
                                TRACES.add(trace);
                            }
                        }
                    }
                } catch(Exception e) {}
            }
        } 
    }
}// SimpleSampler
