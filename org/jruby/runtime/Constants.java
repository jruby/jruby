/*
 * Constants.java - No description
 * Created on 02. November 2001, 01:25
 * 
 * Copyright (C) 2001 Jan Arne Petersen, Stefan Matthias Aust, Alan Moore, Benoit Cerrina
 * Jan Arne Petersen <japetersen@web.de>
 * Stefan Matthias Aust <sma@3plus4.de>
 * Alan Moore <alan_moore@gmx.net>
 * Benoit Cerrina <b.cerrina@wanadoo.fr>
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

package org.jruby.runtime;

import java.io.File;
import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.ablaf.ast.INode;
import org.ablaf.common.ISourcePosition;
import org.ablaf.internal.lexer.DefaultLexerPosition;
import org.ablaf.lexer.LexerFactory;
import org.ablaf.parser.IParser;
import org.jruby.Ruby;
import org.jruby.RubyFixnum;
import org.jruby.RubyInteger;
import org.jruby.RubyObject;
import org.jruby.RubyString;
import org.jruby.RubySymbol;
import org.jruby.ast.MultipleAsgnNode;
import org.jruby.ast.ZeroArgNode;
import org.jruby.common.RubyErrorHandler;
import org.jruby.evaluator.AssignmentVisitor;
import org.jruby.evaluator.EvaluateVisitor;
import org.jruby.exceptions.ArgumentError;
import org.jruby.exceptions.BreakJump;
import org.jruby.exceptions.LoadError;
import org.jruby.exceptions.NextJump;
import org.jruby.exceptions.RaiseException;
import org.jruby.exceptions.RedoJump;
import org.jruby.exceptions.RetryException;
import org.jruby.exceptions.ReturnException;
import org.jruby.exceptions.RubyBugException;
import org.jruby.exceptions.RubySecurityException;
import org.jruby.internal.runtime.methods.IterateMethod;
import org.jruby.internal.runtime.methods.RubyMethodCache;
import org.jruby.javasupport.JavaSupport;
import org.jruby.javasupport.JavaUtil;
import org.jruby.parser.DefaultRubyParser;
import org.jruby.parser.IRubyParserResult;
import org.jruby.parser.RubyParserConfiguration;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.regexp.IRegexpAdapter;
import org.jruby.util.RubyHashMap;
import org.jruby.util.RubyMap;
import org.jruby.util.RubyStack;
import org.jruby.util.collections.CollectionFactory;
import org.jruby.util.collections.IStack;

public abstract class Constants {
    public static final int SCOPE_PUBLIC = 0;
    public static final int SCOPE_PRIVATE = 1;
    public static final int SCOPE_PROTECTED = 2;
    public static final int SCOPE_MODFUNC = 5;

    public static final int NOEX_PUBLIC = 0;
    public static final int NOEX_UNDEF = 1;
    public static final int NOEX_CFUNC = 1;
    public static final int NOEX_PRIVATE = 2;
    public static final int NOEX_PROTECTED = 4;

    public static final String RUBY_MAJOR_VERSION = "1.6";
    public static final String RUBY_VERSION = "1.6.7";

    public static final int MARSHAL_MAJOR = 4;
    public static final int MARSHAL_MINOR = 5;
}