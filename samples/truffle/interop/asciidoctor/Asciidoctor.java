/*
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */

/*
 * To compile:
 *
 * graalvm/bin/javac -classpath graalvm/jre/lib/truffle/truffle-api.jar samples/truffle/interop/asciidoctor/Asciidoctor.java
 *
 * To run:
 *
 * RUBYOPT=-Iasciidoctor/lib graalvm/bin/java -polyglot -classpath samples/truffle/interop/asciidoctor Asciidoctor asciidoctor/benchmark/sample-data/mdbasics.adoc
 *
 */

import com.oracle.truffle.api.source.Source;
import com.oracle.truffle.api.vm.PolyglotEngine;

public class Asciidoctor {

    public static void main(String[] args) throws Exception {
        final PolyglotEngine polyglotEngine = PolyglotEngine
                .newBuilder()
                .globalSymbol("file", args[0])
                .build();

        polyglotEngine.eval(Source
                .newBuilder("require 'asciidoctor'")
                .name("hello")
                .mimeType("application/x-ruby")
                .build());

        final Source convertSource = Source
                .newBuilder("Asciidoctor.load(Truffle::Interop.from_java_string(Truffle::Interop.import('file'))).convert")
                .name("convert")
                .mimeType("application/x-ruby")
                .build();

        while (true) {
            final long startTime = System.currentTimeMillis();
            polyglotEngine.eval(convertSource);
            System.err.println(System.currentTimeMillis() - startTime);
        }
    }

}
