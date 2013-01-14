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
 * Copyright (C) 2013 The JRuby Team (jruby@jruby.org)
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
package org.jruby.ext.ripper;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyNumeric;
import org.jruby.RubyObject;
import org.jruby.anno.JRubyMethod;
import org.jruby.lexer.yacc.InputStreamLexerSource;
import org.jruby.lexer.yacc.LexerSource;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

public class RubyRipper extends RubyObject {
    public static void initRipper(Ruby runtime) {
        RubyClass ripper = runtime.defineClass("Ripper", runtime.getObject(), new ObjectAllocator() {
            @Override
            public IRubyObject allocate(Ruby runtime, RubyClass klazz) {
                return new RubyRipper(runtime, klazz);
            }
        });
        
        ripper.defineAnnotatedMethods(RubyRipper.class);
    }

    private RubyRipper(Ruby runtime, RubyClass klazz) {
        super(runtime, klazz);
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src) {
        return initialize(context, src, null, null);
    }

    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src, IRubyObject file) {
        return initialize(context, src, file, null);
    }
    
    @JRubyMethod
    public IRubyObject initialize(ThreadContext context, IRubyObject src,IRubyObject file, IRubyObject line) {
        String stringSource = sourceAsString(context, src);
        filename = filenameAsString(context, file);
        int lineno = lineAsInt(context, line);
        ByteArrayInputStream bos = new ByteArrayInputStream(stringSource.getBytes());
        LexerSource source = new InputStreamLexerSource(filename.asJavaString(), bos, null, lineno, true);
        parser = new Ripper19Parser(source);
         
        return context.runtime.getNil();
    }

    @JRubyMethod
    public IRubyObject column(ThreadContext context) {
        return context.runtime.newFixnum(parser.getColumn());
    }

    @JRubyMethod
    public IRubyObject encoding(ThreadContext context) {
        return null;
    }

    @JRubyMethod(name = "end_seen?")
    public IRubyObject end_seen_p(ThreadContext context) {
        return null;
    }
    
    @JRubyMethod
    public IRubyObject filename(ThreadContext context) {
        return filename;
    }

    @JRubyMethod
    public IRubyObject lineno(ThreadContext context) {
        return context.runtime.newFixnum(parser.getLineno());
    }
    
    @JRubyMethod
    public IRubyObject parse(ThreadContext context) {
        try {
            parser.parse(true);
        } catch (IOException e) {
            System.out.println("ERRROR: " + e);
        }
        return null;
    }    

    @JRubyMethod
    public IRubyObject yydebug(ThreadContext context) {
        return null;
    }
    
    @JRubyMethod(name = "yydebug=")
    public IRubyObject yydebug_set(ThreadContext context, IRubyObject arg0) {
        return null;
    }
    
    private String sourceAsString(ThreadContext context, IRubyObject src) {
        // FIXME: WTF...respondsTo is true? for a string
        System.out.println("RESPONDS_TO: " + src + "SC:"+ src.getClass()  +", RS: " + src.respondsTo("gets"));
/*        if (!src.respondsTo("gets"))*/ return src.convertToString().asJavaString();

/*        return src.callMethod(context, "gets").asJavaString();*/
    }
    
    private IRubyObject filenameAsString(ThreadContext context, IRubyObject filename) {
        if (filename == null || filename.isNil()) return context.runtime.newString("(ripper)");
        
        return filename.convertToString();
    }
    
    private int lineAsInt(ThreadContext context, IRubyObject line) {
        if (line == null || line.isNil()) return 0;
        
        return RubyNumeric.fix2int(line.convertToInteger());
    }
    
    private RipperParser parser = null;
    private IRubyObject filename = null;
}
