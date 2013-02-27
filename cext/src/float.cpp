 /***** BEGIN LICENSE BLOCK *****
 * Version: EPL 1.0/GPL 2.0/LGPL 2.1
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
 * Copyright (C) 2010 Wayne Meissner
 * Copyright (C) 2010 Tim Felgentreff
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

#include "jruby.h"
#include "Handle.h"
#include "JLocalEnv.h"
#include "JUtil.h"

using namespace jruby;

RubyFloat::RubyFloat(double value): registered_(false)
{
    setType(T_FLOAT);
    memset(&rfloat_, 0, sizeof(rfloat_));
    rfloat_.value = value;
}

RubyFloat::RubyFloat(JNIEnv* env, jobject obj_, jdouble value_): Handle(env, obj_, T_FLOAT)
{
    memset(&rfloat_, 0, sizeof(rfloat_));
    rfloat_.value = value_;
}


static bool
RubyFloat_jsync(JNIEnv* env, DataSync* data)
{
    return ((RubyFloat *) data->data)->jsync(env);
}

static bool
RubyFloat_nsync(JNIEnv* env, DataSync* data)
{
    return ((RubyFloat *) data->data)->nsync(env);
}

static bool
RubyFloat_clean(JNIEnv* env, DataSync* data)
{
    return ((RubyFloat *) data->data)->clean(env);
}

struct RFloat*
RubyFloat::toRFloat()
{
    if (!registered_) {
        jsync_.data = this;
        jsync_.sync = RubyFloat_jsync;
        nsync_.data = this;
        nsync_.sync = RubyFloat_nsync;
        clean_.data = this;
        clean_.sync = RubyFloat_clean;
        TAILQ_INSERT_TAIL(&jruby::cleanq, &clean_, syncq);
        TAILQ_INSERT_TAIL(&jruby::jsyncq, &jsync_, syncq);
        TAILQ_INSERT_TAIL(&jruby::nsyncq, &nsync_, syncq);
        registered_ = true;
    }

    return &rfloat_;
}

bool
RubyFloat::jsync(JNIEnv* env)
{
    env->SetDoubleField(obj, RubyFloat_value_field, rfloat_.value);
    return true;
}

bool
RubyFloat::nsync(JNIEnv* env)
{
    rfloat_.value = env->GetDoubleField(obj, RubyFloat_value_field);
    return true;
}

bool
RubyFloat::clean(JNIEnv* env)
{
    registered_ = false;
    return true;
}

extern "C" struct RFloat*
jruby_rfloat(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_FLOAT) {
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

    return f->asValue();
}

extern "C" double
jruby_float_value(VALUE v)
{
    Handle* h = Handle::valueOf(v);
    if (h->getType() == T_FLOAT) {
        return ((RubyFloat *) h)->doubleValue();
    }

    rb_raise(rb_eTypeError, "wrong type (expected Float)");
}

extern "C" VALUE
rb_Float(VALUE obj)
{
    return rb_convert_type(obj, T_FLOAT, "Float", "to_f");
}

extern "C" double
rb_num2dbl(VALUE val)
{
    switch (TYPE(val)) {
      case T_FLOAT:
        return RFLOAT_VALUE(val);

      case T_STRING:
        rb_raise(rb_eTypeError, "no implicit conversion to float from string");
        break;

      case T_NIL:
        rb_raise(rb_eTypeError, "no implicit conversion to float from nil");
        break;

      default:
        break;
    }

    return RFLOAT_VALUE(rb_Float(val));
}
