/*
 * RubyArgsFile.java - No description
 * Created on 13.01.2002, 17:08:47
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen, Alan Moore, Benoit Cerrina, Chad Fowler
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
 * Chad Fowler <chadfowler@yahoo.com>
 * 
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.jruby.exceptions.IOError;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.RubyInputStream;

public class RubyArgsFile extends RubyObject {

    public RubyArgsFile(Ruby ruby) {
        super(ruby, ruby.getClasses().getObjectClass());
    }

    private boolean first_p = true;
    private int next_p = 0;
    private boolean init_p = false;

    //
    private RubyIO currentFile;
    //private RubyString currentFilename;
    private int currentLineNumber;
    
    public void setCurrentLineNumber(int newLineNumber) {
        this.currentLineNumber = newLineNumber;
    }
    
    public void initArgsFile() {
        extendObject(ruby.getClasses().getEnumerableModule());
        
        ruby.defineReadonlyVariable("$<", this);
        ruby.defineGlobalConstant("ARGF", this);
        
        defineSingletonMethod("each", CallbackFactory.getOptMethod(RubyArgsFile.class, "each_line"));
        defineSingletonMethod("each_line", CallbackFactory.getOptMethod(RubyArgsFile.class, "each_line"));

		defineSingletonMethod("filename", CallbackFactory.getMethod(RubyArgsFile.class, "filename"));
		defineSingletonMethod("gets", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "gets"));
		defineSingletonMethod("readline", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readline"));
		defineSingletonMethod("readlines", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
		
		defineSingletonMethod("to_a", CallbackFactory.getOptSingletonMethod(RubyGlobal.class, "readlines"));
		defineSingletonMethod("to_s", CallbackFactory.getMethod(RubyArgsFile.class, "filename"));

        ruby.defineReadonlyVariable("$FILENAME", RubyString.newString(ruby, "-"));
        currentFile = (RubyIO) ruby.getGlobalVar("$stdin");
    }

    protected boolean nextArgsFile() {
        RubyArray args = (RubyArray)ruby.getGlobalVar("$*");

        if (!init_p) {
            if (args.getLength() > 0) {
                next_p = 1;
            } else {
                next_p = -1;
                currentFile = (RubyIO) ruby.getGlobalVar("$stdin");
                ((RubyString) ruby.getGlobalVar("$FILENAME")).setValue("-");
            }
            init_p = true;
            first_p = false;
            currentLineNumber = 0;
        }

        // retry : while (true) {
        if (next_p == 1) {
            next_p = 0;
            if (args.getLength() > 0) {
                String filename = ((RubyString) args.shift()).getValue();
                ((RubyString) ruby.getGlobalVar("$FILENAME")).setValue(filename);

                if (filename.equals("-")) {
                    currentFile = (RubyIO) ruby.getGlobalVar("$stdin");
                    /*if (ruby_inplace_mode)
                    {
                        rb_warn("Can't do inplace edit for stdio");
                        rb_defout = rb_stdout;
                    }*/
                } else {
                    File file = new File(filename);
                    try {
                        RubyInputStream inStream = new RubyInputStream(new BufferedInputStream(new FileInputStream(file)));
                        // FILE *fr = rb_fopen(fn, "r");

                        /*if (ruby_inplace_mode) {
                            struct stat st, st2;
                            VALUE str;
                            FILE *fw;
                        
                            if (TYPE(rb_defout) == T_FILE && rb_defout != rb_stdout)
                            {
                                rb_io_close(rb_defout);
                            }
                            fstat(fileno(fr), &st);
                            if (*ruby_inplace_mode)
                            {
                                str = rb_str_new2(fn);
                        #ifdef NO_LONG_FNAME
                        
                                ruby_add_suffix(str, ruby_inplace_mode);
                        #else
                        
                                rb_str_cat2(str, ruby_inplace_mode);
                        #endif
                        #ifdef NO_SAFE_RENAME
                        
                                (void)fclose(fr);
                                (void)unlink(RSTRING(str)->ptr);
                                (void)rename(fn, RSTRING(str)->ptr);
                                fr = rb_fopen(RSTRING(str)->ptr, "r");
                        #else
                        
                                if (rename(fn, RSTRING(str)->ptr) < 0)
                                {
                                    rb_warn("Can't rename %s to %s: %s, skipping file",
                                            fn, RSTRING(str)->ptr, strerror(errno));
                                    fclose(fr);
                                    goto retry;
                                }
                        #endif
                            }
                            else
                            {
                        #ifdef NO_SAFE_RENAME
                                rb_fatal("Can't do inplace edit without backup");
                        #else
                        
                                if (unlink(fn) < 0)
                                {
                                    rb_warn("Can't remove %s: %s, skipping file",
                                            fn, strerror(errno));
                                    fclose(fr);
                                    goto retry;
                                }
                        #endif
                            }
                            fw = rb_fopen(fn, "w");
                        #ifndef NO_SAFE_RENAME
                        
                            fstat(fileno(fw), &st2);
                        #ifdef HAVE_FCHMOD
                        
                            fchmod(fileno(fw), st.st_mode);
                        #else
                        
                            chmod(fn, st.st_mode);
                        #endif
                        
                            if (st.st_uid!=st2.st_uid || st.st_gid!=st2.st_gid)
                            {
                                fchown(fileno(fw), st.st_uid, st.st_gid);
                            }
                        #endif
                            rb_defout = prep_stdio(fw, FMODE_WRITABLE, rb_cFile);
                            prep_path(rb_defout, fn);
                        }*/

                        currentFile = new RubyFile(ruby, ruby.getClasses().getFileClass());
                        currentFile.initIO(inStream, null, filename);

                        // prep_stdio(fr, FMODE_READABLE, rb_cFile);
                        // prep_path(current_file, fn);
                    } catch (FileNotFoundException fnfExcptn) {
                        throw new IOError(ruby, fnfExcptn.getMessage());
                    }
                }
                /*if (binmode)
                    rb_io_binmode(current_file);*/
            } else {
                init_p = false;
                return false;
            }
        }
        //break;
        //}
        return true;
    }
    
    public RubyString internalGets(IRubyObject[] args) {
        if (!nextArgsFile()) {
            return RubyString.nilString(ruby);
        }

        RubyString line = (RubyString)currentFile.callMethod("gets", args);
        
        while (line.isNil() && next_p != -1) {
        	next_p = 1;
            currentFile.callMethod("close");
            
            if (!nextArgsFile()) {
            	return line;
        	}
            line = (RubyString)currentFile.callMethod("gets", args);
        }
        
        currentLineNumber++;
        ruby.setGlobalVar("$.", RubyFixnum.newFixnum(ruby, currentLineNumber));
        
        return line;
    }
    
    // ARGF methods
    
    /** Invoke a block for each line.
     * 
     */
    public IRubyObject each_line(IRubyObject[] args) {
        RubyString nextLine = internalGets(args);
        
        while (!nextLine.isNil()) {
        	getRuntime().yield(nextLine);
        	nextLine = internalGets(args);
        }
        
        return this;
    }
    
	public RubyString filename() {
        return (RubyString)ruby.getGlobalVar("$FILENAME");
    }
}
