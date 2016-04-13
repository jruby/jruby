package org.jruby.truffle.platform.darwin;

import jnr.ffi.LibraryLoader;
import jnr.ffi.Pointer;
import org.jruby.truffle.platform.ProcessName;

import java.nio.charset.StandardCharsets;

public class DarwinProcessName implements ProcessName {

    /*
     * When we call _NSGetArgv we seem to always get a string that looks like what we'd expect from running ps, but
     * with a null character inserted early. I don't know where this comes from, but it means I don't know how to get
     * the length of space available for writing in the new program name. We therefore about 40 characters, which is
     * a number without any foundation, but it at least allows the specs to pass, the functionality to be useful,
     * and probably avoid crashing anyone's programs. I can't pretend this is great engineering.
     */
    private static final int MAX_PROGRAM_NAME_LENGTH = 40;

    private final CrtExterns crtExterns;

    public DarwinProcessName() {
        crtExterns = LibraryLoader.create(CrtExterns.class).failImmediately().library("libSystem.B.dylib").load();
    }

    @Override
    public boolean canSet() {
        return true;
    }

    @Override
    public void set(String name) {
        final Pointer programNameAddress = crtExterns._NSGetArgv().getPointer(0).getPointer(0);
        programNameAddress.putString(0, name, MAX_PROGRAM_NAME_LENGTH, StandardCharsets.UTF_8);
    }

}
