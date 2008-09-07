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
import org.jruby.RubyFixnum;
import org.jruby.exceptions.JumpException;
import org.jruby.exceptions.JumpException.BreakJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.internal.runtime.methods.DynamicMethod;
import org.jruby.javasupport.util.RuntimeHelpers;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 */
public abstract class CallSite {
    public final String methodName;
    protected final CallType callType;
    
    public CallSite(String methodName, CallType callType) {
        this.methodName = methodName;
        this.callType = callType;
    }
    
    // no block
    public abstract IRubyObject call(ThreadContext context, IRubyObject self);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject... args);
    // with block pass
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    // with block literal (iter)
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject self, Block block);
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block);
    
    // no block
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args);
    // with block pass
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, Block block);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block);
    // with block literal (iter)
    public abstract IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, Block block);
    public abstract IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block);
    public abstract IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block);
    public abstract IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block);
    public abstract IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block);

    public static class InlineCachingCallSite extends CallSite implements CacheMap.CacheSite {
        private static class CacheEntry {
            /** The cached method */
            public final DynamicMethod cachedMethod;
            /** The cached "real" type from the original method lookup */
            public final RubyClass cachedType;
            /** The original name for which this cache was populated */
            public final String methodName;
            
            public CacheEntry(DynamicMethod method, RubyClass type, String name) {
                cachedMethod = method;
                cachedType = type;
                methodName = name;
            }
            
            /**
             * Check whether this site's cached type matches the incoming
             * type closely enough to allow using the cached method as-is.
             * 
             * The primary check performed here is whether the incoming type
             * and the cached type are exactly the same object. This is the
             * fast path. If that fails, an additional set of checks are
             * performed in case the incoming type is a singleton, which extends
             * but may not mask the method in question on the cached type.
             * 
             * @see CacheEntry#typeGoodEnough
             * @param otherType
             * @return
             */
            public boolean typeOk(RubyClass incomingType) {
                RubyClass cachedType = this.cachedType;
                return cachedType == incomingType || typeGoodEnough(cachedType, incomingType);
            }
            
            /**
             * Check whether an incoming type is similar enough to the
             * original cached type to allow using the cached method.
             * 
             * The logic here is as follows.
             * 
             * If the cached method...
             * <ul>
             * <li>Was not defined immediately on the cached type</li>
             * </ul>
             * 
             * ...and the incoming type...
             * <ul>
             * <li>Immediately extends the same class as the cached type</li>
             * <li>Has not itself overridden the method in question</li>
             * <li>Has not included a module (which may have overridden) the
             * method (caught by the superclass check)</li>
             * </ul>
             * 
             * ...then it is safe to assume that the incoming type can be
             * treated as though it were the cached type for this invocation.
             * 
             * @param thisType The cached type
             * @param otherType The incoming type
             * @return Whether the singleton type is similar enough to permit
             * using the cached method.
             */
            private final boolean typeGoodEnough(RubyClass cachedType, RubyClass incomingType) {
                return cachedType != null &&
                        cachedMethod.getImplementationClass() != cachedType &&
                        incomingType.getSuperClass() == cachedType.getSuperClass() &&
                        incomingType.retrieveMethod(methodName) == null;
            }
        }
        
        private static final CacheEntry NULL_CACHE = new CacheEntry(null, null, null);
        
        private volatile CacheEntry cache = NULL_CACHE;
        private int misses = 0;
        private static final int MAX_MISSES = 50;
        
        public static volatile int totalCallSites;
        public static volatile int failedCallSites;
        
        public InlineCachingCallSite(String methodName, CallType callType) {
            super(methodName, callType);
            totalCallSites++;
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject... args) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, args);
            }
            return cacheAndCall(selfType, args, context, self);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, args, block);
                }
                return cacheAndCall(selfType, block, args, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject[] args, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, args, block);
                }
                return cacheAndCall(selfType, block, args, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName);
            }
            return cacheAndCall(selfType, context, self);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, block);
                }
                return cacheAndCall(selfType, block, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIter(ThreadContext context, IRubyObject self, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, block);
                }
                return cacheAndCall(selfType, block, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1);
            }
            return cacheAndCall(selfType, context, self, arg1);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, block);
                }
                return cacheAndCall(selfType, block, context, self, arg1);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject arg1, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, block);
                }
                return cacheAndCall(selfType, block, context, self, arg1);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2);
            }
            return cacheAndCall(selfType, context, self, arg1, arg2);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, block);
                }
                return cacheAndCall(selfType, block, context, self, arg1, arg2);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, block);
                }
                return cacheAndCall(selfType, block, context, self, arg1, arg2);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3);
            }
            return cacheAndCall(selfType, context, self, arg1, arg2, arg3);
        }
        
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
                }
                return cacheAndCall(selfType, block, context, self, arg1, arg2, arg3);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIter(ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
                }
                return cacheAndCall(selfType, block, context, self, arg1, arg2, arg3);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject... args) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, args);
            }
            return cacheAndCall(caller, selfType, args, context, self);
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, args, block);
                }
                return cacheAndCall(caller, selfType, block, args, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject[] args, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, args, block);
                }
                return cacheAndCall(caller, selfType, block, args, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName);
            }
            return cacheAndCall(caller, selfType, context, self);
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, block);
                }
                return cacheAndCall(caller, selfType, block, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, block);
                }
                return cacheAndCall(caller, selfType, block, context, self);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1);
            }
            return cacheAndCall(caller, selfType, context, self, arg1);
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, block);
                }
                return cacheAndCall(caller, selfType, block, context, self, arg1);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, block);
                }
                return cacheAndCall(caller, selfType, block, context, self, arg1);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2);
            }
            return cacheAndCall(caller, selfType, context, self, arg1, arg2);
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, block);
                }
                return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, block);
                }
                return cacheAndCall(caller, selfType, block, context, self, arg1, arg2);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            RubyClass selfType = pollAndGetClass(context, self);
            CacheEntry myCache = cache;
            if (myCache.typeOk(selfType)) {
                return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3);
            }
            return cacheAndCall(caller, selfType, context, self, arg1, arg2, arg3);
        }
        
        public IRubyObject callFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
                }
                return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            }
        }
        
        public IRubyObject callIterFrom(ThreadContext context, IRubyObject caller, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            RubyClass selfType = pollAndGetClass(context, self);
            try {
                CacheEntry myCache = cache;
                if (myCache.typeOk(selfType)) {
                    return myCache.cachedMethod.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
                }
                return cacheAndCall(caller, selfType, block, context, self, arg1, arg2, arg3);
            } catch (JumpException.BreakJump bj) {
                return handleBreakJump(context, bj);
            } catch (JumpException.RetryJump rj) {
                throw retryJumpError(context);
            } catch (StackOverflowError soe) {
                throw systemStackError(context);
            } finally {
                block.escape();
            }
        }
        
        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, args, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, args, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMising(context, self, method, args);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, args);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, arg);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, arg, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, arg1, arg2);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, arg1, arg2, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2, block);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, arg1, arg2,arg3);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2, arg3);
        }

        protected IRubyObject cacheAndCall(RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, context.getFrameSelf())) {
                return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
        }
        
        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, IRubyObject[] args, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, args, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, args, block);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, IRubyObject[] args, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMising(context, self, method, args);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, args);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, block);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, arg);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, arg, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg, block);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, arg1, arg2);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, arg1, arg2, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2, block);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, arg1, arg2,arg3);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2, arg3);
        }

        protected IRubyObject cacheAndCall(IRubyObject caller, RubyClass selfType, Block block, ThreadContext context, IRubyObject self, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            DynamicMethod method = selfType.searchMethod(methodName);
            if (methodMissing(method, caller)) {
                return callMethodMissing(context, self, method, arg1, arg2, arg3, block);
            }
            updateCacheEntry(method, selfType);
            return method.call(context, self, selfType, methodName, arg1, arg2, arg3, block);
        }

        private IRubyObject callMethodMising(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, args, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, Block block) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, context.getFrameSelf(), callType, block);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject[] args, Block block) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, args, context.getFrameSelf(), callType, block);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg, Block block) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg, context.getFrameSelf(), callType, block);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, Block block) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, context.getFrameSelf(), callType, block);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, arg3, context.getFrameSelf(), callType, Block.NULL_BLOCK);
        }

        private IRubyObject callMethodMissing(ThreadContext context, IRubyObject self, DynamicMethod method, IRubyObject arg1, IRubyObject arg2, IRubyObject arg3, Block block) {
            return RuntimeHelpers.callMethodMissing(context, self, method, methodName, arg1, arg2, arg3, context.getFrameSelf(), callType, block);
        }

        private boolean methodMissing(DynamicMethod method, IRubyObject caller) {
            return method.isUndefined() || (!methodName.equals("method_missing") && !method.isCallableFrom(caller, callType));
        }

        private RubyClass pollAndGetClass(ThreadContext context, IRubyObject self) {
            context.callThreadPoll();

            RubyClass selfType = self.getMetaClass();

            return selfType;
        }
        
        private void updateCacheEntry(DynamicMethod method, RubyClass selfType) {
            if (misses < MAX_MISSES) {
                misses++;
                if (misses >= MAX_MISSES) failedCallSites++;
                cache = new CacheEntry(method, selfType, methodName);
                selfType.getRuntime().getCacheMap().add(method, this);
            }
        }
        
        public void removeCachedMethod() {
            cache = NULL_CACHE;
        }

        private IRubyObject handleBreakJump(ThreadContext context, BreakJump bj) throws BreakJump {
            // consume and rethrow or just keep rethrowing?
            if (context.getFrameJumpTarget() == bj.getTarget()) {
                return (IRubyObject) bj.getValue();
            }
            throw bj;
        }
        
        private RaiseException retryJumpError(ThreadContext context) {
            return context.getRuntime().newLocalJumpError("retry", context.getRuntime().getNil(), "retry outside of rescue not supported");
        }
        
        private RaiseException systemStackError(ThreadContext context) {
            return context.getRuntime().newSystemStackError("stack level too deep");
        }
    }
    
    public static class PlusCallSite extends InlineCachingCallSite {
        public PlusCallSite() {
            super("+", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_plus(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class MinusCallSite extends InlineCachingCallSite {
        public MinusCallSite() {
            super("-", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_minus(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class MulCallSite extends InlineCachingCallSite {
        public MulCallSite() {
            super("*", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_mul(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class DivCallSite extends InlineCachingCallSite {
        public DivCallSite() {
            super("/", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_div(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class LtCallSite extends InlineCachingCallSite {
        public LtCallSite() {
            super("<", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_lt(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class LeCallSite extends InlineCachingCallSite {
        public LeCallSite() {
            super("<=", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_le(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class GtCallSite extends InlineCachingCallSite {
        public GtCallSite() {
            super(">", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_gt(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
    
    public static class GeCallSite extends InlineCachingCallSite {
        public GeCallSite() {
            super(">=", CallType.NORMAL);
        }
        
        @Override
        public IRubyObject call(ThreadContext context, IRubyObject self, IRubyObject arg) {
            if (self instanceof RubyFixnum) {
                return ((RubyFixnum)self).op_ge(context, arg);
            }
            
            return super.call(context, self, arg);
        }
    }
}
