/*
 ***** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2007-2012 Charles Oliver Nutter <headius@headius.com>
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

package org.jruby.anno;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import org.jruby.runtime.Visibility;


@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface JRubyMethod {
    /**
     * The name or names of this method in Ruby-land.
     */
    String[] name() default {};
    /**
     * The number of required arguments.
     */
    int required() default 0;
    /**
     * The number of optional arguments.
     */
    int optional() default 0;
    /**
     * Whether this method has a "rest" argument.
     */
    boolean rest() default false;
    /**
     * Any alias or aliases for this method.
     */
    String[] alias() default {};
    /**
     * Whether this method should be defined on the metaclass.
     */
    boolean meta() default false;
    /**
     * Whether this method should be a module function, defined on metaclass and private on class.
     */
    boolean module() default false;
    /**
     * Whether this method expects to have a call frame allocated for it.
     */
    boolean frame() default false;
    /**
     * The visibility of this method.
     */
    Visibility visibility() default Visibility.PUBLIC;

    /**
     * What, if anything, method reads from caller's frame
     */
    FrameField[] reads() default {};

    /**
     * What, if anything, method writes to caller's frame
     */
    FrameField[] writes() default {};

    /**
     * Argument types to coerce to before calling
     */
    Class[] argTypes() default {};

    /**
     * Whether to use a frame slot for backtrace information
     */
    boolean omit() default false;

    /**
     * Whether this method should show up as defined in response to respond_to? calls
     */
    boolean notImplemented() default false;

    /**
     * A list of classes that implement an abstract JRubyMethod, for backtrace purposes.
     */
    Class[] implementers() default {};

    @Deprecated
    boolean scope() default false;

    @Deprecated
    org.jruby.CompatVersion compat() default org.jruby.CompatVersion.BOTH;

    @Deprecated
    boolean backtrace() default false;
}
