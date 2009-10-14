/**
 * **** BEGIN LICENSE BLOCK *****
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
 * Copyright (C) 2009 Yoko Harada <yokolet@gmail.com>
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
 * **** END LICENSE BLOCK *****
 */
package org.jruby.embed;

import org.jruby.RubyObjectAdapter;
import org.jruby.runtime.Block;

/**
 * Wrapper interface of RubyObjectAdapter for embedding. Methods' arguments can have
 * simple Java objects for easiness. Each methods converts returned object
 * to a Java type specifed in the argumnet.
 *
 * @author Yoko Harada <yokolet@gmail.com>
 */
public interface EmbedRubyObjectAdapter extends RubyObjectAdapter {
    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument.
     * 
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be called
     * @param returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    <T> T callMethod(Object receiver, String methodName, Class<T> returnType);

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have only one argument.
     * 
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be called
     * @param singleArg is an method argument
     * @param returnType returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    <T> T callMethod(Object receiver, String methodName, Object singleArg, Class<T> returnType);

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments.
     *
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be called
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType);

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments, one of which is a block.
     * 
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be called
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @return an instance of requested Java type
     */
    <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType);

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method does not have any argument, and users want to inject Ruby's local
     * variables' values from Java.
     * 
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be called
     * @param returnType is the type we want it to convert to
     * @param unit is parsed unit
     * @return an instance of requested Java type
     */
    <T> T callMethod(Object receiver, String methodName, Class<T> returnType, EmbedEvalUnit unit);

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments, and users want to inject Ruby's local
     * variables' values from Java.
     * 
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be calleid
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @param unit is parsed unit
     * @return an instance of requested Java type
     */
    <T> T callMethod(Object receiver, String methodName, Object[] args, Class<T> returnType, EmbedEvalUnit unit);

    /**
     * Executes a method defined in Ruby script. This method is used when a Ruby
     * method have multiple arguments, one of which is a block, and users want to
     * inject Ruby's local variables' values from Java.
     * 
     * @param receiver is an instance that will receive this method call
     * @param methodName is a method name to be calleid
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @param unit is parsed unit
     * @return is the type we want it to convert to
     */
    <T> T callMethod(Object receiver, String methodName, Object[] args, Block block, Class<T> returnType, EmbedEvalUnit unit);

    /**
     * 
     * @param receiver is an instance that will receive this method call
     * @param args is an array of method arguments
     * @param returnType is the type we want it to convert to
     * @return is the type we want it to convert to
     */
    <T> T callSuper(Object receiver, Object[] args, Class<T> returnType);

    /**
     * 
     * @param receiver is an instance that will receive this method call
     * @param args is an array of method arguments except a block
     * @param block is a block to be executed in this method
     * @param returnType is the type we want it to convert to
     * @return is the type we want it to convert to
     */
    <T> T callSuper(Object receiver, Object[] args, Block block, Class<T> returnType);
}
