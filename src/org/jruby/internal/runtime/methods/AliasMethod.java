/*
 * AliasMethod.java - description
 * Created on 03.03.2002, 00:33:23
 * 
 * Copyright (C) 2001, 2002 Jan Arne Petersen
 * Jan Arne Petersen <jpetersen@uni-bonn.de>
 *
 * JRuby - http://jruby.sourceforge.net
 * 
 * This file is part of JRuby
 * 
 * JRuby is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 * 
 * JRuby is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with JRuby; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 * 
 */
package org.jruby.internal.runtime.methods;

import org.jruby.Ruby;
import org.jruby.RubyModule;
import org.jruby.runtime.ICallable;
import org.jruby.runtime.builtin.IRubyObject;

/**
 *
 * @author  jpetersen
 * @version $Revision$
 */
public class AliasMethod extends AbstractMethod {
    private ICallable oldMethod;
    private String oldName;
    private RubyModule origin;

    public AliasMethod(ICallable oldMethod, String oldName) {
        super(oldMethod.getVisibility());
        if (oldMethod instanceof AliasMethod) {
            initializeFrom(((AliasMethod) oldMethod));
        } else {
            this.oldName = oldName;
            this.origin = oldMethod.getImplementationClass();
            this.oldMethod = oldMethod;
        }
    }

    private void initializeFrom(AliasMethod otherAlias) {
        this.oldName = otherAlias.getOldName();
        this.origin = otherAlias.getOrigin();
        this.oldMethod = otherAlias.getOldMethod();
    }

    /**
     * @fixme name or oldName ?
     */
    public IRubyObject call(Ruby ruby, IRubyObject receiver, String name, IRubyObject[] args, boolean noSuper) {
        return oldMethod.call(ruby, receiver, name, args, noSuper);
    }

    public ICallable getOldMethod() {
        return oldMethod;
    }

    public String getOldName() {
        return oldName;
    }

    public RubyModule getOrigin() {
        return origin;
    }

    public void initializeCacheEntry(CacheEntry cacheEntry) {
        cacheEntry.setVisibility(getVisibility());
        cacheEntry.setOrigin(getOrigin());
        cacheEntry.setOriginalName(getOldName());
        cacheEntry.setMethod(getOldMethod());
        cacheEntry.setRecvClass(getOrigin());
    }
}
