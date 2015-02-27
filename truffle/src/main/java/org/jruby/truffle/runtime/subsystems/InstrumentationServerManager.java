/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.runtime.subsystems;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;

import org.jruby.truffle.runtime.RubyCallStack;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.backtrace.Backtrace;
import org.jruby.truffle.runtime.core.RubyThread;
import org.jruby.truffle.runtime.util.Consumer;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;

@SuppressWarnings("restriction")
public class InstrumentationServerManager {

    private final RubyContext context;
    private final int port;

    public InstrumentationServerManager(RubyContext context, int port) {
        this.context = context;
        this.port = port;
    }

    public void start() {
        final HttpServer server;

        try {
            server = HttpServer.create(new InetSocketAddress(port), 0);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        server.createContext("/stacks", new HttpHandler() {

            @Override
            public void handle(HttpExchange httpExchange) {
                try {
                    final StringBuilder builder = new StringBuilder();

                    context.getSafepointManager().pauseAllThreadsAndExecuteFromNonRubyThread(new Consumer<RubyThread>() {

                        @Override
                        public void accept(RubyThread thread) {
                            try {
                                Backtrace backtrace = RubyCallStack.getBacktrace(null);

                                synchronized (this) {
                                    // Not thread-safe so keep the formatting synchronized for now.
                                    String[] lines = Backtrace.DISPLAY_FORMATTER.format(context, null, backtrace);

                                    builder.append(Thread.currentThread().getName());
                                    builder.append("\n");
                                    for (String line : lines) {
                                        builder.append(line);
                                        builder.append("\n");
                                    }
                                    builder.append("\n");
                                }
                            } catch (Throwable e) {
                                e.printStackTrace();
                            }
                        }

                    });

                    final byte[] bytes = builder.toString().getBytes("UTF-8");

                    httpExchange.getResponseHeaders().set("Content-Type", "text/plain");
                    httpExchange.sendResponseHeaders(200, bytes.length);

                    final OutputStream stream = httpExchange.getResponseBody();
                    stream.write(bytes);
                    stream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

        });

        server.start();
    }

}
