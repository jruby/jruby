/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2002-2004 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

package org.jruby.javasupport.test;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

import junit.framework.TestCase;

import org.jruby.Ruby;
import org.jruby.RubyKernel;
import org.jruby.internal.runtime.GlobalVariable;
import org.jruby.javasupport.JavaUtil;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.NormalizedFile;

public class RubyTestCase extends TestCase {
    private static final IRubyObject[] EMPTY_ARRAY = IRubyObject.NULL_ARRAY;

    public RubyTestCase(String name) {
        super(name);
    }

    protected Ruby createRuby(URL url) throws IOException {
        if (url == null) {
            throw new NullPointerException("url was null");
        }
        InputStream in = url.openStream();
        NormalizedFile f = (NormalizedFile)NormalizedFile.createTempFile("rtc", ".rb");
        FileOutputStream out = new FileOutputStream(f);

        int length;
        byte[] buf = new byte[8096];
        while ((length = in.read(buf, 0, buf.length)) >= 0) {
            out.write(buf, 0, length);
        }
        in.close();
        out.close();

        String filePath = f.getAbsolutePath();
        Ruby runtime = Ruby.newInstance();
        initRuby(runtime);
        RubyKernel.require(runtime.getTopSelf(), runtime.newString(filePath), null);
        f.delete();
        return runtime;
    }

    // Is there something built into JRuby to do this?
    protected void initRuby(Ruby runtime) {
        IRubyObject empty =
            JavaUtil.convertJavaToRuby(
                runtime,
                EMPTY_ARRAY,
                EMPTY_ARRAY.getClass());

        runtime.defineReadonlyVariable("$-p", runtime.getNil(), GlobalVariable.Scope.GLOBAL);
        runtime.defineReadonlyVariable("$-n", runtime.getNil(), GlobalVariable.Scope.GLOBAL);
        runtime.defineReadonlyVariable("$-a", runtime.getNil(), GlobalVariable.Scope.GLOBAL);
        runtime.defineReadonlyVariable("$-l", runtime.getNil(), GlobalVariable.Scope.GLOBAL);
        runtime.defineReadonlyVariable("$\"", empty, GlobalVariable.Scope.GLOBAL);
        runtime.defineReadonlyVariable("$*", empty, GlobalVariable.Scope.GLOBAL);
        runtime.defineReadonlyVariable("$:", empty, GlobalVariable.Scope.GLOBAL);
        runtime.getObject().defineConstant(runtime.getCurrentContext(), "ARGV", empty);
    }
}

