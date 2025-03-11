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
 * Copyright (C) 2007 Charles Oliver Nutter <headius@headius.com>
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

package org.jruby.runtime;

import java.io.IOException;
import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.runtime.marshal.Dumper;
import org.jruby.runtime.marshal.UnmarshalStream;
import org.jruby.util.io.RubyOutputStream;

import static org.jruby.api.Error.typeError;

/**
 *
 * @author headius
 */
public interface ObjectMarshal<T> {
    ObjectMarshal NOT_MARSHALABLE_MARSHAL = new ObjectMarshal() {
        @Deprecated(since = "10.0", forRemoval = true)
        @SuppressWarnings("removal")
        public void marshalTo(Ruby runtime, Object obj, RubyClass type, org.jruby.runtime.marshal.MarshalStream marshalStream) {
            var context = runtime.getCurrentContext();
            throw typeError(context, "no marshal_dump is defined for class " + type.getName(context));
        }

        public void marshalTo(ThreadContext context, RubyOutputStream out, Object obj, RubyClass type, Dumper marshalStream) {
            throw typeError(context, "no marshal_dump is defined for class " + type.getName(context));
        }

        public Object unmarshalFrom(Ruby runtime, RubyClass type, UnmarshalStream unmarshalStream) {
            var context = runtime.getCurrentContext();
            throw typeError(context, "no marshal_load is defined for class " + type.getName(context));
        }
    };

    @Deprecated(since = "10.0", forRemoval = true)
    @SuppressWarnings("removal")
    void marshalTo(Ruby runtime, T obj, RubyClass type, org.jruby.runtime.marshal.MarshalStream marshalStream) throws IOException;
    void marshalTo(ThreadContext context, RubyOutputStream out, T obj, RubyClass type, Dumper marshalStream);
    T unmarshalFrom(Ruby runtime, RubyClass type, UnmarshalStream unmarshalStream) throws IOException;
}
