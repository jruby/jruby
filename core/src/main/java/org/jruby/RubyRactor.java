package org.jruby;

import org.jruby.anno.JRubyMethod;
import org.jruby.api.Convert;
import org.jruby.api.Define;
import org.jruby.api.Error;
import org.jruby.runtime.ThreadContext;
import org.jruby.runtime.builtin.IRubyObject;

import java.util.IdentityHashMap;
import java.util.function.BiFunction;

public class RubyRactor {

    private static final TraverseResult NULL_LEAVE = TraverseResult.CONTINUE;

    public static RubyClass createRactorClass(ThreadContext context, RubyClass Object) {
        return Define
                .defineClass(context, "Ractor", Object, Object.getAllocator())
                .defineMethods(context, RubyRactor.class);
    }

    @JRubyMethod(meta = true)
    public static IRubyObject __make_shareable(ThreadContext context, IRubyObject self, IRubyObject obj, IRubyObject copy) {
        if (copy.isTrue()) {
            return make_shareable_copy(context, obj);
        } else {
            return make_shareable(context, obj);
        }
    }

    @JRubyMethod(name = "shareable?", meta = true)
    public static IRubyObject shareable_p(ThreadContext context, IRubyObject self, IRubyObject obj) {
        return Convert.asBoolean(context, obj.isShareable());
    }

    private static IRubyObject make_shareable(ThreadContext context, IRubyObject obj) {
        objectTraverse(context, obj, RubyRactor::checkShareable, (c, o) -> NULL_LEAVE, RubyRactor::markShareable);

        return obj;
    }

    private static IRubyObject make_shareable_copy(ThreadContext context, IRubyObject obj) {
        // TODO
//        IRubyObject copy = ractorCopy(context, obj);
        IRubyObject copy = obj.dup();
        return make_shareable(context, copy);
    }

    enum TraverseResult {
        CONTINUE,
        SKIP,
        STOP
    }

    static boolean
    objectTraverse(ThreadContext context,
                    IRubyObject obj,
                    BiFunction<ThreadContext, IRubyObject, TraverseResult> enter_func,
                    BiFunction<ThreadContext, IRubyObject, TraverseResult> leave_func,
                    BiFunction<ThreadContext, IRubyObject, TraverseResult> final_func) {
        IdentityHashMap[] rec = {null};
        if (objectTraverseInner(context, obj, enter_func, leave_func, rec)) return true;
        if (final_func != null && rec[0] != null) {
            boolean[] stopped = {false};
            RuntimeException stop = new RuntimeException();
            try {
                rec[0].forEach((k, v) -> {
                    if (final_func.apply(context, (IRubyObject) k) != NULL_LEAVE) {
                        stopped[0] = true;
                        throw stop;
                    }
                });
            } catch (RuntimeException re) {
                if (re != stop) throw re;
            }
            return stopped[0];
        }
        return false;
    }

    private static IdentityHashMap<IRubyObject, IRubyObject> traverseRec(IdentityHashMap<IRubyObject, IRubyObject>[] rec) {
        IdentityHashMap<IRubyObject, IRubyObject> hash = rec[0];
        if (hash == null) {
            hash = rec[0] = new IdentityHashMap<>();
        }
        return hash;
    }

    private static boolean objectTraverseInner(ThreadContext context, IRubyObject obj, BiFunction<ThreadContext, IRubyObject, TraverseResult> enter_func,
                                               BiFunction<ThreadContext, IRubyObject, TraverseResult> leave_func,
                                               IdentityHashMap[] rec) {
//        if (RB_SPECIAL_CONST_P(obj)) return 0;

        switch (enter_func.apply(context, obj)) {
            case CONTINUE: break;
            case SKIP: return false; // skip children
            case STOP: return true; // stop search
        }

        if (traverseRec(rec).containsKey(obj)) {
            // already traversed
            return false;
        } else {
            traverseRec(rec).put(obj, obj);
        }
//        RB_OBJ_WRITTEN(data->rec_hash, Qundef, obj);

        boolean[] stopped = {false};
        RuntimeException stop = new RuntimeException();
        try {
            obj.getInstanceVariables().forEachInstanceVariable((name, value) -> {
                if (objectTraverseInner(context, value, enter_func, leave_func, rec)) {
                    stopped[0] = true;
                    throw stop;
                }
            });
        } catch (RuntimeException re) {
            if (re != stop) throw re;
        }
        if (stopped[0]) return true;

        switch (obj.getType().classIndex) {
            // no child node
            case STRING:
            case FLOAT:
            case BIGNUM:
            case REGEXP:
            case FILE:
            case SYMBOL:
            case MATCHDATA:
                break;

            case OBJECT:
                /* Instance variables already traversed. */
                break;

            case ARRAY:
            {
                RubyArray ary = (RubyArray) obj;
                for (int i = 0; i < ary.size(); i++) {
                    IRubyObject e = ary.eltInternal(i);
                    if (objectTraverseInner(context, e, enter_func, leave_func, rec)) return true;
                }
            }
            break;

            case HASH:
            {
                IRubyObject ifNone = ((RubyHash) obj).getIfNone();
                if (ifNone != RubyBasicObject.UNDEF && objectTraverseInner(context, ifNone, enter_func, leave_func, rec)) {
                    return true;
                }

                try {
                    ((RubyHash) obj).visitAll(context, (c, s, k, v, i) -> {
                        if (objectTraverseInner(context, k, enter_func, leave_func, rec)) {
                            stopped[0] = true;
                            throw stop;
                        }

                        if (objectTraverseInner(context, v, enter_func, leave_func, rec)) {
                            stopped[0] = true;
                            throw stop;
                        }
                    });

                    if (stopped[0]) return true;
                } catch (RuntimeException re) {
                    if (re != stop) throw re;
                }
                }
            break;

//            case STRUCT:
//            {
//                long len = RSTRUCT_LEN(obj);
//            const VALUE *ptr = RSTRUCT_CONST_PTR(obj);
//
//                for (long i=0; i<len; i++) {
//                    if (obj_traverse_i(ptr[i], data)) return 1;
//                }
//            }
//            break;
//
//            case RATIONAL:
//                if (obj_traverse_i(RRATIONAL(obj)->num, data)) return 1;
//            if (obj_traverse_i(RRATIONAL(obj)->den, data)) return 1;
//            break;
//            case COMPLEX:
//                if (obj_traverse_i(RCOMPLEX(obj)->real, data)) return 1;
//            if (obj_traverse_i(RCOMPLEX(obj)->imag, data)) return 1;
//            break;

//            case DATA:
////            case T_IMEMO:
//            {
//                struct obj_traverse_callback_data d = {
//                    .stop = false,
//                .data = data,
//            };
//                RB_VM_LOCKING_NO_BARRIER() {
//                rb_objspace_reachable_objects_from(obj, obj_traverse_reachable_i, &d);
//            }
//                if (d.stop) return 1;
//            }
//            break;
//
//            // unreachable
//            case CLASS:
//            case MODULE:
//            case T_ICLASS:
            default:
                throw new RuntimeException("bug: not yet shareable: " + obj.getClass().getName());
//                rp(obj);
//                rb_bug("unreachable");
        }

        if (leave_func.apply(context, obj) == TraverseResult.STOP) {
            return true;
        }
        else {
            return false;
        }
    }

    private static boolean allow_frozen_shareable_p(ThreadContext context, IRubyObject obj) {
        if (!(obj instanceof RubyData)) {
            return true;
        }
//        else if (RTYPEDDATA_P(obj)) {
//        const rb_data_type_t *type = RTYPEDDATA_TYPE(obj);
//            if (type->flags & RUBY_TYPED_FROZEN_SHAREABLE) {
//                return true;
//            }
//        }

        return false;
    }

    private static TraverseResult checkShareable(ThreadContext context, IRubyObject obj) {
        if (obj.isShareable()) {
            return TraverseResult.SKIP;
        }

        if (!allow_frozen_shareable_p(context, obj)) {
            if (obj instanceof RubyProc) {
//                rb_proc_ractor_make_shareable(obj);
                // TODO
//                obj.makeShareable();
                return NULL_LEAVE;
            }
            else {
                throw Error.ractorError(context, "can not make shareable object for " + obj);
            }
        }

        // CRuby logic forces object_id for all but T_IMEMO
        obj.id();
//        switch (TYPE(obj)) {
//            case T_IMEMO:
//                return traverse_skip;
//            case T_OBJECT:
//            {
//                // If a T_OBJECT is shared and has no free capacity, we can't safely store the object_id inline,
//                // as it would require to move the object content into an external buffer.
//                // This is only a problem for T_OBJECT, given other types have external fields and can do RCU.
//                // To avoid this issue, we proactively create the object_id.
//                shape_id_t shape_id = RBASIC_SHAPE_ID(obj);
//                attr_index_t capacity = RSHAPE_CAPACITY(shape_id);
//                attr_index_t free_capacity = capacity - RSHAPE_LEN(shape_id);
//                if (!rb_shape_has_object_id(shape_id) && capacity && !free_capacity) {
//                    rb_obj_id(obj);
//                }
//            }
//            break;
//            default:
//                break;
//        }

        if (!obj.isFrozen()) {
            obj.callMethod(context, "freeze");

            if (!obj.isFrozen()) {
                throw Error.ractorError(context, "#freeze does not freeze object correctly");
            }

            if (obj.isShareable()) {
                return TraverseResult.SKIP;
            }
        }

        return NULL_LEAVE;
    }

    private static TraverseResult  markShareable(ThreadContext context, IRubyObject obj) {
        ((RubyBasicObject) obj).setFlag(ObjectFlags.SHAREABLE_F, true);
        return NULL_LEAVE;
    }
}
