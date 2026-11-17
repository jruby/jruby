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

import org.jruby.anno.JRubyClass;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.EncodingError;
import org.jruby.runtime.ThreadContext;

import static org.jruby.api.Define.defineClass;

/**
 * The Java representation of a Ruby EncodingError.
 *
 * @see EncodingError
 */
@JRubyClass(name="EncodingError", parent="StandardError")
public class RubyEncodingError extends RubyStandardError {
    protected RubyEncodingError(Ruby runtime, RubyClass exceptionClass) {
        super(runtime, exceptionClass);
    }

    static RubyClass define(ThreadContext context, RubyClass StandardError) {
        return defineClass(context, "EncodingError", StandardError, RubyEncodingError::new);
    }

    protected RaiseException constructThrowable(String message) {
        return new EncodingError(message, this);
    }

    @JRubyClass(name="CompatibilityError", parent="EncodingError")
    public static class RubyCompatibilityError extends RubyEncodingError {
        protected RubyCompatibilityError(Ruby runtime, RubyClass exceptionClass) {
            super(runtime, exceptionClass);
        }

        static RubyClass define(ThreadContext context, RubyClass EncodingError, RubyModule Encoding) {
            return Encoding.defineClassUnder(context, "CompatibilityError", EncodingError, RubyCompatibilityError::new);
        }

        protected RaiseException constructThrowable(String message) {
            return new EncodingError.CompatibilityError(message, this);
        }
    }

    @JRubyClass(name="InvalidByteSequenceError", parent="EncodingError")
    public static class RubyInvalidByteSequenceError extends RubyEncodingError {
        protected RubyInvalidByteSequenceError(Ruby runtime, RubyClass exceptionClass) {
            super(runtime, exceptionClass);
        }

        static RubyClass define(ThreadContext context, RubyClass EncodingError, RubyModule Encoding) {
            return Encoding.defineClassUnder(context, "InvalidByteSequenceError", EncodingError, RubyInvalidByteSequenceError::new).
                    defineMethods(context, RubyConverter.EncodingErrorMethods.class, RubyConverter.InvalidByteSequenceErrorMethods.class);
        }

        protected RaiseException constructThrowable(String message) {
            return new EncodingError.InvalidByteSequenceError(message, this);
        }
    }

    @JRubyClass(name="UndefinedConversionError", parent="EncodingError")
    public static class RubyUndefinedConversionError extends RubyEncodingError {
        protected RubyUndefinedConversionError(Ruby runtime, RubyClass exceptionClass) {
            super(runtime, exceptionClass);
        }

        static RubyClass define(ThreadContext context, RubyClass EncodingError, RubyModule Encoding) {
            return Encoding.defineClassUnder(context, "UndefinedConversionError", EncodingError, RubyUndefinedConversionError::new).
                    defineMethods(context, RubyConverter.EncodingErrorMethods.class, RubyConverter.UndefinedConversionErrorMethods.class);
        }

        protected RaiseException constructThrowable(String message) {
            return new EncodingError.UndefinedConversionError(message, this);
        }
    }

    @JRubyClass(name="ConverterNotFoundError", parent="EncodingError")
    public static class RubyConverterNotFoundError extends RubyEncodingError {
        protected RubyConverterNotFoundError(Ruby runtime, RubyClass exceptionClass) {
            super(runtime, exceptionClass);
        }

        static RubyClass define(ThreadContext context, RubyClass EncodingError, RubyModule Encoding) {
            return Encoding.defineClassUnder(context, "ConverterNotFoundError", EncodingError, RubyConverterNotFoundError::new);
        }

        protected RaiseException constructThrowable(String message) {
            return new EncodingError.ConverterNotFoundError(message, this);
        }
    }
}
