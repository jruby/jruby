/*
 * RubyRuntime.java - No description
 * Created on 09. November 2001, 15:47
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.runtime;

import java.io.*;

import org.jruby.*;
import org.jruby.nodes.*;

/**
 *
 * @author  jpetersen
 */
public class RubyRuntime {
    private Ruby ruby;

    private InputStream inputStream;
    private PrintStream errorStream;
    private PrintStream outputStream;

    public RubyRuntime(Ruby ruby) {
        this.ruby = ruby;
    }
    
    /** This method compiles and interprets a Ruby script.
     *
     *  It can be used if you want to use JRuby as a Macro language.
     *
     */
    public void loadScript(RubyString scriptName, RubyString source, boolean wrap) {
        RubyObject self = ruby.getRubyTopSelf();
        CRefNode savedCRef = ruby.getCRef();
        
        // TMP_PROTECT;
        if (wrap && ruby.getSecurityLevel() >= 4) {
            // Check_Type(fname, T_STRING);
        } else {
            // Check_SafeStr(fname);
        }
        
        // volatile ID last_func;
        // ruby_errinfo = Qnil; /* ensure */
        RubyVarmap.push(ruby);
        ruby.pushClass();
        
        RubyModule wrapper = ruby.getWrapper();
        ruby.setCRef(ruby.getTopCRef());
        
        if (!wrap) {
            ruby.secure(4); /* should alter global state */
            ruby.setRubyClass(ruby.getClasses().getObjectClass());
            ruby.setWrapper(null);
        } else {
            /* load in anonymous module as toplevel */
            ruby.setWrapper(RubyModule.m_newModule(ruby));
            ruby.setRubyClass(ruby.getWrapper());
            self = ruby.getRubyTopSelf().m_clone();
            self.extendObject(ruby.getRubyClass());
            ruby.getCRef().push(ruby.getWrapper());
        }
        ruby.getRubyFrame().push();
        ruby.getRubyFrame().setLastFunc(null);
        ruby.getRubyFrame().setLastClass(null);
        ruby.getRubyFrame().setSelf(self);
        ruby.getRubyFrame().setCbase(new CRefNode(ruby.getRubyClass(), null));
        ruby.getRubyScope().push();
        
        /* default visibility is private at loading toplevel */
        ruby.setActMethodScope(Constants.SCOPE_PRIVATE);
        
        RubyId last_func = ruby.getRubyFrame().getLastFunc();
        try {
            // RubyId last_func = ruby.getRubyFrame().getLastFunc();
            // DEFER_INTS;
            ruby.setInEval(ruby.getInEval() + 1);
            
            ruby.getRubyParser().compileString(scriptName.getValue(), source, 0);
            
            // ---
            ruby.setInEval(ruby.getInEval() - 1);
            
            // evalNode +++
            if (ruby.getParserHelper().getEvalTreeBegin() != null) {
                self.eval(ruby.getParserHelper().getEvalTreeBegin());
                ruby.getParserHelper().setEvalTreeBegin(null);
            }
            if (ruby.getParserHelper().getEvalTree() != null) {
                self.eval(ruby.getParserHelper().getEvalTree());
            }
            // evalNode ---
        } catch (Exception excptn) {
            excptn.printStackTrace(getErrorStream());
        }
        ruby.getRubyFrame().setLastFunc(last_func);
        
        /*if (ruby.getRubyScope().getFlags() == SCOPE_ALLOCA && ruby.getRubyClass() == ruby.getClasses().getObjectClass()) {
            if (ruby_scope->local_tbl)
                free(ruby_scope->local_tbl);
        }*/
        ruby.setCRef(savedCRef);
        ruby.getRubyScope().pop();
        ruby.getRubyFrame().pop();
        ruby.popClass();
        RubyVarmap.pop(ruby);
        ruby.setWrapper(wrapper);
        
        /*if (ruby_nerrs > 0) {
            ruby_nerrs = 0;
            rb_exc_raise(ruby_errinfo);
        }*/
    }
    
    /** This method loads, compiles and interprets a Ruby file.
     *  It is used by Kernel#require.
     *
     *  (matz Ruby: rb_load)
     */
    public void loadFile(RubyString fname, boolean wrap) {
        // fname = findFile(fname);
        if (fname == null) {
            throw new RuntimeException("No such file to load -- " + fname.getValue());
        }
        
        try {
            File rubyFile = new File(fname.getValue());
            StringBuffer source = new StringBuffer((int)rubyFile.length());
            BufferedReader br = new BufferedReader(new FileReader(rubyFile));
            String line;
            while ((line = br.readLine()) != null) {
                source.append(line).append('\n');
            }
            br.close();
            
            loadScript(fname, RubyString.m_newString(ruby, source.toString()), wrap);
            
        } catch (IOException ioExcptn) {
            getErrorStream().println("Cannot read Rubyfile: \"" + fname.getValue() + "\"");
            getErrorStream().println("IOEception: " + ioExcptn.getMessage());
        }
    }
	/**
	 * Gets the errorStream
	 * @return Returns a PrintStream
	 */
	public PrintStream getErrorStream() {
		return errorStream != null ? errorStream : System.err;
	}
    /**
     * Sets the errorStream
     * @param errorStream The errorStream to set
     */
    public void setErrorStream(PrintStream errorStream) {
        this.errorStream = errorStream;
    }

	/**
	 * Gets the inputStream
	 * @return Returns a InputStream
	 */
	public InputStream getInputStream() {
		return inputStream != null ? inputStream : System.in;
	}
    /**
     * Sets the inputStream
     * @param inputStream The inputStream to set
     */
    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

	/**
	 * Gets the outputStream
	 * @return Returns a PrintStream
	 */
	public PrintStream getOutputStream() {
		return outputStream != null ? outputStream : System.out;
	}
    /**
     * Sets the outputStream
     * @param outputStream The outputStream to set
     */
    public void setOutputStream(PrintStream outputStream) {
        this.outputStream = outputStream;
    }

}