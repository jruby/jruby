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

import org.jruby.anno.FrameField;
import org.jruby.anno.JRubyMethod;
import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.ByteList;

public class RubyArgsFile {
    private static final class ArgsFileData {
        private Ruby runtime;
        public ArgsFileData(Ruby runtime) {
            this.runtime = runtime;
        }

        public IRubyObject currentFile;
        public int currentLineNumber;
        public boolean startedProcessing = false; 
        public boolean finishedProcessing = false;

        public boolean nextArgsFile(ThreadContext context) {
            if (finishedProcessing) {
                return false;
            }

            RubyArray args = (RubyArray)runtime.getGlobalVariables().get("$*");

            if (args.getLength() == 0) {
                if (!startedProcessing) { 
                    currentFile = runtime.getGlobalVariables().get("$stdin");
                    ((RubyString) runtime.getGlobalVariables().get("$FILENAME")).setValue(new ByteList(new byte[]{'-'}));
                    currentLineNumber = 0;
                    startedProcessing = true;
                    return true;
                } else {
                    finishedProcessing = true;
                    return false;
                }
            }

            args.shift();
            RubyString filename = (RubyString)args.to_s();
            ByteList filenameBytes = filename.getByteList();
            ((RubyString) runtime.getGlobalVariables().get("$FILENAME")).setValue(filenameBytes);

            if (filenameBytes.length() == 1 && filenameBytes.get(0) == '-') {
                currentFile = runtime.getGlobalVariables().get("$stdin");
            } else {
                currentFile = RubyFile.open(context, runtime.getFile(), new IRubyObject[] {filename.strDup()}, Block.NULL_BLOCK); 
            }
            
            startedProcessing = true;
            return true;
        }

        public static ArgsFileData getDataFrom(IRubyObject recv) {
            ArgsFileData data = (ArgsFileData)recv.dataGetStruct();
            if(data == null) {
                data = new ArgsFileData(recv.getRuntime());
                recv.dataWrapStruct(data);
            }
            return data;
        }
    }    
    
    public static void setCurrentLineNumber(IRubyObject recv, int newLineNumber) {
        ArgsFileData.getDataFrom(recv).currentLineNumber = newLineNumber;
    }

    public static void initArgsFile(Ruby runtime) {
        RubyObject argsFile = new RubyObject(runtime, runtime.getObject());

        runtime.getEnumerable().extend_object(argsFile);
        
        runtime.defineReadonlyVariable("$<", argsFile);
        runtime.defineGlobalConstant("ARGF", argsFile);
        
        RubyClass argfClass = argsFile.getMetaClass();
        argfClass.defineAnnotatedMethods(RubyArgsFile.class);
        runtime.defineReadonlyVariable("$FILENAME", runtime.newString("-"));
    }



    @JRubyMethod(name = {"fileno", "to_i"})
    public static IRubyObject fileno(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(!data.startedProcessing && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream");
        }
        return ((RubyIO)data.currentFile).fileno();
    }

    @JRubyMethod(name = "to_io")
    public static IRubyObject to_io(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(!data.startedProcessing && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream");
        }
        return data.currentFile;
    }
    
    public static IRubyObject internalGets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(data.currentFile == null && !data.nextArgsFile(context)) {
            return recv.getRuntime().getNil();
        }
        
        IRubyObject line = data.currentFile.callMethod(context, "gets", args);
        
        while (line instanceof RubyNil) {
            data.currentFile.callMethod(context, "close");
            if (!data.nextArgsFile(context)) {
                data.currentFile = null;
                return line;
        	}
            line = data.currentFile.callMethod(context, "gets", args);
        }
        
        data.currentLineNumber++;
        recv.getRuntime().getGlobalVariables().set("$.", recv.getRuntime().newFixnum(data.currentLineNumber));
        
        return line;
    }
    
    // ARGF methods

    /** Read a line.
     * 
     */
    @JRubyMethod(name = "gets", optional = 1, frame = true, writes = FrameField.LASTLINE)
    public static IRubyObject gets(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject result = internalGets(context, recv, args);

        if (!result.isNil()) {
            context.getCurrentFrame().setLastLine(result);
        }

        return result;
    }
    
    /** Read a line.
     * 
     */
    @JRubyMethod(name = "readline", optional = 1, frame = true, writes = FrameField.LASTLINE)
    public static IRubyObject readline(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject line = gets(context, recv, args);

        if (line.isNil()) {
            throw recv.getRuntime().newEOFError();
        }
        
        return line;
    }

    @JRubyMethod(name = "readlines", optional = 1, frame = true)
    public static RubyArray readlines(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        IRubyObject[] separatorArgument;
        if (args.length > 0) {
            if (!recv.getRuntime().getNilClass().isInstance(args[0]) &&
                !recv.getRuntime().getString().isInstance(args[0])) {
                throw recv.getRuntime().newTypeError(args[0], 
                        recv.getRuntime().getString());
            } 
            separatorArgument = new IRubyObject[] { args[0] };
        } else {
            separatorArgument = IRubyObject.NULL_ARRAY;
        }

        RubyArray result = recv.getRuntime().newArray();
        IRubyObject line;
        while (! (line = internalGets(context, recv, separatorArgument)).isNil()) {
            result.append(line);
        }
        return result;
    }
    
    @JRubyMethod(name = "each_byte", frame = true)
    public static IRubyObject each_byte(ThreadContext context, IRubyObject recv, Block block) {
        IRubyObject bt;

        while(!(bt = getc(context, recv)).isNil()) {
            block.yield(context, bt);
        }

        return recv;
    }

    /** Invoke a block for each line.
     *
     */
    @JRubyMethod(name = "each_line", alias = {"each"}, optional = 1, frame = true)
    public static IRubyObject each_line(ThreadContext context, IRubyObject recv, IRubyObject[] args, Block block) {
        IRubyObject nextLine = internalGets(context, recv, args);
        
        while (!nextLine.isNil()) {
        	block.yield(context, nextLine);
        	nextLine = internalGets(context, recv, args);
        }
        
        return recv;
    }

    @JRubyMethod(name = "file")
    public static IRubyObject file(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);

        if(data.currentFile == null && !data.nextArgsFile(context)) {
            return recv.getRuntime().getNil();
        }
        return data.currentFile;
    }

    @JRubyMethod(name = "skip")
    public static IRubyObject skip(IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        data.currentFile = null;
        return recv;
    }

    @JRubyMethod(name = "close")
    public static IRubyObject close(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            return recv;
        }
        data.currentFile = null;
        data.currentLineNumber = 0;
        return recv;
    }

    @JRubyMethod(name = "closed?")
    public static IRubyObject closed_p(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            return recv;
        }
        return ((RubyIO)data.currentFile).closed_p();
    }

    @JRubyMethod(name = "binmode")
    public static IRubyObject binmode(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream");
        }
        
        return ((RubyIO)data.currentFile).binmode();
    }

    @JRubyMethod(name = "lineno")
    public static IRubyObject lineno(IRubyObject recv) {
        return recv.getRuntime().newFixnum(ArgsFileData.getDataFrom(recv).currentLineNumber);
    }

    @JRubyMethod(name = "tell", alias = {"pos"})
    public static IRubyObject tell(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream to tell");
        }
        return ((RubyIO)data.currentFile).pos();
    }

    @JRubyMethod(name = "rewind")
    public static IRubyObject rewind(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream to rewind");
        }
        return ((RubyIO)data.currentFile).rewind();
    }

    @JRubyMethod(name = {"eof", "eof?"})
    public static IRubyObject eof(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile != null && !data.nextArgsFile(context)) {
            return recv.getRuntime().getTrue();
        }

        return ((RubyIO)data.currentFile).eof_p();
    }

    @JRubyMethod(name = "pos=", required = 1)
    public static IRubyObject set_pos(ThreadContext context, IRubyObject recv, IRubyObject offset) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream to set position");
        }
        return ((RubyIO)data.currentFile).pos_set(offset);
    }

    @JRubyMethod(name = "seek", required = 1, optional = 1)
    public static IRubyObject seek(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        if(data.currentFile == null && !data.nextArgsFile(context)) {
            throw recv.getRuntime().newArgumentError("no stream to seek");
        }
        return ((RubyIO)data.currentFile).seek(args);
    }

    @JRubyMethod(name = "lineno=", required = 1)
    public static IRubyObject set_lineno(IRubyObject recv, IRubyObject line) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        data.currentLineNumber = RubyNumeric.fix2int(line);
        return recv.getRuntime().getNil();
    }

    @JRubyMethod(name = "readchar")
    public static IRubyObject readchar(ThreadContext context, IRubyObject recv) {
        IRubyObject c = getc(context, recv);
        if(c.isNil()) {
            throw recv.getRuntime().newEOFError();
        }
        return c;
    }

    @JRubyMethod(name = "getc")
    public static IRubyObject getc(ThreadContext context, IRubyObject recv) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        IRubyObject bt;
        while(true) {
            if(data.currentFile == null && !data.nextArgsFile(context)) {
                return recv.getRuntime().getNil();
            }
            if(!(data.currentFile instanceof RubyFile)) {
                bt = data.currentFile.callMethod(context,"getc");
            } else {
                bt = ((RubyIO)data.currentFile).getc();
            }
            if(bt.isNil()) {
                data.currentFile = null;
                continue;
            }
            return bt;
        }
    }

    @JRubyMethod(name = "read", optional = 2)
    public static IRubyObject read(ThreadContext context, IRubyObject recv, IRubyObject[] args) {
        ArgsFileData data = ArgsFileData.getDataFrom(recv);
        IRubyObject tmp, str, length;
        long len = 0;
        if(args.length > 0) {
            length = args[0];
            if(args.length > 1) {
                str = args[1];
            } else {
                str = recv.getRuntime().getNil();
            }
        } else {
            length = recv.getRuntime().getNil();
            str = recv.getRuntime().getNil();
        }

        if(!length.isNil()) {
            len = RubyNumeric.num2long(length);
        }
        if(!str.isNil()) {
            str = str.convertToString();
            ((RubyString)str).modify();
            ((RubyString)str).getByteList().length(0);
            args[1] = recv.getRuntime().getNil();
        }
        while(true) {
            if(data.currentFile == null && !data.nextArgsFile(context)) {
                return str;
            }
            if(!(data.currentFile instanceof RubyIO)) {
                tmp = data.currentFile.callMethod(context, "read", args);
            } else {
                tmp = ((RubyIO)data.currentFile).read(args);
            }
            if(str.isNil()) {
                str = tmp;
            } else if(!tmp.isNil()) {
                ((RubyString)str).append(tmp);
            }
            if(tmp.isNil() || length.isNil()) {
                data.currentFile = null;
                continue;
            } else if(args.length >= 1) {
                if(((RubyString)str).getByteList().length() < len) {
                    len -= ((RubyString)str).getByteList().length();
                    args[0] = recv.getRuntime().newFixnum(len);
                    continue;
                }
            }
            return str;
        }
    }

    @JRubyMethod(name = "filename", alias = {"path"})
    public static RubyString filename(IRubyObject recv) {
        return (RubyString)recv.getRuntime().getGlobalVariables().get("$FILENAME");
    }

    @JRubyMethod(name = "to_s") 
    public static IRubyObject to_s(IRubyObject recv) {
        return recv.getRuntime().newString("ARGF");
    }
}
