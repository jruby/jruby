/*
 * Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
 * code is released under a tri EPL/GPL/LGPL license. You can use it,
 * redistribute it and/or modify it under the terms of the:
 *
 * Eclipse Public License version 1.0
 * GNU General Public License version 2
 * GNU Lesser General Public License version 2.1
 */
package org.jruby.truffle.stdlib.psych;

import com.oracle.truffle.api.object.DynamicObject;
import com.oracle.truffle.api.object.DynamicObjectFactory;
import org.jruby.truffle.core.basicobject.BasicObjectLayout;
import org.jruby.truffle.om.dsl.api.Layout;
import org.jruby.truffle.om.dsl.api.Nullable;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.parser.Parser;

@Layout
public interface ParserLayout extends BasicObjectLayout {

    DynamicObjectFactory createParserShape(
            DynamicObject logicalClass,
            DynamicObject metaClass);

    DynamicObject createParser(
            DynamicObjectFactory factory,
            @Nullable Parser parser,
            @Nullable Event event);

    boolean isParser(DynamicObject object);

    Parser getParser(DynamicObject object);
    void setParser(DynamicObject object, Parser value);

    Event getEvent(DynamicObject object);
    void setEvent(DynamicObject object, Event value);

}
