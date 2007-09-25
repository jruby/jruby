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
package org.jruby.ast.executable;

import java.io.Reader;
import java.io.IOException;

import java.util.Map;
import java.util.HashMap;

import org.jruby.Ruby;
import org.jruby.RubyArray;

import org.jruby.parser.StaticScope;
import org.jruby.parser.LocalStaticScope;
import org.jruby.runtime.DynamicScope;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.lexer.yacc.SimpleSourcePosition;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubiniusRunner implements Runnable {
    private Ruby runtime;

    public final static int RUBINIUS_BYTECODE_VERSION = 3;

    public RubiniusRunner(Ruby runtime, Reader reader, String filename) {
        try {
            this.runtime = runtime;
            readMagic(reader);
            readVersion(reader);
            readRest(reader);
            reader.close();
        } catch(IOException e) {
            throw new RuntimeException("Couldn't read script: " + e);
        }
    }

    private final void readMagic(Reader reader) throws IOException {
        char[] first = new char[4];
        reader.read(first);
        if(first[0] != 'R' || first[1] != 'B' || first[2] != 'I' || first[3] != 'X') {
            throw new RuntimeException("File is not a compiled Rubinius file");
        }
    }

    private final void readVersion(Reader reader) throws IOException {
        int version = readInt(reader);
        if(version != RUBINIUS_BYTECODE_VERSION) {
            throw new RuntimeException("Can't run Rubinius code with version " + version);
        }
    }

    public static int readInt(Reader reader) throws IOException {
        char[] theInt = new char[4];
        reader.read(theInt);
        int val = 0;
        val += (theInt[0]<<24);
        val += (theInt[1]<<16);
        val += (theInt[2]<<8);
        val += (theInt[3]);
        return val;
    }

    private Map methods = new HashMap();

    private final void readRest(Reader reader) throws IOException {
        RubiniusCMethod obj = null;
        while((obj = unmarshalCMethod(reader)) != null) {
            methods.put(obj.name, obj);
        }
    }

    private final char[] unmarshalCharArray(Reader reader) throws IOException {
        int length = readInt(reader);
        char[] arr = new char[length];
        reader.read(arr);
        return arr;
    }

    private final String unmarshalString(Reader reader) throws IOException {
        int length = readInt(reader);
        char[] arr = new char[length];
        reader.read(arr);
        return String.valueOf(arr);
    }

    private final int unmarshalInt(Reader reader) throws IOException {
        int neg = reader.read();
        int val = readInt(reader);
        if(neg == 'n') {
            val = -val;
        }
        return val;
    }

    private final IRubyObject[] unmarshalTuple(Reader reader) throws IOException {
        int length = readInt(reader);
        IRubyObject[] vals = new IRubyObject[length];
        for(int i=0;i<length;i++) {
            vals[i] = unmarshal(reader);
        }
        return vals;
    }

    private final RubiniusCMethod unmarshalCMethod(Reader reader) throws IOException {
        RubyArray obj = (RubyArray)unmarshal(reader);
        if(obj == null) {
            return null;
        }
        return new RubiniusCMethod(obj);
    }

    private final IRubyObject unmarshal(Reader reader)  throws IOException {
        int tag = reader.read();
        int len = -1;
        char[] data;
        switch(tag) {
        case 'i': return runtime.newFixnum(unmarshalInt(reader));
        case 's': return runtime.newString(unmarshalString(reader));
        case 'x': return runtime.newSymbol(unmarshalString(reader));
        case 'p': return runtime.newArray(unmarshalTuple(reader));
        case 'b': return runtime.newString(unmarshalString(reader)); 
        case 'm': return runtime.newArray(unmarshalTuple(reader));
        case 'B': System.err.println("B"); return null;
        case 'd': System.err.println("d"); return null;
        case 'r': System.err.println("r"); return null;
        case 'n': return runtime.getNil(); 
        case 't': return runtime.getTrue();
        case 'f': return runtime.getFalse();
        }
        return null;
    }

    public void run() {
        RubiniusCMethod method = (RubiniusCMethod)methods.get("__script__");
        ThreadContext context = runtime.getCurrentContext();
        StaticScope scope = new LocalStaticScope(null);

        if (scope.getModule() == null) {
            scope.setModule(runtime.getObject());
        }
        
        scope.setVariables(new String[method.locals]);
        
        context.setPosition(new SimpleSourcePosition(method.file, -1));
        context.preScopedBody(new DynamicScope(scope,null));
        RubiniusMachine.INSTANCE.exec(context, runtime.getObject(), method.code, method.literals, new IRubyObject[0]);
    }
}// RubiniusRunner
