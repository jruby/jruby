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
package org.jvyamlb.tokens;

/**
 * @author <a href="mailto:ola.bini@ki.se">Ola Bini</a>
 */
public abstract class Token {
    public final static Token DOCUMENT_START = new DocumentStartToken();
    public final static Token DOCUMENT_END = new DocumentEndToken();
    public final static Token BLOCK_MAPPING_START = new BlockMappingStartToken();
    public final static Token BLOCK_SEQUENCE_START = new BlockSequenceStartToken();
    public final static Token BLOCK_ENTRY = new BlockEntryToken();
    public final static Token BLOCK_END = new BlockEndToken();
    public final static Token FLOW_ENTRY = new FlowEntryToken();
    public final static Token FLOW_MAPPING_END = new FlowMappingEndToken();
    public final static Token FLOW_MAPPING_START = new FlowMappingStartToken();
    public final static Token FLOW_SEQUENCE_END = new FlowSequenceEndToken();
    public final static Token FLOW_SEQUENCE_START = new FlowSequenceStartToken();
    public final static Token KEY = new KeyToken();
    public final static Token VALUE = new ValueToken();
    public final static Token STREAM_END = new StreamEndToken();
    public final static Token STREAM_START = new StreamStartToken();

    public Token() {
    }

    public void setValue(final Object value) {
    }

    public String toString() {
        return "#<" + this.getClass().getName() + ">";
    }
}// Token
