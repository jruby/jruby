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
 * Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
 * Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
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
package org.jruby.internal.runtime.methods;

import org.jruby.RubyModule;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.Visibility;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class CacheEntry {
    private String name;            /* method's id */
    private String originalName;    /* method's original id */
    private RubyModule recvClass;   /* receiver's class */
    private RubyModule origin;      /* where method defined  */

    private ICallable method;
    private Visibility visibility;

    private CacheEntry(String name, String originalName, RubyModule recvClass, RubyModule origin, ICallable method, Visibility visibility) {
        this.name = name;
        this.originalName = originalName;
        this.recvClass = recvClass;
        this.origin = origin;
        this.method = method;
        this.visibility = visibility;
    }

    public CacheEntry(String name, RubyModule recvClass) {
        this(name, name, recvClass, null, null, null);
    }

    public static CacheEntry createUndefined(String name, RubyModule recvClass) {
        return new CacheEntry(name, name, recvClass, recvClass, UndefinedMethod.getInstance(), Visibility.PUBLIC);
    }

    /** Getter for property recvClass.
     * @return Value of property recvClass.
     */
    public RubyModule getRecvClass() {
        return recvClass;
    }

    /** Setter for property recvClass.
     * @param recvClass New value of property recvClass.
     */
    public void setRecvClass(RubyModule recvClass) {
        this.recvClass = recvClass;
    }

    /** Getter for property method.
     * @return Value of property method.
     */
    public ICallable getMethod() {
        return method;
    }

    /** Setter for property method.
     * @param method New value of property method.
     */
    public void setMethod(ICallable method) {
        this.method = method;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public Visibility getVisibility() {
        return visibility;
    }

    public void setVisibility(Visibility visibility) {
        this.visibility = visibility;
    }

    /** Getter for property origin.
     * @return Value of property origin.
     */
    public RubyModule getOrigin() {
        return origin;
    }

    /** Setter for property origin.
     * @param origin New value of property origin.
     */
    public void setOrigin(RubyModule origin) {
        this.origin = origin;
    }

    public boolean isDefined() {
        return ! getMethod().isUndefined();
    }
}

