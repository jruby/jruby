/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2010 Hiroshi Nakamura <nahi@ruby-lang.org>
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
package org.jruby.util.io;

import java.io.FileNotFoundException;

/**
 * Signals that an attempt to open the file denoted by a specified pathname
 * has failed by 'Permission Denied'.
 *
 * <p>This exception might be thrown by the {@link ChannelDescriptor#open}
 * when trying to create new file and the specified pathname cannot be written.
 * Bear in mind that {@link ChannelDescriptor#open} throws
 * not PermissionDeniedException but FileNotFindException as same as Java
 * manner when trying to read existing but unreadable file.
 * See org.jruby.RubyFile#fopen and sysopen how we handle that situation.</p>
 */
public class PermissionDeniedException extends FileNotFoundException {

    /**
     * Constructs a PermissionDeniedException with null as its error detail
     * message.
     */
    public PermissionDeniedException() {
        super();
    }

    /**
     * Constructs a PermissionDeniedException with the specified detail
     * message.  The string msg can be retrieved later by the
     * {@link java.lang.Throwable#getMessage} method of class
     * java.lang.Throwable.
     *
     * @param msg the detail message.
     */
    public PermissionDeniedException(String msg) {
        super(msg);
    }
}
