/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 1.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2010 Charles O Nutter <headius@headius.com>
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
package org.jruby.ext.psych;

import org.jcodings.Encoding;
import org.jcodings.specific.UTF16BEEncoding;
import org.jcodings.specific.UTF16LEEncoding;
import org.jcodings.specific.UTF8Encoding;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyModule;
import org.jruby.RubyString;
import org.jruby.internal.runtime.methods.JavaMethod.JavaMethodZero;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.load.Library;

public class PsychLibrary implements Library {
    public void load(final Ruby runtime, boolean wrap) {
        RubyModule psych = runtime.defineModule("Psych");
        
        RubyString version = runtime.newString("0.1.4");
        version.setFrozen(true);
        
        final RubyArray versionElements = runtime.newArray(runtime.newFixnum(0), runtime.newFixnum(1), runtime.newFixnum(4));
        versionElements.setFrozen(true);
        
        psych.setConstant("LIBYAML_VERSION", runtime.newString("0.1.4"));
        psych.getSingletonClass().addMethod("libyaml_version", new JavaMethodZero(psych, Visibility.PUBLIC) {
            @Override
            public IRubyObject call(ThreadContext context, IRubyObject self, RubyModule clazz, String name) {
                return versionElements;
            }
        });
        
        PsychParser.initPsychParser(runtime, psych);
        PsychEmitter.initPsychEmitter(runtime, psych);
        PsychToRuby.initPsychToRuby(runtime, psych);
        PsychYamlTree.initPsychYamlTree(runtime, psych);
    }

    public enum YAMLEncoding {
        YAML_ANY_ENCODING(UTF8Encoding.INSTANCE),
        YAML_UTF8_ENCODING(UTF8Encoding.INSTANCE),
        YAML_UTF16LE_ENCODING(UTF16LEEncoding.INSTANCE),
        YAML_UTF16BE_ENCODING(UTF16BEEncoding.INSTANCE);

        YAMLEncoding(Encoding encoding) {
            this.encoding = encoding;
        }

        public final Encoding encoding;
    }
}
