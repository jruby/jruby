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
# Copyright (C) 2004 Thomas E Enebo <enebo@acm.org>
# Copyright (C) 2004 David Corbin <dcorbin@users.sourceforge.net>
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

  def JavaProxy.convert_arguments(arguments)
    arguments.collect {|v|
      if v.respond_to?('java_object')
	    v = v.java_object
      end
      Java.primitive_to_java(v)
    }
  end

  def convert_arguments(arguments)
    JavaProxy.convert_arguments(arguments)
  end
end


module JavaUtilities
  @proxy_classes = {}

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

  def JavaUtilities.wrap(java_object, return_type)
    real_type = java_object.java_class
    java_class = real_type.public? ? real_type : return_type

    proxy_class = get_proxy_class(java_class)
    proxy = proxy_class.new_proxy
    proxy.java_class = java_class
    proxy.java_object = java_object

    unless real_type.public?
      # If the instance is private, but implements public interfaces
      # then we want the methods on those available.

      public_interfaces = real_type.interfaces.select { |interface| interface.public? }
      interface_proxies = public_interfaces.collect { |interface| get_proxy_class(interface) }
      interface_proxies.each { |interface_proxy| proxy.extend(interface_proxy) }
    end

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
      setup_instance_methods(java_class, proxy_class)
      setup_attributes(java_class, proxy_class)
      setup_class_methods(java_class, proxy_class)
      setup_constants(java_class, proxy_class)
      setup_inner_classes(java_class, proxy_class)
    end
    return proxy_class
  end

  class << self

    def create_class_constructor(java_class, proxy_class)
      class << proxy_class
        def new(*args)
          constructors =
            @java_class.constructors.select {|c| c.arity == args.length }
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

    def create_array_class_constructor(java_class, proxy_class)
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

    def create_array_type_accessor(java_class, proxy_class)
      class << proxy_class
        def []
          JavaUtilities.get_proxy_class(java_class.array_class)
        end
      end
    end

    def create_interface_constructor(java_class, proxy_class)
      class << proxy_class
        def new
	  Java.new_proxy_instance(@java_class) {
	    |proxy, method, *java_args|
	    java_args.collect! { |arg|
	      Java.java_to_primitive(arg)
	    }
	    args = []
	    java_args.each_with_index { |arg, idx|
	      if arg.kind_of?(JavaObject)
    		arg = JavaUtilities.wrap(arg, method.argument_types[idx])
          end
	      args[idx] = arg
	    }
	    result = proxy.__send__(method.name, *args)
	    result = result.java_object if result.kind_of?(JavaProxy)
	    Java.primitive_to_java(result)
	  }
	end
      end
    end

    def setup_array_methods(java_class, proxy_class)
      def proxy_class.create_array_methods(java_class)
        include Enumerable
        define_method(:[]) {|index|
          value = java_object[index]
          value = Java.java_to_primitive(value)
          if value.kind_of?(JavaObject)
            value = JavaUtilities.wrap(value, java_class.component_type)
          end
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

    def setup_instance_methods(java_class, proxy_class)
      # TODO: Come up with cleaner way for defining methods for a particular interface
      # TODO: Default and Block Comparator should only get defined once
      def proxy_class.create_instance_methods(java_class)
        if Java::JavaClass.for_name('java.lang.Exception').assignable_from? java_class
          class <<self
            def ===(rhs)
              (NativeException == rhs.class) && (java_class.assignable_from?(rhs.cause.java_class))
            end
          end
        end
        if Java::JavaClass.for_name('java.util.Map').assignable_from? java_class
          class_eval(<<END
            def each(&block)
              entrySet.each { |pair| block.call(pair.key, pair.value) }
            end
END
          )
        elsif Java::JavaClass.for_name('java.util.List').assignable_from? java_class
          class_eval(<<END
            include Enumerable

            def each(&block)
              0.upto(size-1) { |index| block.call(get(index)) }
            end
            def <<(a); add(a); end
            def sort()
              include_class 'java.util.ArrayList'
              include_class 'java.util.Collections'
              include_class 'java.util.Comparator'

              comparator = Comparator.new

              if block_given?
                class << comparator
                  def compare(o1, o2); yield(o1, o2); end
                end
              else
                class << comparator
                  def compare(o1, o2); o1 <=> o2; end
                end
              end

              list = ArrayList.new
              list.addAll(self)

              Collections.sort(list, comparator)

              list
            end
            def sort!()
              include_class 'java.util.Collections'
              include_class 'java.util.Comparator'

              comparator = Comparator.new

              if block_given?
                class << comparator
                  def compare(o1, o2); yield(o1, o2); end
                end
              else
                class << comparator
                  def compare(o1, o2); o1 <=> o2; end;
                end
              end

              Collections.sort(java_object, comparator)

              self
            end
            def construct()
              include_class 'java.util.ArrayList'
      
              ArrayList.new
            end
            def _wrap_yield(*args)
              p = yield(*args)
              p p
            end
END
          )
        elsif Java::JavaClass.for_name('java.util.Set').assignable_from? java_class
          class_eval(<<END
            def each(&block)
              iter = iterator
              while iter.hasNext
                block.call(iter.next)
              end
            end
END
          )
        end
        if Java::JavaClass.for_name('java.lang.Comparable').assignable_from? java_class
          class_eval('include Comparable; def <=>(a); compareTo(a); end')
        end
        java_class.java_instance_methods.select { |m| 
          m.public? 
        }.group_by { |m| 
          m.name 
        }.each { |name, methods|
          # Define literal java method (e.g. getFoo())
          define_method(name) do |*args|
            args = convert_arguments(args)
            m = JavaUtilities.matching_method(methods.find_all {|m|
              m.arity == args.length}, args)
            result = m.invoke(self.java_object, *args)
            result = Java.java_to_primitive(result)
            if result.kind_of?(JavaObject)
              result = JavaUtilities.wrap(result, m.return_type)
            end
            result
	      end
	      
	      # Lets treat javabean properties like ruby attributes
	      case name[0,4]
	      when /get./
	        newName = name[3..-1].downcase!
            next if instance_methods.member?(newName)

	        define_method(newName) do |*args|
              args = convert_arguments(args)
              m = JavaUtilities.matching_method(methods.find_all {|m|
                m.arity == args.length}, args)
              result = m.invoke(self.java_object, *args)
              result = Java.java_to_primitive(result)
              if result.kind_of?(JavaObject)
                result = JavaUtilities.wrap(result, m.return_type)
              end
              result
	        end
	      when /set./
	      	define_method(name[3..-1].downcase!.concat('=')) do |*args|
              args = convert_arguments(args)
              m = JavaUtilities.matching_method(methods.find_all {|m|
                m.arity == args.length}, args)
              result = m.invoke(self.java_object, *args)
              result = Java.java_to_primitive(result)
              if result.kind_of?(JavaObject)
                result = JavaUtilities.wrap(result, m.return_type)
              end
              result
	        end
	      end
	    }
      end
      proxy_class.create_instance_methods(java_class)
    end

    def matching_method(methods, args)
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


    def create_single_class_method(proxy_class, name, method)
      proxy_class.class_eval("def self." + name + "(*args);" +
                             <<END
                                 args = JavaProxy.convert_arguments(args)
                                 methods = @class_methods['#{name}']
                                 method = methods.first
                                 return_type = method.return_type
                                 result = Java.java_to_primitive(method.invoke_static(*args))
                                 if result.kind_of?(JavaObject)
                                   result = JavaUtilities.wrap(result, method.return_type)
                                 end
                                 result
                                 end
END
                             )
    end

    def create_matched_class_methods(proxy_class, name, methods)
      proxy_class.class_eval("def self." + name + "(*args);" +
                             <<END
            methods = @class_methods['#{name}']
            args = JavaProxy.convert_arguments(args)
            m = JavaUtilities.matching_method(methods.find_all {|m|
	            m.arity == args.length
                  }, args)
            result = m.invoke_static(*args)
            result = Java.java_to_primitive(result)
            if result.kind_of?(JavaObject)
              result = JavaUtilities.wrap(result, m.return_type)
            end
            result
          end
END
                             )
    end

    def setup_class_methods(java_class, proxy_class)
      public_methods =
        java_class.java_class_methods.select {|m| m.public? }
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

    def setup_attributes(java_class, proxy_class)
      def proxy_class.create_attribute_methods(java_class)
        fields = java_class.fields.collect {|name| java_class.field(name) }
        instance_methods = 
	  java_class.java_instance_methods.collect {|m| m.name}
	attrs = fields.select {|field| field.public? && !field.static? }

	attrs.each do |attr|
	  # Do not add any constants that has same-name method
	  next if instance_methods.detect {|m| m == attr.name }

	  define_method(attr.name) do |*args|
	    result = Java.java_to_primitive(attr.value(@java_object))
	    if result.kind_of?(JavaObject)
	      result = JavaUtilities.wrap(result, result.java_class)
	    end
	    result
	  end

	  next if attr.final?

	  define_method("#{attr.name}=") do |*args|
            args = JavaProxy.convert_arguments(args)
	    result = Java.java_to_primitive(attr.set_value(@java_object, 
							   args.first))
	    if result.kind_of?(JavaObject)
	      result = JavaUtilities.wrap(result, result.java_class)
	    end
	    result
	  end
	end
      end
      proxy_class.create_attribute_methods(java_class)
    end

    def setup_constants(java_class, proxy_class)
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

        proxy_class.class_eval(<<END
	  def self.#{constant.name}(*args)
	    result = @java_class.field('#{constant.name}').static_value
            result = Java.java_to_primitive(result)
            if result.kind_of?(JavaObject)
               result = JavaUtilities.wrap(result, result.java_class)
            end
            result
          end
END
        );
      end
    end

    def setup_inner_classes(java_class, proxy_class)
      def proxy_class.const_missing(constant)
        inner_class = nil
        begin
          inner_class =
            Java::JavaClass.for_name(@java_class.name + '$' + constant.to_s)
        rescue NameError
          return super
        end
        JavaUtilities.create_proxy_class(constant, inner_class, self)
      end
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

      eval("#{constant} = JavaUtilities.get_proxy_class(\"#{full_class_name}\")")
    end
  end
end
