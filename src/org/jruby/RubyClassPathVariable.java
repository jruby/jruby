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

import java.net.URL;

import org.jruby.runtime.Block;
import org.jruby.runtime.CallbackFactory;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyClassPathVariable extends RubyObject {
    public static void createClassPathVariable(Ruby runtime) {
        RubyClassPathVariable self = new RubyClassPathVariable(runtime);
        runtime.getModule("Enumerable").extend_object(self);
        runtime.defineReadonlyVariable("$CLASSPATH", self);
        
        CallbackFactory cf = runtime.callbackFactory(RubyClassPathVariable.class);

        self.getMetaClass().defineFastMethod("<<", cf.getFastMethod("append", IRubyObject.class));
        self.getMetaClass().defineFastMethod("append", cf.getFastMethod("append", IRubyObject.class));
        self.getMetaClass().defineFastMethod("size", cf.getFastMethod("size"));
        self.getMetaClass().defineFastMethod("length", cf.getFastMethod("size"));
        self.getMetaClass().defineMethod("each", cf.getMethod("each"));
        self.getMetaClass().defineFastMethod("to_s", cf.getFastMethod("to_s"));
        self.getMetaClass().defineFastMethod("inspect", cf.getFastMethod("inspect"));
    }

    private RubyClassPathVariable(Ruby runtime) {
        super(runtime, runtime.getObject());
    }

    public IRubyObject append(IRubyObject obj) throws Exception {
        String ss = obj.convertToString().toString();
        if(ss.indexOf("://") == -1) {
            ss = "file://" + ss;
        }
        getRuntime().getJRubyClassLoader().addURL(new URL(ss));
        return this;
    }

    public IRubyObject size() {
        return getRuntime().newFixnum(getRuntime().getJRubyClassLoader().getURLs().length);
    }

    public IRubyObject each(Block block) {
        URL[] urls = getRuntime().getJRubyClassLoader().getURLs();
        ThreadContext ctx = getRuntime().getCurrentContext();
        for(int i=0,j=urls.length;i<j;i++) {
            block.yield(ctx, getRuntime().newString(urls[i].toString()));
        }
        return getRuntime().getNil();
    }

    public IRubyObject to_s() {
        return callMethod(getRuntime().getCurrentContext(), "to_a").callMethod(getRuntime().getCurrentContext(), "to_s");
    }    

    public IRubyObject inspect() {
        return callMethod(getRuntime().getCurrentContext(), "to_a").callMethod(getRuntime().getCurrentContext(), "inspect");
    }    
}// RubyClassPathVariable
