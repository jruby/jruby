package org.jruby.util;

import org.jruby.Ruby;
import org.jruby.RubyHash;
import org.jruby.RubySymbol;
import org.jruby.exceptions.Unrescuable;
import org.jruby.runtime.Block;
import org.jruby.runtime.Helpers;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

/**
 * Deprecated MRI-style recursion guard logic pulled out of {@link Ruby}.
 */
public class MRIRecursionGuard {
    private final Ruby runtime;
    private final RubySymbol recursiveKey;
    private final ThreadLocal<Map<String, RubyHash>> recursive = new ThreadLocal<Map<String, RubyHash>>();
    private final ThreadLocal<Boolean> inRecursiveListOperation = new ThreadLocal<>();

    @Deprecated
    public MRIRecursionGuard(Ruby runtime) {
        this.runtime = runtime;
        this.recursiveKey = runtime.newSymbol("__recursive_key__");
    }

    @Deprecated
    public interface RecursiveFunction  {
        IRubyObject call(IRubyObject obj, boolean recur);
    }

    @Deprecated
    public IRubyObject execRecursive(RecursiveFunction func, IRubyObject obj) {
        if (!inRecursiveListOperation.get()) {
            throw runtime.newThreadError("BUG: execRecursive called outside recursiveListOperation");
        }
        return execRecursiveInternal(func, obj, null, false);
    }

    @Deprecated
    public IRubyObject execRecursiveOuter(RecursiveFunction func, IRubyObject obj) {
        try {
            return execRecursiveInternal(func, obj, null, true);
        } finally {
            recursiveListClear();
        }
    }

    @Deprecated
    public <T extends IRubyObject> T recursiveListOperation(Callable<T> body) {
        try {
            inRecursiveListOperation.set(true);
            return body.call();
        } catch (Exception e) {
            Helpers.throwException(e);
            return null; // not reached
        } finally {
            recursiveListClear();
            inRecursiveListOperation.set(false);
        }
    }

    // exec_recursive
    @Deprecated
    private IRubyObject execRecursiveInternal(RecursiveFunction func, IRubyObject obj, IRubyObject pairid, boolean outer) {
        ExecRecursiveParams p = new ExecRecursiveParams();
        p.list = recursiveListAccess();
        p.objid = obj.id();
        boolean outermost = outer && !recursiveCheck(p.list, recursiveKey, null);
        if(recursiveCheck(p.list, p.objid, pairid)) {
            if(outer && !outermost) {
                throw new RecursiveError(p.list);
            }
            return func.call(obj, true);
        } else {
            IRubyObject result;
            p.func = func;
            p.obj = obj;
            p.pairid = pairid;

            if(outermost) {
                recursivePush(p.list, recursiveKey, null);
                try {
                    result = execRecursiveI(p);
                } catch(RecursiveError e) {
                    if(e.tag != p.list) {
                        throw e;
                    } else {
                        result = p.list;
                    }
                }
                recursivePop(p.list, recursiveKey, null);
                if(result == p.list) {
                    result = func.call(obj, true);
                }
            } else {
                result = execRecursiveI(p);
            }

            return result;
        }
    }

    // exec_recursive_i
    private IRubyObject execRecursiveI(ExecRecursiveParams p) {
        IRubyObject result = null;
        recursivePush(p.list, p.objid, p.pairid);
        try {
            result = p.func.call(p.obj, false);
        } finally {
            recursivePop(p.list, p.objid, p.pairid);
        }
        return result;
    }

    private IRubyObject recursiveListAccess() {
        Map<String, RubyHash> hash = recursive.get();
        String sym = runtime.getCurrentContext().getFrameName();
        IRubyObject list = runtime.getNil();
        if(hash == null) {
            hash = new HashMap<>();
            recursive.set(hash);
        } else {
            list = hash.get(sym);
        }
        if(list == null || list.isNil()) {
            list = RubyHash.newHash(runtime);
            hash.put(sym, (RubyHash)list);
        }
        return list;
    }

    private void recursiveListClear() {
        Map<String, RubyHash> hash = recursive.get();
        if(hash != null) {
            hash.clear();
        }
    }

    private void recursivePush(IRubyObject list, IRubyObject obj, IRubyObject paired_obj) {
        IRubyObject pair_list;
        Ruby runtime = this.runtime;
        final ThreadContext context = runtime.getCurrentContext();
        if (paired_obj == null) {
            ((RubyHash) list).op_aset(context, obj, runtime.getTrue());
        } else if ((pair_list = ((RubyHash)list).fastARef(obj)) == null) {
            ((RubyHash) list).op_aset(context, obj, paired_obj);
        } else {
            if (!(pair_list instanceof RubyHash)) {
                IRubyObject other_paired_obj = pair_list;
                pair_list = RubyHash.newHash(runtime);
                ((RubyHash) pair_list).op_aset(context, other_paired_obj, runtime.getTrue());
                ((RubyHash) list).op_aset(context, obj, pair_list);
            }
            ((RubyHash)pair_list).op_aset(context, paired_obj, runtime.getTrue());
        }
    }

    private void recursivePop(IRubyObject list, IRubyObject obj, IRubyObject paired_obj) {
        if(paired_obj != null) {
            IRubyObject pair_list = ((RubyHash)list).fastARef(obj);
            if(pair_list == null) {
                throw runtime.newTypeError("invalid inspect_tbl pair_list for " + runtime.getCurrentContext().getFrameName());
            }
            if(pair_list instanceof RubyHash) {
                ((RubyHash)pair_list).delete(runtime.getCurrentContext(), paired_obj, Block.NULL_BLOCK);
                if(!((RubyHash)pair_list).isEmpty()) {
                    return;
                }
            }
        }
        ((RubyHash)list).delete(runtime.getCurrentContext(), obj, Block.NULL_BLOCK);
    }

    private boolean recursiveCheck(IRubyObject list, IRubyObject obj_id, IRubyObject paired_obj_id) {
        IRubyObject pair_list = ((RubyHash)list).fastARef(obj_id);
        if(pair_list == null) {
            return false;
        }
        if(paired_obj_id != null) {
            if(!(pair_list instanceof RubyHash)) {
                if(pair_list != paired_obj_id) {
                    return false;
                }
            } else {
                IRubyObject paired_result = ((RubyHash)pair_list).fastARef(paired_obj_id);
                if(paired_result == null || paired_result.isNil()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static class RecursiveError extends Error implements Unrescuable {
        public RecursiveError(Object tag) {
            this.tag = tag;
        }
        public final Object tag;

        @Override
        public synchronized Throwable fillInStackTrace() {
            return this;
        }
    }

    private static class ExecRecursiveParams {
        public ExecRecursiveParams() {}
        public RecursiveFunction func;
        public IRubyObject list;
        public IRubyObject obj;
        public IRubyObject objid;
        public IRubyObject pairid;
    }
}
