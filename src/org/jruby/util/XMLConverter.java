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
 * Copyright (C) 2013 Sebastien Le Callonnec <sebastien@weblogism.com>
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
package org.jruby.util;

import org.jruby.RubyHash;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;


public final class XMLConverter {
    
    private final static XMLCharacterTranslator XML_TEXT_TRANSLATOR = new XMLTextCharacterTranslator();
    private final static XMLCharacterTranslator XML_ATTR_TRANSLATOR = new XMLAttrCharacterTranslator();
    
    private XMLConverter() {}
    
    public static void checkOptions(ThreadContext context, IRubyObject options) {
        RubyHash hash = (RubyHash)options;
        if (!context.runtime.is1_8()) {
            IRubyObject xmlOption = hash.fastARef(context.runtime.newSymbol("xml"));
            if (xmlOption != null && !(xmlOption.op_equal(context, context.runtime.newSymbol("attr")).isTrue()
                    || xmlOption.op_equal(context, context.runtime.newSymbol("text")).isTrue())) {
                throw context.runtime.newArgumentError("unexpected value for xml option: " + xmlOption.asString());
            }
        }
    }
    
    public static ByteList convert(ThreadContext context, ByteList input, IRubyObject options) {
        checkOptions(context, options);
        
        RubyHash hash = (RubyHash)options;
        IRubyObject xmlOption = hash.fastARef(context.runtime.newSymbol("xml"));
        
        String stringInput = context.runtime.newString(input).asJavaString();
        String stringOutput;
        if (xmlOption.op_equal(context, context.runtime.newSymbol("attr")).isTrue()) {
            stringOutput = XML_ATTR_TRANSLATOR.translate(stringInput);
        } else {
            stringOutput = XML_TEXT_TRANSLATOR.translate(stringInput);
        }
        
        return new ByteList(stringOutput.getBytes());
    }
   
    static interface CharacterTranslator {
        String translate(String input);
    }
    
    static abstract class XMLCharacterTranslator implements CharacterTranslator {
        abstract String[] table();
        
        @Override
        public String translate(String input) {
            String output = input;
            for (int i = 0; i < table().length; i+=2) {
                output = output.replace(table()[i], table()[i+1]);
            }
            return output;
        }
    }
    
    static class XMLTextCharacterTranslator extends XMLCharacterTranslator {
        @Override
        public String[] table() {
            return new String[] { "&", "&amp;", "<", "&lt;", ">", "&gt;" };
        }
    }
    
    static class XMLAttrCharacterTranslator extends XMLTextCharacterTranslator {
        @Override
        public String[] table() {
            return new String[] { "&", "&amp;", "<", "&lt;", ">", "&gt;", "\"", "&quot;" };
        }        

        @Override
        public String translate(String input) {
            return "\"" + super.translate(input) + "\"";
        }
    }    
}
