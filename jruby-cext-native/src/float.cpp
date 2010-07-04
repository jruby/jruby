
#include "jruby.h"
#include "Handle.h"
#include "JLocalEnv.h"
#include "JUtil.h"

using namespace jruby;

RubyFloat::RubyFloat(double value)
{
    type = T_FLOAT;
    rfloat.value = value;
}

RubyFloat::RubyFloat(JNIEnv* env, jobject obj_, jdouble value_): Handle(env, obj_, T_FLOAT)
{
    rfloat.value = value_;
}

extern "C" struct RFloat*
jruby_rfloat(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->type == T_FLOAT) {
        return ((RubyFloat *) h)->toRFloat();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}

extern "C" VALUE
rb_float_new(double value)
{
    JLocalEnv env;

    //env->CallStaticObjectMethod();
    RubyFloat* f = new RubyFloat(value);
    jvalue params[3];
    params[0].l = jruby::getRuntime();
    params[1].j = p2j(f);
    params[2].d = value;
    
    jobject rubyFloat = env->CallStaticObjectMethodA(JRuby_class, JRuby_newFloat, params);
    f->obj = env->NewGlobalRef(rubyFloat);

    return (VALUE) f;
}

extern "C" double
jruby_float_value(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->type == T_FLOAT) {
        return ((RubyFloat *) h)->doubleValue();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}

extern "C" double
rb_num2dbl(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->type == T_FLOAT) {
        return ((RubyFloat *) h)->doubleValue();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}