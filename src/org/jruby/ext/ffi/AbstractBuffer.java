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
 * Copyright (C) 2008 JRuby project
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

package org.jruby.ext.ffi;

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.anno.JRubyClass;
import org.jruby.runtime.ObjectAllocator;

/**
 * A memory buffer to pass data between ruby and native code.
 */
@JRubyClass(name=AbstractBuffer.ABSTRACT_BUFFER_RUBY_CLASS, parent="AbstractMemory")
abstract public class AbstractBuffer extends AbstractMemory {
    public final static String ABSTRACT_BUFFER_RUBY_CLASS = "AbstractBuffer";

    public static RubyClass createBufferClass(Ruby runtime) {
        RubyModule module = FFIProvider.getModule(runtime);
        RubyClass result = module.defineClassUnder(ABSTRACT_BUFFER_RUBY_CLASS,
                module.getClass(AbstractMemory.ABSTRACT_MEMORY_RUBY_CLASS),
                ObjectAllocator.NOT_ALLOCATABLE_ALLOCATOR);
        
        result.defineAnnotatedMethods(AbstractBuffer.class);
        result.defineAnnotatedConstants(AbstractBuffer.class);

        return result;
    }
    protected AbstractBuffer(Ruby runtime, RubyClass klass, long offset, long size) {
        super(runtime, klass, offset, size);
    }
}
