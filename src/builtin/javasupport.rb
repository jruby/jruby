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

module JavaProxy
  attr :java_class, true

  def convert_arguments(arguments)
    arguments.collect {|v| Java.primitive_to_java(v) }
  end
end

# Extensions to existing classes and modules

class Module
  private

  ##
  # Includes a Java package into this class/module. The Java classes in the
  # package will become available in this class/module, unless a constant
  # with the same name as a Java class is already defined.
  #

  def include_package(package_name)
    unless defined? @included_packages
      @included_packages = []
    end
    @included_packages << package_name      
    def self.const_missing(constant)
      java_class = find_java_class(constant)
      if java_class.nil?
        return super
      end
      load_java_class(constant, java_class)
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

  def find_java_class(constant)
    @included_packages.detect {|package|
      begin
        class_name = package + '.' + constant.to_s
        return Java::JavaClass.for_name(class_name)
      rescue NameError
        return nil
      end
    }
  end

  def load_java_class(constant, java_class)
    # Create proxy class
    self.module_eval("class " + constant.to_s + "; include JavaProxy; end")
    proxy_class = eval(self.name + '::' + constant.to_s)
    proxy_class.class_eval("@java_class = java_class")

    # FIXME: take types into consideration, like the old javasupport,
    #        and do the searching long before call-time.
    def proxy_class.new(*args)
      arity = args.length
      constructor = @java_class.constructors.detect {|c| c.arity == arity }
      if constructor.nil?
        raise NameError.new("wrong # of arguments for constructor")
      end
      result = constructor.new_instance(self, *args)
      result.java_class = @java_class
      result
    end

    def proxy_class.create_methods(java_class)
      public_methods =
        java_class.java_instance_methods.select {|m| m.public? }
      grouped_methods = public_methods.group_by {|m| m.name }
      grouped_methods.each {|name, methods|
        if methods.length == 1
          m = methods.first
          define_method(m.name) {|*args|
            args = convert_arguments(args)
            m.invoke(self, *args)
          }
        else
          methods_by_arity = methods.group_by {|m| m.arity }
          methods_by_arity.each {|arity, same_arity_methods|
            if same_arity_methods.length == 1
              # just one method with this length
              define_method(name) {|*args|
                m = methods_by_arity[args.length].first
                args = convert_arguments(args)
                m.invoke(self, *args)
              }
            else
              # overloaded on same length
              raise "java methods only differing on argument types not supported yet: #{name}"
            end
          }
        end
      }
    end
    proxy_class.create_methods(java_class)

    return proxy_class
  end
end


if __FILE__ == $0

  class JavaUtil
    include_package "java.util"
  end

  p JavaUtil::Random

  r = JavaUtil::Random.new

  p r.to_s
  p r.type.instance_methods

  p r.nextInt
  p r.nextInt(10)
  p r.nextInt
end
