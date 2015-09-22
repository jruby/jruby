/*
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle;

import com.oracle.truffle.api.vm.TruffleVM;
import org.jruby.truffle.runtime.RubyLanguage;

import java.io.*;
import java.nio.charset.StandardCharsets;

public class TruffleVMMain {

    public static void main(String[] args) {
        final TruffleVM vm = TruffleVM.newVM().build();

        for (String arg : args) {
            try (InputStream inputStream = new FileInputStream(arg)) {
                final Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
                vm.eval(RubyLanguage.MIME_TYPE, reader);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

}
