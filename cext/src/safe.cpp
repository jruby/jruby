
/**********************************************************************

  safe.c -

  $Author: nobu $
  created at: Tue Sep 23 09:44:32 JST 2008

  Copyright (C) 2008 Yukihiro Matsumoto

**********************************************************************/

/* safe-level:
   0 - strings from streams/environment/ARGV are tainted (default)
   1 - no dangerous operation by tainted value
   2 - process/file operations prohibited
   3 - all generated objects are tainted
   4 - no global (non-tainted) variable modification/no direct output
*/

#include "jruby.h"
#include "ruby.h"
#include "JLocalEnv.h"

using namespace jruby;

#define rb_frame_callee() (0)

extern "C" int
rb_safe_level(void)
{
    return FIX2INT(rb_gv_get("$SAFE"));
}

extern "C" void
rb_secure(int level)
{
    if (level <= rb_safe_level()) {
        if (rb_frame_callee()) {
            rb_raise(rb_eSecurityError, "Insecure operation `%s' at level %d",
                     rb_id2name(rb_frame_callee()), rb_safe_level());
        }
        else {
            rb_raise(rb_eSecurityError, "Insecure operation at level %d",
                     rb_safe_level());
        }
    }
}

extern "C" void
rb_secure_update(VALUE obj)
{
    if (!OBJ_TAINTED(obj))
        rb_secure(4);
}

extern "C" void
rb_insecure_operation(void)
{
    if (rb_frame_callee()) {
        rb_raise(rb_eSecurityError, "Insecure operation - %s",
                 rb_id2name(rb_frame_callee()));
    }
    else {
        rb_raise(rb_eSecurityError, "Insecure operation: -r");
    }
}


extern "C" void
rb_check_safe_obj(VALUE x)
{
    if (rb_safe_level() > 0 && OBJ_TAINTED(x)) {
        rb_insecure_operation();
    }
    rb_secure(4);
}

extern "C" void
rb_check_safe_str(VALUE x)
{
    rb_check_safe_obj(x);
    if (!RB_TYPE_P(x, T_STRING)) {
        rb_raise(rb_eTypeError, "wrong argument type %s (expected String)",
                 rb_obj_classname(x));
    }
}
