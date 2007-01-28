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
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Charles O Nutter <headius@headius.com>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2006 Ola Bini <ola.bini@ki.se>
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
package org.jruby.runtime.marshal;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.Iterator;
import java.util.Map;
import org.jruby.IRuby;
import org.jruby.RubyArray;
import org.jruby.RubyBignum;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyFloat;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyRegexp;
import org.jruby.RubyString;
import org.jruby.RubyStruct;
import org.jruby.RubySymbol;
import org.jruby.runtime.ClassIndex;
import org.jruby.runtime.Constants;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * Marshals objects into Ruby's binary marshal format.
 *
 * @author Anders
 */
public class MarshalStream extends FilterOutputStream {
    private final IRuby runtime;
    private final int depthLimit;
    private int depth = 0;
    private MarshalCache cache;

    private final static char TYPE_IVAR = 'I';
    private final static char TYPE_USRMARSHAL = 'U';
    private final static char TYPE_USERDEF = 'u';
    private final static char TYPE_UCLASS = 'C';

    public MarshalStream(IRuby runtime, OutputStream out, int depthLimit) throws IOException {
        super(out);

        this.runtime = runtime;
        this.depthLimit = depthLimit >= 0 ? depthLimit : Integer.MAX_VALUE;
        this.cache = new MarshalCache();

        out.write(Constants.MARSHAL_MAJOR);
        out.write(Constants.MARSHAL_MINOR);
    }

    public void dumpObject(IRubyObject value) throws IOException {
        depth++;
        
        if (depth > depthLimit) {
            throw runtime.newArgumentError("exceed depth limit");
        }
        
        if (!shouldBeRegistered(value)) {
            writeDirectly(value);
        } else {
            writeAndRegister(value);
        }
        depth--;
        if (depth == 0) {
            out.flush(); // flush afer whole dump is complete
        }
    }

    private boolean shouldBeRegistered(IRubyObject value) {
        if (value.isNil()) {
            return false;
        } else if (value instanceof RubyBoolean) {
            return false;
        } else if (value instanceof RubyFixnum) {
            return false;
        } else if (value instanceof RubyFloat) {
            return false;
        }
        return true;
    }

    private void writeAndRegister(IRubyObject value) throws IOException {
        if (cache.isRegistered(value)) {
            cache.writeLink(this, value);
        } else {
            cache.register(value);
            if (hasNewUserDefinedMarshaling(value)) {
                userNewMarshal(value);
            } else if (hasUserDefinedMarshaling(value)) {
                userMarshal(value);
            } else {
                writeDirectly(value);
            }
        }
    }

    private void writeDirectly(IRubyObject value) throws IOException {
        Map instanceVariables = null;
        
        if (value.getNativeTypeIndex() != ClassIndex.OBJECT) {
            if (value.safeHasInstanceVariables() && value.getNativeTypeIndex() != ClassIndex.CLASS) {
                // object has instance vars and isn't a class, get a snapshot to be marshalled
                // and output the ivar header here

                instanceVariables = value.safeGetInstanceVariables();

                // write `I' instance var signet if class is NOT a direct subclass of Object
                write(TYPE_IVAR);
            }

            if (value.getNativeTypeIndex() != value.getMetaClass().index) {
                // object is a custom class that extended one of the native types other than Object
                writeUserClass(value);
            }
        } // Object's instance var logic is handled in the metaclass's marshal
        
        writeObjectData(value);
        
        if (instanceVariables != null) {
            dumpInstanceVars(instanceVariables);
        }
    }

    private void writeObjectData(IRubyObject value) throws IOException {
        // switch on the object's *native type*. This allows use-defined
        // classes that have extended core native types to piggyback on their
        // marshalling logic.
        switch (value.getNativeTypeIndex()) {
        case ClassIndex.ARRAY:
            write('[');
            RubyArray.marshalTo((RubyArray)value, this);
            break;
        case ClassIndex.FALSE:
            write('F');
            break;
        case ClassIndex.FIXNUM: {
            RubyFixnum fixnum = (RubyFixnum)value;
            
            if (fixnum.getLongValue() <= RubyFixnum.MAX_MARSHAL_FIXNUM) {
                write('i');
                writeInt((int) fixnum.getLongValue());
                break;
            }
            // FIXME: inefficient; constructing a bignum just for dumping?
            value = RubyBignum.newBignum(value.getRuntime(), fixnum.getLongValue());
            
            // fall through
        }
        case ClassIndex.BIGNUM:
            write('l');
            RubyBignum.marshalTo((RubyBignum)value, this);
            break;
        case ClassIndex.CLASS:
            write('c');
            RubyClass.marshalTo((RubyClass)value, this);
            break;
        case ClassIndex.FLOAT:
            write('f');
            RubyFloat.marshalTo((RubyFloat)value, this);
            break;
        case ClassIndex.HASH: {
            RubyHash hash = (RubyHash)value;
            
            if (hash.hasNonProcDefault()) {
                write('}');
            } else {
		write('{');
            }
            
            RubyHash.marshalTo(hash, this);
            break;
        }
        case ClassIndex.MODULE:
            write('m');
            RubyModule.marshalTo((RubyModule)value, this);
            break;
        case ClassIndex.NIL:
            write('0');
            break;
        case ClassIndex.OBJECT:
            dumpDefaultObjectHeader(value.getMetaClass());
            value.getMetaClass().marshal(value, this);
            break;
        case ClassIndex.REGEXP:
            write('/');
            RubyRegexp.marshalTo((RubyRegexp)value, this);
            break;
        case ClassIndex.STRING:
            write('"');
            writeString(value.toString());
            break;
        case ClassIndex.STRUCT:
            write('S');
            RubyStruct.marshalTo((RubyStruct)value, this);
            break;
        case ClassIndex.SYMBOL:
            write(':');
            writeString(value.toString());
            break;
        case ClassIndex.TRUE:
            write('T');
            break;
        default:
            dumpDefaultObjectHeader(value.getMetaClass());
            value.getMetaClass().marshal(value, this);
        }
    }

    private boolean hasNewUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("marshal_dump");
    }

    private void userNewMarshal(final IRubyObject value) throws IOException {
        write(TYPE_USRMARSHAL);
        dumpObject(RubySymbol.newSymbol(runtime, value.getMetaClass().getName()));

        IRubyObject marshaled =  value.callMethod(runtime.getCurrentContext(), "marshal_dump"); 
        dumpObject(marshaled);
    }

    private boolean hasUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("_dump");
    }

    private void userMarshal(IRubyObject value) throws IOException {
        write(TYPE_USERDEF);
        dumpObject(RubySymbol.newSymbol(runtime, value.getMetaClass().getName()));

        RubyString marshaled = (RubyString) value.callMethod(runtime.getCurrentContext(), "_dump", runtime.newFixnum(depthLimit)); 
        writeString(marshaled.toString());
    }
    
    public void writeUserClass(IRubyObject obj) throws IOException {
        write(TYPE_UCLASS);
    	
    	// w_unique
    	if (obj.getMetaClass().getName().charAt(0) == '#') {
    		throw obj.getRuntime().newTypeError("Can't dump anonymous class");
    	}
    	
    	// w_symbol
    	// TODO: handle symlink?
    	dumpSymbol(obj.getMetaClass().getName());
    }
    
    public void dumpInstanceVars(Map instanceVars) throws IOException {
        writeInt(instanceVars.size());
        for (Iterator iter = instanceVars.keySet().iterator(); iter.hasNext();) {
            String name = (String) iter.next();
            IRubyObject value = (IRubyObject)instanceVars.get(name);

            writeAndRegister(runtime.newSymbol(name));
            dumpObject(value);
        }
    }
    
    public void dumpDefaultObjectHeader(RubyClass type) throws IOException {
        write('o');
        RubySymbol classname = RubySymbol.newSymbol(runtime, type.getName());
        dumpObject(classname);
    }

    public void writeString(String value) throws IOException {
        writeInt(value.length());
        out.write(RubyString.stringToBytes(value));
    }

    public void dumpSymbol(String value) throws IOException {
        write(':');
        writeInt(value.length());
        out.write(RubyString.stringToBytes(value));
    }

    public void writeInt(int value) throws IOException {
        if (value == 0) {
            out.write(0);
        } else if (0 < value && value < 123) {
            out.write(value + 5);
        } else if (-124 < value && value < 0) {
            out.write((value - 5) & 0xff);
        } else {
            byte[] buf = new byte[4];
            int i = 0;
            for (; i < buf.length; i++) {
                buf[i] = (byte)(value & 0xff);
                
                value = value >> 8;
                if (value == 0 || value == -1) {
                    break;
                }
            }
            int len = i + 1;
            out.write(value < 0 ? -len : len);
            out.write(buf, 0, i + 1);
        }
    }
}
