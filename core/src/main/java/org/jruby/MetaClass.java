/***** BEGIN LICENSE BLOCK *****
 * Version: EPL 2.0/GPL 2.0/LGPL 2.1
 *
 * The contents of this file are subject to the Eclipse Public
 * License Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.eclipse.org/legal/epl-v20.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 * Copyright (C) 2002-2004 Jan Arne Petersen <jpetersen@uni-bonn.de>
 * Copyright (C) 2004-2006 Thomas E Enebo <enebo@acm.org>
 * Copyright (C) 2004 Stefan Matthias Aust <sma@3plus4.de>
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

package org.jruby;

import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.util.cli.Options;
import org.jruby.util.log.Logger;
import org.jruby.util.log.LoggerFactory;

public final class MetaClass extends RubyClass {

    @Deprecated
    public MetaClass(Ruby runtime, RubyClass superClass, IRubyObject attached) {
        this(runtime, superClass, (RubyBasicObject) attached);
    }

    /**
     * rb_class_boot for meta classes ({@link #makeMetaClass(RubyClass)})
     */
    MetaClass(Ruby runtime, RubyClass superClass, RubyBasicObject attached) {
        super(runtime, superClass, false);
        this.attached = attached;
        // use same ClassIndex as metaclass, since we're technically still of that type
        setClassIndex(superClass.getClassIndex());
        superClass.addSubclass(this);

        if (LOG_SINGLETONS || LOG_SINGLETONS_VERBOSE) {
            logSingleton(runtime, superClass, attached);
        }
    }

    private static void logSingleton(Ruby runtime, RubyClass superClass, RubyBasicObject attached) {
        if (runtime.isBooting()) return; // don't log singleton created during boot

        String attachedString = attached == null ? "null object" : "object of type " + attached.getMetaClass();
        LOG.info("singleton class created for type " + superClass + " attached to " + attachedString);

        if (LOG_SINGLETONS_VERBOSE) {
            LOG.info(new Exception("singleton creation stack trace"));
        }
    }

    @Override
    public final IRubyObject allocate(){
        throw runtime.newTypeError("can't create instance of virtual class");
    }

    @Override
    public boolean isSingleton() {
        return true;
    }

    /**
     * rb_make_metaclass
     * @param superClass
     * @return singleton-class for this (singleton) class
     */
    @Override
    public RubyClass makeMetaClass(RubyClass superClass) {
        MetaClass klass = new MetaClass(runtime, getSuperSingletonMetaClass(), this);
        setMetaClass(klass);

        // Foo.singleton_class.singleton_class: #<Class:#<Class:Foo>>
        // #<Class:#<Class:Foo>>'s singleton_class == #<Class:#<Class:Foo>>
        klass.setMetaClass(klass);

        return klass;
    }

    private RubyClass getSuperSingletonMetaClass() {
        if (attached instanceof RubyClass) {
            RubyClass superClass = ((RubyClass) attached).getSuperClass();
            if (superClass != null) superClass = superClass.getRealClass();
            // #<Class:BasicObject>'s singleton class == Class.singleton_class
            if (superClass == null) return runtime.getClassClass().getSingletonClass();
            return superClass.getMetaClass().getSingletonClass();
        }

        return getSuperClass().getRealClass().getMetaClass(); // NOTE: is this correct?
    }

    @Override
    RubyClass toSingletonClass(RubyBasicObject target) {
        return attached == target ? this : super.toSingletonClass(target);
    }

    public RubyBasicObject getAttached() {
        return attached;
    }

    public void setAttached(RubyBasicObject attached) {
        this.attached = attached;
    }

    private RubyBasicObject attached;

    private static final Logger LOG = LoggerFactory.getLogger(MetaClass.class);
    private static final boolean LOG_SINGLETONS = Options.LOG_SINGLETONS.load();
    private static final boolean LOG_SINGLETONS_VERBOSE = Options.LOG_SINGLETONS_VERBOSE.load();

}
