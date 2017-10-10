/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v10.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2014 Timur Duehr <tduehr@gmail.com>
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

package org.jruby.ext.ffi;

import org.jruby.anno.JRubyClass;
import org.jruby.anno.JRubyMethod;
import org.jruby.ext.ffi.Enum;
import org.jruby.Ruby;
import org.jruby.RubyArray;
import org.jruby.RubyBoolean;
import org.jruby.RubyClass;
import org.jruby.RubyHash;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.RubySymbol;
import org.jruby.runtime.Block;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.ObjectAllocator;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.Visibility;

import java.util.Iterator;

/**
 * Represents a C enum
 */
@JRubyClass(name="FFI::Enums", parent="Object")
public final class Enums extends RubyObject {
    private final RubyArray allEnums;
    private final RubyHash symbolMap;
    private final RubyHash taggedEnums;
        
    public static RubyClass createEnumsClass(Ruby runtime, RubyModule ffiModule) {
        RubyClass enumsClass = ffiModule.defineClassUnder("Enums", runtime.getObject(),
                Allocator.INSTANCE);
        enumsClass.defineAnnotatedMethods(Enums.class);
        enumsClass.defineAnnotatedConstants(Enums.class);
        enumsClass.includeModule(ffiModule.getConstant("DataConverter"));

        return enumsClass;
    }

    private static final class Allocator implements ObjectAllocator {
        private static final ObjectAllocator INSTANCE = new Allocator();

        public final IRubyObject allocate(Ruby runtime, RubyClass klass) {
            return new Enums(runtime, klass);
        }
    }

    private Enums(Ruby runtime, RubyClass klass) {
        super(runtime, klass);
        allEnums    = RubyArray.newArray(runtime);
        taggedEnums = RubyHash.newHash(runtime);
        symbolMap   = RubyHash.newHash(runtime);
    }

    @JRubyMethod(name = "initialize", visibility = Visibility.PRIVATE)
    public final IRubyObject initialize(ThreadContext context) {
        return (IRubyObject) this;
    }

    @JRubyMethod(name = "<<")
    public IRubyObject append(final ThreadContext context, IRubyObject item){
        if(!(item instanceof Enum)){
            throw context.runtime.newTypeError(item, context.runtime.getFFI().ffiModule.getClass("Enum"));
        }
        allEnums.append(item);
        if (!(item == null || item == context.nil)){
            IRubyObject tag = ((Enum)item).tag(context);
            if (tag != null && !tag.isNil())
                taggedEnums.fastASet(tag, item);
        }
        symbolMap.merge_bang(context, ((Enum)item).symbol_map(context), Block.NULL_BLOCK);
        return item;
    }

    public boolean isEmpty(){
        return ( allEnums.isEmpty() && symbolMap.isEmpty() && taggedEnums.isEmpty());
    }

    @JRubyMethod(name = "empty?")
    public RubyBoolean empty_p(){
        return isEmpty() ?  getRuntime().getTrue() : getRuntime().getFalse();
    }

    @JRubyMethod(name = "find")
    public IRubyObject find(final ThreadContext context, IRubyObject query){
        if (taggedEnums.has_key_p(context, query).isTrue()){
            return taggedEnums.fastARef(query);
        }
        for (int i = 0; i < allEnums.getLength(); i++){
            IRubyObject item = (IRubyObject)allEnums.entry(i);
            if (((RubyArray)item.callMethod(context, "symbols")).include_p(context, query).isTrue()){
                return item;
            }
        }
        return context.runtime.getNil();
    }

    @JRubyMethod(name = "__map_symbol")
    public IRubyObject mapSymbol(final ThreadContext context, IRubyObject symbol){
        return symbolMap.op_aref(context, symbol);
    }
}
