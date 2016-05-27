/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.tools;

import com.oracle.truffle.api.CompilerDirectives.TruffleBoundary;
import com.oracle.truffle.api.object.DynamicObject;
import org.jruby.truffle.Layouts;
import org.jruby.truffle.RubyContext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

public class CallFrequency {

    private final RubyContext context;
    private final Map<String, AtomicLong> calls = new HashMap<>();

    public CallFrequency(RubyContext context) {
        this.context = context;
    }

    @TruffleBoundary
    public void called(Object receiver, Object name) {
        final DynamicObject metaClass = context.getCoreLibrary().getMetaClass(receiver);
        final String formatted = Layouts.CLASS.getFields(metaClass).getName() + "#" + name;

        AtomicLong count;

        synchronized (this) {
            count = calls.get(formatted);

            if (count == null) {
                count = new AtomicLong();
                calls.put(formatted, count);
            }
        }

        count.incrementAndGet();
    }

    public void start() {
        final Thread printThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    // Doesn't matter
                }

                synchronized (this) {
                    final List<Map.Entry<String, AtomicLong>> entries = new ArrayList<>(calls.entrySet());
                    entries.sort((a, b) -> Long.compare(a.getValue().get(), b.getValue().get()));
                    Collections.reverse(entries);

                    while (entries.size() > 10) {
                        entries.remove(10);
                    }

                    System.err.println(System.currentTimeMillis());

                    for (Map.Entry<String, AtomicLong> entry : entries) {
                        System.err.println("  " + entry.getValue().get() + " " + entry.getKey());
                    }

                    calls.clear();
                }
            }
        });

        printThread.setDaemon(true);
        printThread.start();
    }

}
