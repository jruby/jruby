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
 * Copyright (C) 2009, 2010 Wayne Meissner
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

#include <jni.h>

#include "queue.h"

#include "JUtil.h"
#include "jruby.h"
#include "JavaException.h"
#include "ruby.h"
#include "JLocalEnv.h"
#include "Handle.h"

using namespace jruby;
DataHandleList jruby::dataHandles = TAILQ_HEAD_INITIALIZER(dataHandles);

static void rubydata_finalize(Handle *);

extern "C" VALUE
rb_data_object_alloc(VALUE klass, void* data, RUBY_DATA_FUNC dmark, RUBY_DATA_FUNC dfree)
{
    JLocalEnv env;

    RubyData* h = new RubyData(data, dmark, dfree);

    jvalue params[3];
    params[0].l = getRuntime();
    params[1].l = valueToObject(env, klass);
    params[2].j = p2j(h);

    jobject obj = env->CallStaticObjectMethodA(RubyData_class, RubyData_newRubyData_method, params);
    checkExceptions(env);

    h->obj = env->NewGlobalRef(obj);
    checkExceptions(env);

    return h->asValue();
}

RubyData::RubyData(void* data, RUBY_DATA_FUNC dmark, RUBY_DATA_FUNC dfree)
{
    memset(&rdata, 0, sizeof(rdata));
    rdata.data = data;
    rdata.dmark = dmark;
    rdata.dfree = dfree;
    setType(T_DATA);
    TAILQ_INSERT_TAIL(&dataHandles, this, dataList);
}

RubyData::~RubyData()
{
    TAILQ_REMOVE(&dataHandles, this, dataList);

    if (rdata.dfree == (void *) -1) {
        xfree(rdata.data);

    } else if (rdata.dfree != NULL) {
        (*rdata.dfree)(rdata.data);
    }
}

extern "C" void*
jruby_data(VALUE v)
{
    return jruby_rdata(v)->data;
}

extern "C" struct RData*
jruby_rdata(VALUE v)
{
    if (TYPE(v) != T_DATA) {
        rb_raise(rb_eTypeError, "not a data object");
        return NULL;
    }

    RubyData* d = dynamic_cast<RubyData *>(Handle::valueOf(v));

    return d->toRData();
}
