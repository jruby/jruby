###### BEGIN LICENSE BLOCK ######
# Version: CPL 1.0/GPL 2.0/LGPL 2.1
#
# The contents of this file are subject to the Common Public
# License Version 1.0 (the "License"); you may not use this file
# except in compliance with the License. You may obtain a copy of
# the License at http://www.eclipse.org/legal/cpl-v10.html
#
# Software distributed under the License is distributed on an "AS
# IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
# implied. See the License for the specific language governing
# rights and limitations under the License.
#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
# Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
# Copyright (C) 2004-2005 Thomas E Enebo <enebo@acm.org>
# Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
# Copyright (C) 2005 Jason Foreman <jforeman@hark.org>
# 
# Alternatively, the contents of this file may be used under the terms of
# either of the GNU General Public License Version 2 or later (the "GPL"),
# or the GNU Lesser General Public License Version 2.1 or later (the "LGPL"),
# in which case the provisions of the GPL or the LGPL are applicable instead
# of those above. If you wish to allow use of your version of this file only
# under the terms of either the GPL or the LGPL, and not to allow others to
# use your version of this file under the terms of the CPL, indicate your
# decision by deleting the provisions above and replace them with the notice
# and other provisions required by the GPL or the LGPL. If you do not delete
# the provisions above, a recipient may use your version of this file under
# the terms of any one of the CPL, the GPL or the LGPL.
###### END LICENSE BLOCK ######

module JavaProxy
  attr :java_class, true
  attr :java_object, true

  @method_extenders = []

  def ==(rhs)
    return @java_object == rhs
  end
  
  def to_s
    @java_object.to_s
  end

  def eql?(rhs)
    self == rhs
  end
  
  def equal?(rhs)
    @java_object.equal?(rhs)
  end
  
  def hash()
    @java_object.hash()
  end
  
  def JavaProxy.add_method_extender(extender)
    @method_extenders << extender
  end
  
  def JavaProxy.convert_arguments(arguments)
    arguments.collect do |v|
      v = v.java_object if v.respond_to? :java_object
      Java.primitive_to_java(v)
    end
  end

  def convert_arguments(arguments)
    JavaProxy.convert_arguments(arguments)
  end
	
  alias_method :old_respond_to?, :respond_to?
  
  # Does 'method' respond to an existing method or one which will come into existence the
  # first time it is referenced?
  def respond_to?(method, include_priv=false)
    old_respond_to?(method, include_priv) || !find_java_methods(method.to_s).empty?
  end
  
  def create_method(java_methods)
    name = java_methods.keys.first
    meths = java_methods[name]
    self.class.send(:define_method, name) { |*args|
      args = convert_arguments(args)
      m = JavaUtilities.matching_method(meths.find_all {|m| m.arity == args.length}, args)
      result = Java.java_to_primitive(m.invoke(self.java_object, *args))
      result = JavaUtilities.wrap(result) if result.kind_of?(Java::JavaObject)
      result
    }
    JavaProxy.extend_method(self.class, name, meths)
    name
  end

  # Lazily create a java method or defer to default method_missing behavior
  def method_missing(name, *args)
    method = find_java_methods(name.to_s)
    return super(name, *args) if method.empty?
    self.send(self.create_method(method), *args)
  end

  private

  def JavaProxy.extend_method(proxy_class, name, method)
    @method_extenders.each { |x| x.extend_method(proxy_class, name, method) }
  end

  # Find all java methods matching name (or its java beans inflected forms) and return them
  # as a Hash (or an empty one if no matches are found).
  def find_java_methods(name)
    names = inflect_method_name(name)
    @java_class.java_instance_methods.select {|m| names.include?(m.name) }.group_by {|m| m.name }
  end

  def inflect_method_name(name)
    names = [name]
    
    # getters and setters called by proper name
    return names if name =~ /^([gs]et[A-Z]|[^a-zA-Z])/
   
    sub_name = name.split(/(\?|=)/).first.sub!(/^./) { |c| c.upcase }
		
    if name =~ /=$/      # setter method name: foo= -> setFoo
      names << 'set' + sub_name 
    elsif name =~ /\?$/  # getter method name: foo? -> isFoo
      names << 'is' + sub_name
    else                 # ordinary method name: foo -> getFoo, isFoo
      names << 'get' + sub_name << 'is' + sub_name
    end
  end
end


module JavaUtilities
  @proxy_classes = {}
  @proxy_extenders = []
  
 def JavaUtilities.add_proxy_extender extender
    @proxy_extenders << extender
  end
  
  def JavaUtilities.extend_proxy(proxy_class, java_class)
    @proxy_extenders.each {|e| e.extend_proxy(proxy_class, java_class)}
  end

  def JavaUtilities.valid_constant_name?(name)
    return false if name.empty?
    first_char = name[0..0]
    return (first_char == first_char.upcase && 
	    first_char != first_char.downcase)
  end

  def JavaUtilities.get_proxy_class(java_class)
    java_class = Java::JavaClass.for_name(java_class) if java_class.kind_of?(String)
    class_id = java_class.id

    unless @proxy_classes[class_id]
      proxy_class = Class.new
      proxy_class.class_eval(<<END)
        include(JavaProxy)
        @java_class = java_class
        @java_object = java_class
END

      @proxy_classes[class_id] = proxy_class

      setup_proxy_class(java_class, proxy_class)
      extend_proxy(proxy_class, java_class)
    end

    @proxy_classes[class_id]
  end

  def JavaUtilities.get_java_class(name)
    begin
      return Java::JavaClass.for_name(name)
    rescue NameError
      return nil
    end
  end
  
  def JavaUtilities.create_proxy_class(constant, java_class, mod)
    mod.const_set(constant.to_s, get_proxy_class(java_class))
  end
  
  def JavaUtilities.access(java_type)
    if java_type.public?
      "public"
    elsif java_type.protected?
      "protected"
    else
      "private"
    end
  end
  
  def JavaUtilities.print_class(java_type, indent="")
     while (!java_type.nil? && java_type.name != "java.lang.Class")
        puts "#{indent}Name:  #{java_type.name}, access: #{ JavaUtilities.access(java_type) }  Interfaces: "
        java_type.interfaces.each { |i| print_class(i, "  #{indent}") }
        puts "#{indent}SuperClass: "
        print_class(java_type.superclass, "  #{indent}")
        java_type = java_type.superclass
     end
  end
  
  # We could have a private class which implements more than one interface.
  # I am working on a new way of creating proxies and I am not sure this will be there in that
  # version.  This heuristic works with all the standard java libraries we use.
  def JavaUtilities.narrowest_type(java_type)
    # We want narrowest java class as wrapped type (e.g. Foo.clone should wrap a Foo if public)
    # Also note this will not be an endless loop since we will always reach Object
    while (!java_type.public?) do
      java_type.interfaces.each {|i| return i if i.public? }
      java_type = java_type.superclass
    end
    
    java_type
  end

  def JavaUtilities.wrap(java_object)
    real_type = java_object.java_class
    java_class = JavaUtilities.narrowest_type(real_type)
    
    proxy_class = get_proxy_class(java_class)
    proxy = proxy_class.new_proxy
    proxy.java_class = java_class
    proxy.java_object = java_object
    proxy
  end

  def JavaUtilities.setup_proxy_class(java_class, proxy_class)
    class << proxy_class
      alias_method(:new_proxy, :new)
      attr_reader :java_class
      attr_reader :java_object

      # Carry the Java class as an instance variable on the
      # derived classes too, otherwise their constructors
      # won't work.
      def inherited(subclass)
        java_class = @java_class
        subclass.class_eval("@java_class = java_class")
      end
    end

    if java_class.interface?
      create_interface_constructor(java_class, proxy_class)
      create_array_type_accessor(java_class, proxy_class)
    elsif java_class.array?
      create_array_class_constructor(java_class, proxy_class)
    else
      create_class_constructor(java_class, proxy_class)
      create_array_type_accessor(java_class, proxy_class)
    end

    if java_class.array?
      setup_array_methods(java_class, proxy_class)
    else
      setup_attributes(java_class, proxy_class)
      setup_class_methods(java_class, proxy_class)
      setup_constants(java_class, proxy_class)
      setup_inner_classes(java_class, proxy_class)
    end
    return proxy_class
  end

  def JavaUtilities.create_class_constructor(java_class, proxy_class)
    class << proxy_class
      def new(*args)
        constructors = @java_class.constructors.select {|c| c.arity == args.length }
        if constructors.empty?
          raise NameError.new("wrong # of arguments for constructor")
        end
        args = JavaProxy.convert_arguments(args)
        constructor = JavaUtilities.matching_method(constructors, args)
        java_object = constructor.new_instance(*args)
        result = new_proxy
        result.java_class = @java_class
        result.java_object = java_object
        result
      end
    end
  end
  
  def JavaUtilities.create_array_class_constructor(java_class, proxy_class)
    class << proxy_class
      def new(size)
        java_object = java_class.component_type.new_array(size)
        result = new_proxy
        result.java_class = @java_class
        result.java_object = java_object
        result
      end
    end
  end

  def JavaUtilities.create_array_type_accessor(java_class, proxy_class)
    class << proxy_class
      def []
        JavaUtilities.get_proxy_class(java_class.array_class)
      end
    end
  end

  def JavaUtilities.create_interface_constructor(java_class, proxy_class)
    proxy_class.class_eval do
      def initialize
        self.java_object = Java.new_proxy_instance(self.class.java_class) { |proxy, method, *java_args|
          java_args.collect! { |arg| Java.java_to_primitive(arg) }
          args = []
          java_args.each_with_index { |arg, idx|
            arg = JavaUtilities.wrap(arg) if arg.kind_of?(Java::JavaObject)
            args[idx] = arg
          }
          result = self.__send__(method.name, *args)
          result = result.java_object if result.kind_of?(JavaProxy)
          Java.primitive_to_java(result)
        }
      end
    end
  end

  def JavaUtilities.setup_array_methods(java_class, proxy_class)
    def proxy_class.create_array_methods(java_class)
      include Enumerable
      define_method(:[]) {|index|
        value = java_object[index]
        value = Java.java_to_primitive(value)
        value = JavaUtilities.wrap(value) if value.kind_of?(Java::JavaObject)
        value
      }
      define_method(:[]=) {|index, value|
        value = JavaProxy.convert_arguments([value]).first
        java_object[index] = value
        value
      }
      define_method(:length) {| |
        java_object.length
      }
      class_eval(<<END
        def each()
          block = Proc.new
          for index in 0...java_object.length
            value = self[index]
            block.call(value)
          end
        end
END
        )
    end
    proxy_class.create_array_methods(java_class)
  end

  def JavaUtilities.matching_method(methods, args)
    # Only one method to match
    return methods.first if methods.length == 1

    arg_types = args.collect {|a| a.java_class }

    exact_match = methods.detect {|m|
      m.argument_types == arg_types
    }
    return exact_match unless exact_match.nil?
    compatible_match = methods.detect {|m|
      types = m.argument_types
      match = true
      0.upto(types.length - 1) {|i|
        unless types[i].assignable_from?(arg_types[i])
          match = false
        end
      }
      match
    }
    return compatible_match unless compatible_match.nil?

    if methods.first.kind_of?(JavaConstructor)
      raise NameError.new("no constructor with arguments matching " +
                          arg_types.inspect)
    else
      raise NameError.new("no method '" + methods.first.name +
                          "' with argument types matching " +
                          arg_types.inspect)
    end
  end

  def JavaUtilities.create_single_class_method(proxy_class, name, method)
    proxy_class.class_eval("def self." + name + "(*args);" + <<-END
        args = JavaProxy.convert_arguments(args)
        method = @class_methods['#{name}'].first
        return_type = method.return_type
        result = Java.java_to_primitive(method.invoke_static(*args))
        result = JavaUtilities.wrap(result) if result.kind_of?(Java::JavaObject)
        result
      end
    END
    )
  end

  def JavaUtilities.create_matched_class_methods(proxy_class, name, methods)
    proxy_class.class_eval("def self." + name + "(*args);" + <<-END
        methods = @class_methods['#{name}']
        args = JavaProxy.convert_arguments(args)
        m = JavaUtilities.matching_method(methods.find_all {|m| m.arity == args.length }, args)
        result = m.invoke_static(*args)
        result = Java.java_to_primitive(result)
        result = JavaUtilities.wrap(result) if result.kind_of?(Java::JavaObject)
        result
      end
    END
    )
  end

  def JavaUtilities.setup_class_methods(java_class, proxy_class)
    public_methods = java_class.java_class_methods.select {|m| m.public? }
    grouped_methods = public_methods.group_by {|m| m.name }
    proxy_class.class_eval("@class_methods = grouped_methods")
    # FIXME: error handling, arity awareness, ...
    grouped_methods.each {|name, methods|
      if methods.length == 1
        create_single_class_method(proxy_class, name, methods.first)
      else
        create_matched_class_methods(proxy_class, name, methods)
      end
    }
  end

  def JavaUtilities.setup_attributes(java_class, proxy_class)
    def proxy_class.create_attribute_methods(java_class)
      fields = java_class.fields.collect {|name| java_class.field(name) }
      instance_methods = java_class.java_instance_methods.collect {|m| m.name}
      attrs = fields.select {|field| field.public? && !field.static? }

      attrs.each do |attr|
	    # Do not add any constants that has same-name method
	    next if instance_methods.detect {|m| m == attr.name }

	    define_method(attr.name) do |*args|
	      result = Java.java_to_primitive(attr.value(@java_object))
	      result = JavaUtilities.wrap(result) if result.kind_of?(Java::JavaObject)
	      result
	    end

	    next if attr.final?

	    define_method("#{attr.name}=") do |*args|
          args = JavaProxy.convert_arguments(args)
	      result = Java.java_to_primitive(attr.set_value(@java_object, args.first))
	      result = JavaUtilities.wrap(result) if result.kind_of?(Java::JavaObject)
	      result
	    end
	  end
    end
    proxy_class.create_attribute_methods(java_class)
  end

  def JavaUtilities.setup_constants(java_class, proxy_class)
    fields = java_class.fields.collect {|name| java_class.field(name) }
    constants = fields.select {|field|
      field.static? and field.final? and JavaUtilities.valid_constant_name?(field.name)
    }
    constants.each {|constant|
      value = Java.java_to_primitive(constant.static_value)
      proxy_class.const_set(constant.name, value)
    }
    naughty_constants = fields.select {|field|
      field.static? and field.final? and !JavaUtilities.valid_constant_name?(field.name)
    }

    class_methods = java_class.java_class_methods.collect { |m| m.name }

    naughty_constants.each do |constant|
      # Do not add any constants that has same-name class method
      next if class_methods.detect {|m| m == constant.name }

      proxy_class.class_eval(<<-END
	    def self.#{constant.name}(*args)
	      result = @java_class.field('#{constant.name}').static_value
          result = Java.java_to_primitive(result)
          result = JavaUtilities.wrap(result) if result.kind_of?(Java::JavaObject)
          result
        end
      END
      );
    end
  end

  def JavaUtilities.setup_inner_classes(java_class, proxy_class)
    def proxy_class.const_missing(constant)
      inner_class = nil
      begin
        inner_class = Java::JavaClass.for_name(@java_class.name + '$' + constant.to_s)
      rescue NameError
        return super
      end
      JavaUtilities.create_proxy_class(constant, inner_class, self)
    end
  end
end

# Extensions to the standard Module package.

class Module
  private

  ##
  # Includes a Java package into this class/module. The Java classes in the
  # package will become available in this class/module, unless a constant
  # with the same name as a Java class is already defined.
  #

  def include_package(package_name)
    if defined? @included_packages
      @included_packages << package_name      
      return
    end
    @included_packages = [package_name]
    @java_aliases = {} unless @java_aliases

    def self.const_missing(constant)
      real_name = @java_aliases[constant]
      real_name = constant unless real_name

      java_class = nil
      return super unless @included_packages.detect {|package|
          java_class = JavaUtilities.get_java_class(package + '.' + real_name.to_s)
      }
      
      JavaUtilities.create_proxy_class(constant, java_class, self)
    end
  end

  def java_alias(new_id, old_id)
    @java_aliases[new_id] = old_id
  end
end

class Object
  def include_class(include_class)
    class_names = include_class.to_a

    class_names.each do |full_class_name|
      package_name, class_name = full_class_name.match(/((.*)\.)?([^\.]*)/)[2,3]

      if block_given?
        constant = yield(package_name, class_name)
      else
        constant = class_name
      end

      if (respond_to?(:class_eval, true))
        class_eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")")
      else
        eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")")
      end
    end
  end
end


class JavaInterfaceExtender
  def initialize(javaClassName, &defBlock)
    @java_class = Java::JavaClass.for_name(javaClassName)
    @defBlock = defBlock
  end
  
  def extend_proxy(proxy_class, java_class)
    if @java_class.assignable_from? java_class
        proxy_class.class_eval &@defBlock
    end
  end
end

require 'builtin/java/beans'
require 'builtin/java/exceptions'
require 'builtin/java/collections'
