<?xml version="1.0"?> 
<stylesheet xmlns="http://www.w3.org/1999/XSL/Transform" version="1.0">
<output method="text"/>
<strip-space elements="*"/>

<!--  Copyright (C) 2002 Thomas E. Enebo <enebo@acm.org>

   JRuby - http://jruby.sourceforge.net
 
   This file is part of JRuby
 
   JRuby is free software; you can redistribute it and/or
   modify it under the terms of the GNU General Public License as
   published by the Free Software Foundation; either version 2 of the
   License, or (at your option) any later version.
  
   JRuby is distributed in the hope that it will be useful,
   but WITHOUT ANY WARRANTY; without even the implied warranty of
   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
   GNU General Public License for more details.
 
   You should have received a copy of the GNU General Public
   License along with JRuby; if not, write to
   the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
   Boston, MA  02111-1307 USA
  -->    

<!--
   method_generator.xsl - Generate class/module definitions from an XML
   source.  To run this manually you can use ../samples/xslt.rb.  Use
   This file as the stylesheet and use test_data.xml as the source.
   Compare it against test_data.expected to see if it is working
   properly (I compare with diff -w).
  -->

<!-- The main match.  This writes out the entire class. -->
<template match="/">
  <text>package org.jruby.internal.runtime.builtin.definitions;

/* This is auto-generated source code from src/org/jruby/</text>
  <value-of select="//module/name"/>
  <text>.xml.
 * Edit xml and rebuild.  Changing it here is wrong.
 */

import org.jruby.Ruby;
import org.jruby.RubyClass;
import org.jruby.RubyClasses;
import org.jruby.RubyModule;
import org.jruby.runtime.builtin.IRubyObject;
import org.jruby.runtime.builtin.definitions.MethodContext;
import org.jruby.runtime.builtin.definitions.SingletonMethodContext;
import org.jruby.runtime.builtin.definitions.ModuleDefinition;
import org.jruby.runtime.builtin.definitions.ClassDefinition;
import org.jruby.runtime.builtin.definitions.ModuleFunctionsContext;
import org.jruby.util.Asserts;

public class </text>
  <value-of select="//module/name"/>
  <text>Definition extends </text>
  <if test="//module[@type='module']">
    <text>Module</text>
  </if>
  <if test="//module[not(@type='module')]">
    <text>Class</text>
  </if>
  <text>Definition { 
    private static final int </text>
  <call-template name="uc">
    <with-param name="value" select="//module/name"/>
  </call-template>
  <text> = </text>
  <if test="//module/index-offset">
    <value-of select="//module/index-offset"/>
  </if>
  <if test="not(//module/index-offset)">
    <text>0xf000</text>
  </if>
  <text>;
    private static final int STATIC = </text>
  <call-template name="uc">
    <with-param name="value" select="//module/name"/>
  </call-template>
  <text> | 0x100;
</text>
  <apply-templates select="//instance-methods" mode="declarations"/>
  <apply-templates select="//class-methods" mode="declarations"/>
  <text>
    public </text>
  <value-of select="//module/name"/>
  <text>Definition(Ruby runtime) {
        super(runtime);
    }

</text>
  <call-template name="type"/>
  <text>

    protected void defineMethods(MethodContext context) {
</text>
  <apply-templates select="//instance-methods" mode="definitions"/>
  <text>    }

</text>
  <if test="//module[@type='module']">
    <text>    protected void defineModuleFunctions(ModuleFunctionsContext context) {
</text>
  </if>
  <if test="//module[not(@type='module')]">
    <text>    protected void defineSingletonMethods(SingletonMethodContext context) {
</text>
  </if>
  <apply-templates select="//class-methods" mode="definitions"/>
  <text>    }

    public IRubyObject callIndexed(int index, IRubyObject receiver, 
                                   IRubyObject[] args) {
        switch (index) {
</text>
  <apply-templates select="//class-methods" mode="callIndexed"/>
  <text>          default:
              Asserts.notReached("'" + index + "' is not a valid index.");
              return null;
        }
    }
}
</text>
</template>

<!-- writes out the index declarations for all instance methods and also
     the first two obligatory private base value declarations -->
<template match="instance-methods" mode="declarations">
  <apply-templates mode="declarations"/>
</template>

<!-- writes out the index declarations for all class methods -->
<template match="class-methods" mode="declarations">
  <apply-templates mode="declarations"/>
</template>

<!-- writes out a single declaration of an index for method (index or 
     static) -->
<template match="method" mode="declarations">
  <text>    public static final int </text>
  <apply-templates select="@name"/>
  <text> = </text>
  <if test="name(..)='instance-methods'">
    <call-template name="uc">
      <with-param name="value" select="//module/name"/>
    </call-template>
  </if>
  <if test="name(..)='class-methods'">
    <text>STATIC</text>
  </if>
  <text> | </text>
  <number count="//method"/>
  <text>;
</text>
</template>

<!-- writes out the create(Type|Module) method. -->
<template name="type">
  <if test="//module/@type='class'">
    <text>    protected RubyClass createType(Ruby runtime) {
        RubyClasses classes = runtime.getClasses();
        RubyClass result = 
            runtime.defineClass("</text>
    <value-of select='//module/name'/>
    <text>", </text>
    <if test='//module/superclass'>
      <text>
                                (RubyClass) classes.getClass("</text>
      <value-of select='//module/superclass'/>
      <text>")</text>
    </if>
    <if test='not(//module/superclass)'>
      <text>(RubyClass) null</text>
    </if>
    <text>);
</text>
  </if>
  <if test="//module/@type='module'">
    <text>    protected RubyModule createModule(Ruby runtime) {
        RubyModule result = runtime.defineModule("</text>
    <value-of select='//module/name'/>
    <text>");
</text>
  </if>
  <apply-templates select="//includes"/>        
    <text>        return result;
    }</text>
</template>

<!-- calls children which write out the bodies of defineMethods and
     defineSingletonMethods -->
<template match="instance-methods|class-methods" mode="definitions">
  <apply-templates mode="definitions"/>
</template>

<!-- writes out context creation call for each method. -->
<template match="method" mode="definitions">
  <text>        context.create</text>
  <if test="arity[@optional]">
    <text>Optional</text>
  </if>
  <text>("</text>
  <value-of select='@name'/>
  <text>", </text>
  <apply-templates select="@name"/>
  <text>, </text>
  <if test="arity">
    <value-of select="arity"/>
  </if>
  <if test="not(arity)">
    <text>0</text>
  </if>);
</template>

<!-- writes out context creation for each alias (looks up method it is
     aliasing to do this) -->
<template match="method-alias" mode="definitions">
  <text>        context.create</text>
  <call-template name="get_optional_for">
    <with-param name="name" select="@original"/>
  </call-template>
  <text>("</text>
  <value-of select='@name'/>
  <text>", </text>
  <call-template name="get_method_for">
    <with-param name="name" select="@original"/>
  </call-template>
  <text>, </text>
  <call-template name="get_arity_for">
    <with-param name="name" select="@original"/>
  </call-template>);
</template>

<!-- writes out context undefine for each undefined method -->
<template match="undefine-method" mode="definitions">
  <text>        context.undefineMethod("</text>
  <value-of select='@name'/>
  <text>");
</text>
</template>

<!-- Includes all modules into the class/module -->
<template match="includes">
  <text>        result.includeModule(classes.getClass("</text>
  <value-of select='.'/>
  <text>"));
</text>
</template>

<!-- calls children which write out the bodies of callIndexed switch
     statements -->
<template match="class-methods" mode="callIndexed">
  <apply-templates mode="callIndexed"/>
</template>

<!-- writes out appropriate switch case for each static method in
     callIndexed. This section generates:
        case FOO:
           return org.jruby.Foo.heh(reciever, args[0], args[1]); -->
<template match="method" mode="callIndexed">
  <text>          case </text>
  <call-template name="uc_cond">
    <with-param name="test" select="java"/>
    <with-param name="fail" select="@name"/>
  </call-template>
  <text>:
              return </text>
  <value-of select="//implementation"/>
  <text>.</text>

  <!-- If java exists, then use it for the method name (duh), otherwise
       try and use the definitions name.  We only use java when we have
       a java-unfriendly method -->
  <if test="java">
    <value-of select="java"/>
  </if>
  <if test="not(java)">
    <value-of select="@name"/>
  </if>

  <!-- Create argument list for the method -->
  <text>(receiver</text>
  <call-template name="method_arity"/>
  <text>);
</text>
</template>

<!-- use java name over method name -->
<template match="@name">
  <call-template name="uc_cond">
    <with-param name="test" select="../java"/>
    <with-param name="fail" select="."/>
  </call-template>
</template>

<!-- get_method_for: applies the name attribute of the method which matches 
     the supplied name parameter.  This is used to find the name of the 
     method which matches an alias.  This template exists because I could 
     not figure out how to inline the XPath expression in this template. -->
<template name="get_method_for">
  <param name="name"/>
  <apply-templates select="../method[@name=$name]/@name"/>
</template>

<!-- get_arity_for: return proper arity value for method of 'name'. If
     unable to find arity or method, a '0' is returned.  This is used
     to generate arity for method-aliases. -->
<template name="get_arity_for">
  <param name="name"/>
  <if test="../method[@name=$name]/arity">
    <value-of select="../method[@name=$name]/arity"/>
  </if>
  <if test="not(../method[@name=$name]/arity)">
    <text>0</text>
  </if>
</template>

<!-- method_arity: prints out args or args[0], .... args[n] depending
     on whether arity if optional or not
  -->
<template name="method_arity">
  <if test="arity[@optional]">
    <text>, args</text>
  </if>
  <if test="arity[not(@optional)]">
    <call-template name="method_arity_args_count">
      <with-param name="count" select="arity"/>
      <with-param name="size" select="arity"/>
    </call-template>
  </if>
</template>

<!-- method_arity_args_count: recursive function which prints out
     args[0], args[1], .... args[size] 
  -->
<template name="method_arity_args_count">
  <param name="count"/>
  <param name="size"/>
  <if test="$count > 0">
    <text>, args[</text>
    <value-of select="format-number($size - $count, '0')"/>
    <text>]</text>
    <call-template name="method_arity_args_count">
      <with-param name="count" select="$count - 1"/>
      <with-param name="size" select="$size"/>
    </call-template>
  </if>
</template>

<!-- get_optional_for: See if the method of alias has optional arity.  
     If so then print optional.  Otherwise do nothing. -->
<template name="get_optional_for">
  <param name="name"/>
  <if test="../method[@name=$name]/arity[@optional]">
    <text>Optional</text>
  </if>
</template>

<!-- uc_cond: Convert 'test' argument to upper-case (uc) if it exists.  
     Otherwise, convert 'fail' argument to upper-case -->
<template name="uc_cond">
  <param name="test"/>
  <param name="fail"/>
  <if test="$test">
    <call-template name="uc">
      <with-param name="value" select="$test"/>
    </call-template>
  </if>
  <if test="not($test)">
    <call-template name="uc">
      <with-param name="value" select="$fail"/>
    </call-template>
  </if>
</template>

<!-- uc: Convert parameter 'value' to upper-case (uc) -->
<template name="uc">
  <param name="value"/>
  <value-of select="translate($value, 'abcdefghijklmnopqrstuvwxyz', 'ABCDEFGHIJKLMNOPQRSTUVWXYZ')"/>
</template>

</stylesheet>
