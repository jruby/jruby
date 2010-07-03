/*
 * Copyright (C) 2010 Wayne Meissner
 *
 * This file is part of jruby-cext.
 *
 * This code is free software: you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 3 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 3 for more details.
 *
 * You should have received a copy of the GNU General Public License
 * version 3 along with this work.  If not, see <http://www.gnu.org/licenses/>.
 */


package org.jruby.cext;

import java.lang.ref.Reference;
import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import org.jruby.RubyString;

/**
 *
 */
public final class RString extends WeakReference<RubyString> {
    private static ReferenceQueue<RubyString> queue = new ReferenceQueue<RubyString>();
    private static RString list = null;

    private final long address;
    private RString prev, next;

    private RString(RubyString str, long address) {
        super(str);
        this.address = address;
    }

    static RString newRString(RubyString str, long address) {
        RString rstring = new RString(str, address);

        if (list != null) {
            rstring.next = list;
            list.prev = rstring;
        }
        list = rstring;

        return rstring;
    }

    final long address() {
        return address;
    }

    private static final Runnable reaper = new Runnable() {

        public void run() {
            while (true) {
                Reference<? extends RubyString> ref;
                try {
                     ref = queue.remove();
                } catch (InterruptedException ex) {
                    break;
                }
                GIL.acquire();
                try {
                    do {
                        if (ref instanceof RString) {
                            RString r = (RString) ref;

                            if (r.next != null) {
                                r.next.prev = r.prev;
                            }
                            if (r.prev != null) {
                                r.prev.next = r.next;
                            }
                            if (r == list) {
                                if (list.next != null) {
                                    list = list.next;
                                } else {
                                    list = list.prev;
                                }
                            }
                            r.prev = r.next = null;
                            Native.freeRString(r.address);
                        }
                    } while((ref = queue.poll()) != null);
                } finally {
                    GIL.releaseNoCleanup();
                }
            }
        }
    };

    static {
        Thread t = new Thread(reaper);
        t.setDaemon(true);
        t.start();
    }

}
