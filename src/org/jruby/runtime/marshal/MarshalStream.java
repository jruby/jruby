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

import org.jruby.IRuby;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyFixnum;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
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
    
    protected static char TYPE_UCLASS = 'C';

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
        if (! shouldBeRegistered(value)) {
            writeDirectly(value);
        } else if (hasNewUserDefinedMarshaling(value)) {
            userNewMarshal(value);
        } else if (hasUserDefinedMarshaling(value)) {
            userMarshal(value);
        } else {
            writeAndRegister(value);
        }
        depth--;
        if (depth == 0) {
        	out.flush(); // flush afer whole dump is complete
        }
    }
    
    public void writeUserClass(IRubyObject obj, RubyClass baseClass, MarshalStream output) throws IOException {
    	// TODO: handle w_extended code here
    	
    	if (obj.getMetaClass().equals(baseClass)) {
    		// do nothing, simple builtin type marshalling
    		return;
    	}
    	
    	output.write(TYPE_UCLASS);
    	
    	// w_unique
    	if (obj.getMetaClass().getName().charAt(0) == '#') {
    		throw obj.getRuntime().newTypeError("Can't dump anonymous class");
    	}
    	
    	// w_symbol
    	// TODO: handle symlink?
    	output.dumpObject(RubySymbol.newSymbol(obj.getRuntime(), obj.getMetaClass().getName()));
    }
    
    public void writeInstanceVars(IRubyObject obj, MarshalStream output) throws IOException {
    	IRuby runtime = obj.getRuntime();
        output.dumpInt(obj.getInstanceVariables().size());
        
        for (Iterator iter = obj.instanceVariableNames(); iter.hasNext();) {
            String name = (String) iter.next();
            IRubyObject value = obj.getInstanceVariable(name);

            // Between getting name and retrieving value the instance variable could have been
            // removed
            if (value != null) {
            	output.dumpObject(runtime.newSymbol(name));
            	output.dumpObject(value);
            }
        }
    }

    private void writeDirectly(IRubyObject value) throws IOException {
        if (value.isNil()) {
            out.write('0');
        } else {
            value.marshalTo(this);
        }
    }

    private boolean shouldBeRegistered(IRubyObject value) {
        if (value.isNil()) {
            return false;
        } else if (value instanceof RubyBoolean) {
            return false;
        } else if (value instanceof RubyFixnum) {
            return false;
        }
        return true;
    }

    private void writeAndRegister(IRubyObject value) throws IOException {
        if (cache.isRegistered(value)) {
            cache.writeLink(this, value);
        } else {
            cache.register(value);
            value.marshalTo(this);
        }
    }

    private boolean hasUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("_dump");
    }

    private boolean hasNewUserDefinedMarshaling(IRubyObject value) {
        return value.respondsTo("marshal_dump");
    }

    private void userMarshal(IRubyObject value) throws IOException {
        out.write(TYPE_USERDEF);
        dumpObject(RubySymbol.newSymbol(runtime, value.getMetaClass().getName()));

        RubyString marshaled = (RubyString) value.callMethod("_dump", runtime.newFixnum(depthLimit)); 
        dumpString(marshaled.toString());
    }

    private void userNewMarshal(final IRubyObject value) throws IOException {
        out.write(TYPE_USRMARSHAL);
        dumpObject(RubySymbol.newSymbol(runtime, value.getMetaClass().getName()));

        IRubyObject marshaled =  value.callMethod("marshal_dump"); 
        dumpObject(marshaled);
    }

    public void dumpString(String value) throws IOException {
        dumpInt(value.length());
        out.write(RubyString.stringToBytes(value));
    }

    public void dumpInt(int value) throws IOException {
        if (value == 0) {
            out.write(0);
        } else if (0 < value && value < 123) {
            out.write(value + 5);
        } else if (-124 < value && value < 0) {
            out.write((value - 5) & 0xff);
        } else {
            int[] buf = new int[4];
            int i;
            for (i = 0; i < buf.length; i++) {
                buf[i] = value & 0xff;
                value = value >> 8;
                if (value == 0 || value == -1) {
                    break;
                }
            }
            int len = i + 1;
            out.write(value < 0 ? -len : len);
            for (i = 0; i < len; i++) {
                out.write(buf[i]);
            }
        }
    }

	public void writeIVar(IRubyObject obj, MarshalStream output) throws IOException {
		if (obj.getInstanceVariables().size() > 0) {
			out.write(TYPE_IVAR);
		} 
	}
}
