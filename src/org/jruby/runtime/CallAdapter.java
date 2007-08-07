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
 * Copyright (C) 2007 Charles O Nutter <headius@headius.com>
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

package org.jruby.runtime;

import org.jruby.RubyClass;
import org.jruby.RubyModule;
import org.jruby.RubyObject;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.MethodCache;

/**
 *
 * @author headius
 */
public abstract class CallAdapter {
    protected final int methodID;
    protected final String methodName;
    protected final CallType callType;
    
    public CallAdapter(int methodID, String methodName, CallType callType) {
        this.methodID = methodID;
        this.methodName = methodName;
        this.callType = callType;
    }
    
    // no block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args);
    // with block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    
    public static class DefaultCallAdapter extends CallAdapter implements CacheMap.CacheSite {
        byte[] cachedTable;
        DynamicMethod cachedMethod;
        RubyClass cachedType;
        
        public DefaultCallAdapter(int methodID, String methodName, CallType callType) {
            super(methodID, methodName, callType);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self) {
            IRubyObject[] args = IRubyObject.NULL_ARRAY;
            Block block = Block.NULL_BLOCK;
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1) {
            IRubyObject[] args = new IRubyObject[] {arg1};
            Block block = Block.NULL_BLOCK;
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            IRubyObject[] args = new IRubyObject[] {arg1,arg2};
            Block block = Block.NULL_BLOCK;
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            IRubyObject[] args = new IRubyObject[] {arg1,arg2,arg3};
            Block block = Block.NULL_BLOCK;
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args) {
            Block block = Block.NULL_BLOCK;
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, Block block) {
            IRubyObject[] args = IRubyObject.NULL_ARRAY;
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block) {
            IRubyObject[] args = new IRubyObject[] {arg1};
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            IRubyObject[] args = new IRubyObject[] {arg1,arg2};
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            IRubyObject[] args = new IRubyObject[] {arg1,arg2,arg3};
            
            return call(context, self, args, block);
        }

        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            try {
                RubyClass selfType = self.getMetaClass();
                
                DynamicMethod cMethod = this.cachedMethod;
                RubyModule cType = this.cachedType;
                
                if (cType == selfType) {
                    if (cachedTable.length > methodID && cachedTable[methodID] != 0) {
                        return selfType.getDispatcher().callMethod(context, self, selfType, methodID, methodName, args, callType, block);
                    } else {
                        return cMethod.call(context, self, selfType, methodName, args, block);
                    }
                }
                
                byte[] table = selfType.getDispatcher().switchTable;
                if (table.length > methodID && table[methodID] != 0) {
                    cachedTable = table;
                    cachedType = selfType;
                    return selfType.getDispatcher().callMethod(context, self, selfType, methodID, methodName, args, callType, block);
                }
                DynamicMethod method = null;
                method = selfType.searchMethod(methodName);


                if (method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(context.getFrameSelf(), callType))) {
                    return RubyObject.callMethodMissing(context, self, method, methodName, args, context.getFrameSelf(), callType, block);
                }

                cachedMethod = method;
                cachedType = selfType;
                cachedTable = table;
                
                selfType.getRuntime().getCacheMap().add(method, this);
                
                return method.call(context, self, selfType, methodName, args, block);
            } catch (StackOverflowError soe) {
                throw context.getRuntime().newSystemStackError("stack level too deep");
            }
        }

        public void removeCachedMethod(String name) {
            cachedType = null;
            cachedMethod = null;
        }
    }
}
