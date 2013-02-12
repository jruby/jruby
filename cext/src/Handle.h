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
 * Copyright (C) 2008-2010 Wayne Meissner
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


#ifndef JRUBY_HANDLE_H
#define	JRUBY_HANDLE_H

#include <jni.h>
#include <vector>
#include "jruby.h"
#include "util.h"
#include "queue.h"
#include "ruby.h"

namespace jruby {

    class Handle;
    class RubyData;
    struct Symbol;
    extern Handle* constHandles[3];
    extern std::vector<Symbol*> symbols;
    TAILQ_HEAD(HandleList, Handle);
    TAILQ_HEAD(DataHandleList, RubyData);
    SIMPLEQ_HEAD(SyncQueue, Handle);
    extern HandleList liveHandles, deadHandles;
    extern DataHandleList dataHandles;


    class Handle {
    private:
        void Init();
        void makeStrong_(JNIEnv* env);
        void makeWeak_(JNIEnv* env);


    public:
        Handle();
        Handle(JNIEnv* env, jobject obj, int type_ = T_NONE);
        virtual ~Handle();

        static inline Handle* valueOf(VALUE v) {
            return likely(!SPECIAL_CONST_P(v)) ? (Handle *) v : specialHandle(v);
        }

	inline VALUE asValue() {
	    return (VALUE) this;
	}

        inline bool isWeak() {
            return (flags & FL_WEAK) != 0;
        }

        inline void makeStrong(JNIEnv* env) {
            if (unlikely(isWeak())) {
                makeStrong_(env);
            }
        }

        inline void makeWeak(JNIEnv* env) {
            if (unlikely(!isWeak())) {
                makeWeak_(env);
            }
        }

        inline int getType() {
            return flags & T_MASK;
        }

        inline void setType(int type_) {
            this->flags |= (type_ & T_MASK);
        }

        static Handle* specialHandle(VALUE v);

        jobject obj;
        int flags;
        TAILQ_ENTRY(Handle) all;
    };

    class RubyFixnum : public Handle {
    private:
        jlong value;

    public:
        RubyFixnum(JNIEnv* env, jobject obj_, jlong value_);

        inline jlong longValue() {
            return value;
        }
    };

    class RubyFloat : public Handle {
    private:
        bool registered_;
        struct RFloat rfloat_;
        DataSync jsync_;
        DataSync nsync_;
        DataSync clean_;

    public:
        RubyFloat(jdouble value_);
        RubyFloat(JNIEnv* env, jobject obj_, jdouble value_);

        inline jdouble doubleValue() {
            return rfloat_.value;
        }

        struct RFloat* toRFloat();
        bool jsync(JNIEnv* env);
        bool nsync(JNIEnv* env);
        bool clean(JNIEnv* env);
    };

    class RubyIO : public Handle {
    private:
        struct RIO rio;

    public:
        RubyIO(FILE* native_file, int native_fd, int mode_);
        RubyIO(JNIEnv* env, jobject obj_, jint fileno, jint mode_);
        virtual ~RubyIO();

        struct RIO* toRIO();
    };

    class RubyData : public Handle {
    private:
        RData rdata;

    public:
        RubyData(void* data, RUBY_DATA_FUNC dmark, RUBY_DATA_FUNC dfree);
        virtual ~RubyData();

        inline struct RData* toRData() {
            return &rdata;
        }

        TAILQ_ENTRY(RubyData) dataList;

    };

    class RubyString : public Handle {
    private:
        struct RWData {
            bool readonly;
	    bool valid;
            RString* rstring;
            DataSync jsync;
            DataSync nsync;
            DataSync clean;
            DataSync rosync;
        };
        RWData rwdata;

    public:
        RubyString(JNIEnv* env, jobject obj);
        virtual ~RubyString();

        RString* toRString(bool readonly);
        bool jsync(JNIEnv* env);
        bool nsync(JNIEnv* env);
        bool clean(JNIEnv* env);
        int length();
    };

    class RubyArray : public Handle {
    private:
        struct RWData {
            bool readonly;
	    bool valid;
            RArray* rarray;
            DataSync jsync;
            DataSync nsync;
            DataSync clean;
            DataSync rosync;
        };
        RWData rwdata;

    public:
        RubyArray(JNIEnv* env, jobject obj);
        virtual ~RubyArray();

        RArray* toRArray(bool readonly);
        int length();
        void markElements();
        bool jsync(JNIEnv* env);
        bool nsync(JNIEnv* env);
        bool clean(JNIEnv* env);
    };

    extern void runSyncQueue(JNIEnv* env, DataSyncQueue* q);

    inline void jsync(JNIEnv* env) {
        if (unlikely(!TAILQ_EMPTY(&jsyncq))) {
            runSyncQueue(env, &jsyncq);
        }
    }

    inline void nsync(JNIEnv* env) {
        if (unlikely(!TAILQ_EMPTY(&nsyncq))) {
            runSyncQueue(env, &nsyncq);
        }
    }

    inline VALUE makeStrongRef(JNIEnv* env, VALUE v) {
        if (!SPECIAL_CONST_P(v)) {
            Handle::valueOf(v)->makeStrong(env);
        }

        return v;
    }

    struct Symbol {
        ID id;
        char* cstr;
        jobject obj;
        jobject jstr;
    };

    extern Symbol* resolveSymbolById(ID id);

    inline Symbol* lookupSymbolById(ID id) {
        if (likely(id < symbols.size() && (symbols[id] != NULL))) {
            return symbols[id];
        }
        return resolveSymbolById(id);
    }

    inline jobject idToObject(JNIEnv* env, ID id) {
        return lookupSymbolById(id)->obj;
    }

    inline jobject idToString(JNIEnv* env, ID id) {
        return lookupSymbolById(id)->jstr;
    }

    extern jobject fixnumToObject(JNIEnv* env, VALUE v);
}

#endif	/* JRUBY_HANDLE_H */
