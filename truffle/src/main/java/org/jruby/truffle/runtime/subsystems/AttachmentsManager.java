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

import com.oracle.truffle.api.frame.VirtualFrame;
import com.oracle.truffle.api.instrument.Instrument;
import com.oracle.truffle.api.instrument.Probe;
import com.oracle.truffle.api.instrument.StandardInstrumentListener;
import com.oracle.truffle.api.instrument.StandardSyntaxTag;
import com.oracle.truffle.api.nodes.Node;
import com.oracle.truffle.api.source.LineLocation;
import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.tools.LineToProbesMap;
import org.jruby.truffle.runtime.RubyArguments;
import org.jruby.truffle.runtime.RubyContext;
import org.jruby.truffle.runtime.core.RubyBinding;
import org.jruby.truffle.runtime.core.RubyProc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AttachmentsManager {

    private final RubyContext context;
    private final LineToProbesMap lineToProbesMap;
    private final Map<LineLocation, List<Instrument>> attachments = new HashMap<>();

    public AttachmentsManager(RubyContext context) {
        this.context = context;

        // TODO CS 28-Feb-15 this is global isn't it?

        lineToProbesMap = new LineToProbesMap();
        lineToProbesMap.install();
    }

    public synchronized void attach(String file, int line, final RubyProc block) {
        final Instrument instrument = Instrument.create(new StandardInstrumentListener() {

            @Override
            public void enter(Probe probe, Node node, VirtualFrame frame) {
                final RubyBinding binding = new RubyBinding(context.getCoreLibrary().getBindingClass(), RubyArguments.getSelf(frame.getArguments()), frame.materialize());
                block.rootCall(binding);
            }

            @Override
            public void returnVoid(Probe probe, Node node, VirtualFrame virtualFrame) {
            }

            @Override
            public void returnValue(Probe probe, Node node, VirtualFrame virtualFrame, Object o) {
            }

            @Override
            public void returnExceptional(Probe probe, Node node, VirtualFrame virtualFrame, Exception e) {
            }

        }, String.format("Truffle::Primitive.attach@%s:%d", file, line));

        final Source source = context.getSourceManager().forFileBestFuzzily(file);

        final LineLocation lineLocation = source.createLineLocation(line);

        List<Instrument> instruments = attachments.get(lineLocation);

        if (instruments == null) {
            instruments = new ArrayList<>();
            attachments.put(lineLocation, instruments);
        }

        instruments.add(instrument);

        for (Probe probe : lineToProbesMap.findProbes(lineLocation)) {
            if (probe.isTaggedAs(StandardSyntaxTag.STATEMENT)) {
                probe.attach(instrument);
                return;
            }
        }

        throw new RuntimeException("couldn't find a statement!");
    }

    public synchronized void detach(String file, int line) {
        final Source source = context.getSourceManager().forFileBestFuzzily(file);

        final LineLocation lineLocation = source.createLineLocation(line);

        final List<Instrument> instruments = attachments.remove(lineLocation);

        if (instruments != null) {
            for (Instrument instrument : instruments) {
                instrument.dispose();
            }
        }
    }

}
