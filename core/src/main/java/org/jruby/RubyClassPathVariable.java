/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
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
 * use your version of this file under the terms of the EPL, indicate your
 * decision by deleting the provisions above and replace them with the notice
 * and other provisions required by the GPL or the LGPL. If you do not delete
 * the provisions above, a recipient may use your version of this file under
 * the terms of any one of the EPL, the GPL or the LGPL.
 ***** END LICENSE BLOCK *****/

package org.jruby;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import org.jruby.anno.JRubyMethod;
import org.jruby.internal.runtime.GlobalVariable;

import org.jruby.runtime.Block;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import static org.jruby.api.Convert.asFixnum;
import static org.jruby.api.Create.newString;
import static org.jruby.api.Error.argumentError;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public class RubyClassPathVariable extends RubyObject {
    public static void createClassPathVariable(ThreadContext context, RubyModule Enumerable) {
        RubyClassPathVariable self = new RubyClassPathVariable(context.runtime);
        Enumerable.extend_object(context, self);
        context.runtime.defineReadonlyVariable("$CLASSPATH", self, GlobalVariable.Scope.GLOBAL);
        self.getMetaClass().defineMethods(context, RubyClassPathVariable.class);
    }

    private RubyClassPathVariable(Ruby runtime) {
        super(runtime, runtime.getObject());
    }
    
    @Deprecated
    public IRubyObject append(IRubyObject obj) {
        return append(obj.getRuntime().getCurrentContext(), obj);
    }

    @JRubyMethod(name = {"append", "<<"})
    public IRubyObject append(ThreadContext context, IRubyObject obj) {
        IRubyObject[] paths;
        if (obj.respondsTo("to_a")) {
            paths = ((RubyArray<?>) obj.callMethod(context, "to_a")).toJavaArrayMaybeUnsafe();
        } else {
            paths = new IRubyObject[] { obj };
        }

        for (IRubyObject path: paths) {
            try {
                URL url = getURL(path.convertToString().toString());
                if (url.getProtocol().equals("file")) {
                    path = RubyFile.expand_path(context, null, path);
                    url = getURL(path.convertToString().toString());
                }
                context.runtime.getJRubyClassLoader().addURL(url);
            } catch (MalformedURLException mue) {
                throw argumentError(context, mue.getLocalizedMessage());
            }
        }
        return this;
    }

    private URL getURL(String target) throws MalformedURLException {
        try {
            // First try assuming a protocol is included
            return new URL(target);
        } catch (MalformedURLException e) {
            // Assume file: protocol
            File f = new File(target);
            String path = target;
            if (f.exists() && f.isDirectory() && !path.endsWith("/")) {
                // URLClassLoader requires that directories end with slashes
                path = path + '/';
            }
            return new URL("file", null, path);
        }
    }

    @JRubyMethod(name = {"size", "length"})
    public IRubyObject size(ThreadContext context) {
        return asFixnum(context, context.runtime.getJRubyClassLoader().getURLs().length);
    }
    public IRubyObject size() {
        return size(getRuntime().getCurrentContext());
    }

    @JRubyMethod
    public IRubyObject each(Block block) {
        final ThreadContext context = getRuntime().getCurrentContext();
        URL[] urls = context.runtime.getJRubyClassLoader().getURLs();
        for (URL url: urls) {
            block.yield(context, newString(context, url.toString()));
        }
        return context.nil;
    }

    @Override
    @JRubyMethod
    public IRubyObject to_s(ThreadContext context) {
        return callMethod(context, "to_a").callMethod(context, "to_s");
    }

    @Override
    @JRubyMethod(name = "inspect")
    public IRubyObject inspect(ThreadContext context) {
        return callMethod(context, "to_a").callMethod(context, "inspect");
    }
}// RubyClassPathVariable
