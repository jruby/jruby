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
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
 * Copyright (C) 2007 Ola Bini <ola@ologix.com>
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
package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Arity;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby runtime) {
        super(runtime, runtime.getObject());
    }

    private IRubyObject currentFile;
    private int currentLineNumber;
    private boolean startedProcessing = false; 
    private boolean finishedProcessing = false;
    
    public void setCurrentLineNumber(int newLineNumber) {
        this.currentLineNumber = newLineNumber;
    }
    
    public void initArgsFile() {
        getRuntime().getEnumerable().extend_object(this);
        
        getRuntime().defineReadonlyVariable("$<", this);
        getRuntime().defineGlobalConstant("ARGF", this);
        
        RubyClass argfClass = getMetaClass();
        argfClass.defineAnnotatedMethods(RubyArgsFile.class);
        getRuntime().defineReadonlyVariable("$FILENAME", getRuntime().newString("-"));
    }

    protected boolean nextArgsFile() {
        if (finishedProcessing) {
            return false;
        }

        RubyArray args = (RubyArray)getRuntime().getGlobalVariables().get("$*");

        if (args.getLength() == 0) {
            if (!startedProcessing) { 
                currentFile = getRuntime().getGlobalVariables().get("$stdin");
                ((RubyString) getRuntime().getGlobalVariables().get("$FILENAME")).setValue(new StringBuffer("-"));
                currentLineNumber = 0;
                startedProcessing = true;
                return true;
            } else {
                finishedProcessing = true;
                return false;
            }
        }

        String filename = args.shift().toString();
        ((RubyString) getRuntime().getGlobalVariables().get("$FILENAME")).setValue(new StringBuffer(filename));

        if (filename.equals("-")) {
            currentFile = getRuntime().getGlobalVariables().get("$stdin");
        } else {
            currentFile = RubyFile.open(getRuntime().getFile(), new IRubyObject[] {getRuntime().newString(filename)}, Block.NULL_BLOCK); 
        }

        startedProcessing = true;
        return true;
    }

    @JRubyMethod(name = {"fileno", "to_i"})
    public IRubyObject fileno() {
        if(!startedProcessing && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream");
        }
        return ((RubyIO)currentFile).fileno();
    }

    @JRubyMethod(name = "to_io")
    public IRubyObject to_io() {
        if(currentFile == null && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream");
        }
        return currentFile;
    }
    
    public IRubyObject internalGets(IRubyObject[] args) {
        if (currentFile == null && !nextArgsFile()) {
            return getRuntime().getNil();
        }
        
        ThreadContext context = getRuntime().getCurrentContext();
        
        IRubyObject line = currentFile.callMethod(context, "gets", args);
        
        while (line instanceof RubyNil) {
            currentFile.callMethod(context, "close");
            if (!nextArgsFile()) {
                currentFile = null;
                return line;
        	}
            line = currentFile.callMethod(context, "gets", args);
        }
        
        currentLineNumber++;
        getRuntime().getGlobalVariables().set("$.", getRuntime().newFixnum(currentLineNumber));
        
        return line;
    }
    
    // ARGF methods

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "gets", optional = 1, frame = true)
    public IRubyObject gets(IRubyObject[] args) {
        IRubyObject result = internalGets(args);

        if (!result.isNil()) {
            getRuntime().getCurrentContext().getCurrentFrame().setLastLine(result);
        }

        return result;
    }
    
    /** Read a line.
     * 
     */
    @JRubyMethod(name = "readline", optional = 1, frame = true)
    public IRubyObject readline(IRubyObject[] args) {
        IRubyObject line = gets(args);

        if (line.isNil()) {
            throw getRuntime().newEOFError();
        }
        
        return line;
    }

    @JRubyMethod(name = "readlines", optional = 1, frame = true)
    public RubyArray readlines(IRubyObject[] args) {
        IRubyObject[] separatorArgument;
        if (args.length > 0) {
            if (!getRuntime().getNilClass().isInstance(args[0]) &&
                !getRuntime().getString().isInstance(args[0])) {
                throw getRuntime().newTypeError(args[0], 
                        getRuntime().getString());
            } 
            separatorArgument = new IRubyObject[] { args[0] };
        } else {
            separatorArgument = IRubyObject.NULL_ARRAY;
        }

        RubyArray result = getRuntime().newArray();
        IRubyObject line;
        while (! (line = internalGets(separatorArgument)).isNil()) {
            result.append(line);
        }
        return result;
    }
    
    @JRubyMethod(name = "each_byte", frame = true)
    public IRubyObject each_byte(Block block) {
        IRubyObject bt;
        ThreadContext ctx = getRuntime().getCurrentContext();

        while(!(bt = getc()).isNil()) {
            block.yield(ctx, bt);
        }

        return this;
    }

    /** Invoke a block for each line.
     *
     */
    @JRubyMethod(name = "each_line", alias = {"each"}, optional = 1, frame = true)
    public IRubyObject each_line(IRubyObject[] args, Block block) {
        IRubyObject nextLine = internalGets(args);
        
        while (!nextLine.isNil()) {
        	block.yield(getRuntime().getCurrentContext(), nextLine);
        	nextLine = internalGets(args);
        }
        
        return this;
    }

    @JRubyMethod(name = "file")
    public IRubyObject file() {
        if(currentFile == null && !nextArgsFile()) {
            return getRuntime().getNil();
        }
        return currentFile;
    }

    @JRubyMethod(name = "skip")
    public IRubyObject skip() {
        currentFile = null;
        return this;
    }

    @JRubyMethod(name = "close")
    public IRubyObject close() {
        if(currentFile == null && !nextArgsFile()) {
            return this;
        }
        currentFile = null;
        currentLineNumber = 0;
        return this;
    }

    @JRubyMethod(name = "closed?")
    public IRubyObject closed_p() {
        if(currentFile == null && !nextArgsFile()) {
            return this;
        }
        return ((RubyIO)currentFile).closed_p();
    }

    @JRubyMethod(name = "binmode")
    public IRubyObject binmode() {
        if(currentFile == null && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream");
        }
        
        return ((RubyIO)currentFile).binmode();
    }

    @JRubyMethod(name = "lineno")
    public IRubyObject lineno() {
        return getRuntime().newFixnum(currentLineNumber);
    }

    @JRubyMethod(name = "tell", alias = {"pos"})
    public IRubyObject tell() {
        if(currentFile == null && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream to tell");
        }
        return ((RubyIO)currentFile).pos();
    }

    @JRubyMethod(name = "rewind")
    public IRubyObject rewind() {
        if(currentFile == null && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream to rewind");
        }
        return ((RubyIO)currentFile).rewind();
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public IRubyObject eof() {
        if(currentFile != null && !nextArgsFile()) {
            return getRuntime().getTrue();
        }

        return ((RubyIO)currentFile).eof_p();
    }

    @JRubyMethod(name = "pos=", required = 1)
    public IRubyObject set_pos(IRubyObject offset) {
        if(currentFile == null && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream to set position");
        }
        return ((RubyIO)currentFile).pos_set(offset);
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1)
    public IRubyObject seek(IRubyObject[] args) {
        if(currentFile == null && !nextArgsFile()) {
            throw getRuntime().newArgumentError("no stream to seek");
        }
        return ((RubyIO)currentFile).seek(args);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public IRubyObject set_lineno(IRubyObject line) {
        currentLineNumber = RubyNumeric.fix2int(line);
        return getRuntime().getNil();
    }

    @JRubyMethod(name = "readchar")
    public IRubyObject readchar() {
        IRubyObject c = getc();
        if(c.isNil()) {
            throw getRuntime().newEOFError();
        }
        return c;
    }

    @JRubyMethod(name = "getc")
    public IRubyObject getc() {
        IRubyObject bt;
        while(true) {
            if(currentFile == null && !nextArgsFile()) {
                return getRuntime().getNil();
            }
            if(!(currentFile instanceof RubyFile)) {
                bt = currentFile.callMethod(getRuntime().getCurrentContext(),"getc");
            } else {
                bt = ((RubyIO)currentFile).getc();
            }
            if(bt.isNil()) {
                currentFile = null;
                continue;
            }
            return bt;
        }
    }

    @JRubyMethod(name = "read", optional = 2)
    public IRubyObject read(IRubyObject[] args) {
        IRubyObject tmp, str, length;
        long len = 0;
        Arity.checkArgumentCount(getRuntime(), args,0,2);
        if(args.length > 0) {
            length = args[0];
            if(args.length > 1) {
                str = args[1];
            } else {
                str = getRuntime().getNil();
            }
        } else {
            length = getRuntime().getNil();
            str = getRuntime().getNil();
        }

        if(!length.isNil()) {
            len = RubyNumeric.num2long(length);
        }
        if(!str.isNil()) {
            str = str.convertToString();
            ((RubyString)str).modify();
            ((RubyString)str).getByteList().length(0);
            args[1] = getRuntime().getNil();
        }
        while(true) {
            if(currentFile == null && !nextArgsFile()) {
                return str;
            }
            if(!(currentFile instanceof RubyIO)) {
                tmp = currentFile.callMethod(getRuntime().getCurrentContext(),"read",args);
            } else {
                tmp = ((RubyIO)currentFile).read(args);
            }
            if(str.isNil()) {
                str = tmp;
            } else if(!tmp.isNil()) {
                ((RubyString)str).append(tmp);
            }
            if(tmp.isNil() || length.isNil()) {
                currentFile = null;
                continue;
            } else if(args.length >= 1) {
                if(((RubyString)str).getByteList().length() < len) {
                    len -= ((RubyString)str).getByteList().length();
                    args[0] = getRuntime().newFixnum(len);
                    continue;
                }
            }
            return str;
        }
    }

    @JRubyMethod(name = "filename", alias = {"path"})
    public RubyString filename() {
        return (RubyString)getRuntime().getGlobalVariables().get("$FILENAME");
    }

    @JRubyMethod(name = "to_s")
    public IRubyObject to_s() {
        return getRuntime().newString("ARGF");
    }
}
