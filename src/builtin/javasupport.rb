#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
# Copyright (C) 2002 Jan Arne Petersen <jpetersen@uni-bonn.de>
#
# JRuby - http://jruby.sourceforge.net
#
# This file is part of JRuby
#
# JRuby is free software; you can redistribute it and/or
# modify it under the terms of the GNU General Public License as
# published by the Free Software Foundation; either version 2 of the
# License, or (at your option) any later version.
#
# JRuby is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU General Public
# License along with JRuby; if not, write to
# the Free Software Foundation, Inc., 59 Temple Place, Suite 330,
# Boston, MA  02111-1307 USA

# $Revision$

module JavaProxy
  attr :java_class, true
  attr :java_object, true

  def JavaProxy.convert_arguments(arguments)
    arguments.collect {|v|
      if v.kind_of?(JavaProxy)
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
  class << self

    def create_proxy_class(constant, java_class, mod)
      mod.module_eval("class " + constant.to_s + "; include JavaProxy; end")
      proxy_class = eval(mod.name + '::' + constant.to_s)
      proxy_class.class_eval("@java_class = java_class")
      setup_proxy_class(java_class, proxy_class)
    end

    def wrap(java_object, return_type_name)
      return_type = Java::JavaClass.for_name(return_type_name)
      real_type = Java::JavaClass.for_name(java_object.java_type)

      if real_type.public?
        java_class = real_type
      else
        java_class = return_type
      end

      proxy_class = new_proxy_class(java_class.name)
      proxy = proxy_class.new_proxy
      proxy.java_class = java_class
      proxy.java_object = java_object
      proxy
    end

    def proxy_classes
      unless defined? @proxy_classes
        @proxy_classes = {}
      end
      @proxy_classes
    end

    def new_proxy_class(java_class_name)
      proxy_classes = JavaUtilities.proxy_classes

      if proxy_classes.has_key?(java_class_name)
        return proxy_classes[java_class_name]
      end

      java_class = Java::JavaClass.for_name(java_class_name)

      proxy_class = Class.new
      proxy_class.class_eval { include(JavaProxy) }

      proxy_class.class_eval("@java_class = java_class")
      proxy_classes[java_class_name] = proxy_class
      setup_proxy_class(java_class, proxy_class)
    end

    def create_constructor(java_class, proxy_class)
      class << proxy_class
        def new(*args)
          # FIXME: take types into consideration, like the old javasupport,
          #        and do the searching long before call-time.
          arity = args.length
          constructor = @java_class.constructors.detect {|c| c.arity == arity }
          if constructor.nil?
            raise NameError.new("wrong # of arguments for constructor")
          end
          args = JavaProxy.convert_arguments(args)
          java_object = constructor.new_instance(*args)
          result = new_proxy
          result.java_class = @java_class
          result.java_object = java_object
          result
        end
      end
    end

    def setup_proxy_class(java_class, proxy_class)

      class << proxy_class
        alias_method(:new_proxy, :new)
      end

      unless java_class.interface?
        create_constructor(java_class, proxy_class)
      end

      def proxy_class.create_instance_methods(java_class)
        public_methods =
          java_class.java_instance_methods.select {|m| m.public? }
        grouped_methods = public_methods.group_by {|m| m.name }
        grouped_methods.each {|name, methods|
          if methods.length == 1
            m = methods.first
            return_type = m.return_type
            define_method(m.name) {|*args|
              args = JavaProxy.convert_arguments(args)
              result = m.invoke(self.java_object, *args)
              result = Java.java_to_primitive(result)
              if result.kind_of?(JavaObject)
                result = JavaUtilities.wrap(result, m.return_type)
              end
              result
            }
          else
            methods_by_arity = methods.group_by {|m| m.arity }
            methods_by_arity.each {|arity, same_arity_methods|
              if same_arity_methods.length == 1
                # just one method with this length
                define_method(name) {|*args|
                  m = methods_by_arity[args.length].first
                  return_type = m.return_type
                  args = convert_arguments(args)
                  result = m.invoke(self.java_object, *args)
                  result = Java.java_to_primitive(result)
                  if result.kind_of?(JavaObject)
                    result = JavaUtilities.wrap(result, m.return_type)
                  end
                  result
                }
              else
                # overloaded on same length
                define_method(name) {
                  # FIXME
                  raise "java methods only differing on arg types not yet supported"
                }
              end
            }
          end
        }
      end
      proxy_class.create_instance_methods(java_class)

      public_methods =
        java_class.java_class_methods.select {|m| m.public? }
      grouped_methods = public_methods.group_by {|m| m.name }
      proxy_class.class_eval("@class_methods = grouped_methods")
      # FIXME: error handling, arity awareness, ...
      grouped_methods.each {|name, methods|
        proxy_class.class_eval("def self." + name + "(*args);" +
                               "args = JavaProxy.convert_arguments(args);" +
                               "methods = @class_methods['" + name + "'];" +
                               "method = methods.first;" +
                               "return_type = method.return_type;" +
                               "result = Java.java_to_primitive(method.invoke_static(*args));" +
                               "if result.kind_of?(JavaObject);" +
                               "  result = JavaUtilities.wrap(result, method.return_type);" +
                               "end;" +
                               "result;" +
                               "end")
      }

      fields = java_class.fields.collect {|name| java_class.field(name) }
      constants = fields.select {|field| field.static? and field.final? }
      constants.each {|field|
        name = field.name
        proxy_class.class_eval("def self. " + name + ";" +
                               "field = @java_class.field('" + name + "');" +
                               "result = field.static_value;" +
                               "result = Java.java_to_primitive(result);" +
                               "if result.kind_of?(JavaObject);" +
                               "  result = JavaUtilities.wrap(result, method.return_type);" +
                               "end;" +
                               "result;" +
                               "end")
      }

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

      return proxy_class
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

    def self.const_missing(constant)
      java_class = nil
      @included_packages.detect {|package|
        java_class = get_java_class(package + '.' + constant.to_s)
      }
      if java_class.nil?
        return super
      end
      JavaUtilities.create_proxy_class(constant, java_class, self)
    end
  end

  ##
  # Removes an imported Java package. No new Java classes will be loaded
  # from the package, but any Java classes that have already been
  # referenced will remain.
  #

  def remove_package(package_name)
    if defined? @included_packages
      @included_packages.delete(package_name)
    end
  end

  def get_java_class(name)
    begin
      return Java::JavaClass.for_name(name)
    rescue NameError
      return nil
    end
  end
end
