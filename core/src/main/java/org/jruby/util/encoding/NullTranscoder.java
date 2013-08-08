/*
 **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2013 The JRuby Community (jruby.org)
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
package org.jruby.util.encoding;

import org.jcodings.Encoding;
import org.jcodings.specific.ASCIIEncoding;
import org.jruby.Ruby;
import org.jruby.runtime.ThreadContext;
import org.jruby.util.ByteList;

/**
 * A transcoder that does nothing.
 * 
 * TODO: handle doing decoration logic (XML, crlf, etc)
 */
public class NullTranscoder extends Transcoder {
    public NullTranscoder(ThreadContext context) {
        super(context, ASCIIEncoding.INSTANCE, ASCIIEncoding.INSTANCE);
    }
    @Override
    public ByteList transcode(ThreadContext context, ByteList value) {
        return value.shallowDup();
    }

    @Override
    public ByteList transcode(ThreadContext context, ByteList value, boolean is7BitASCII) {
        return value.shallowDup();
    }

    @Override
    public ByteList convert(ThreadContext context, ByteList value, boolean is7BitASCII) {
        return value.shallowDup();
    }

    @Override
    public ByteList econvStrConvert(ThreadContext context, ByteList value, boolean finish) {
        return value.shallowDup();
    }

    @Override
    public RubyCoderResult primitiveConvert(ThreadContext context, ByteList inBuffer, ByteList outBuffer, int outOffset, int outLimit, Encoding inEncoding, boolean is7BitASCII, int flags) {
        Ruby runtime = context.runtime;
        
        Encoding outEncoding = this.outEncoding != null ? this.outEncoding : inBuffer.getEncoding();
        
        if (outLimit < 0) {
            outBuffer.append(inBuffer);
        } else {
            outBuffer.replace(outOffset, outLimit - outOffset, inBuffer.getUnsafeBytes(), inBuffer.getBegin(), Math.min(inBuffer.getRealSize(), outLimit));
        }
        
        return lastResult = new RubyCoderResult("finished", inEncoding, outEncoding, null, null);
        
        
    }

    @Override
    public ByteList finish() {
        return ByteList.EMPTY_BYTELIST;
    }
    
}
