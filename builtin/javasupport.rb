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
end

# Extensions to existing classes and modules

module Java
  class JavaClass

    def grouped_instance_methods(&block)
      methods = java_instance_methods.select {|m| m.public? }
      by_name = {}
      methods.each {|m|
        if by_name.has_key?(m.name)
          by_name[m.name] << m
        else
          by_name[m.name] = [m]
        end
      }
      by_name.each(&block)
    end
  end
end


class Module
  private
  def include_package(package)
    unless defined? @included_packages
      @included_packages = []
    end
    @included_packages << package

    def self.constant_missing(constant)
      java_class = nil
      @included_packages.detect {|package|
        begin
          class_name = package + '.' + constant.to_s
          java_class = Java::JavaClass.for_name(class_name)
          true
        rescue NameError
          false
        end
      }
      if java_class.nil?
        return super
      end

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
        # FIXME: deal with overloaded methods
        java_class.grouped_instance_methods {|name, methods|
          if methods.length == 1
            m = methods[0]
            define_method(m.name) {|*args|
              m.invoke(self, *args)
            }
          else
            raise "Didn't think you would get away with overloading, now did you!"
          end
        }
      end
      proxy_class.create_methods(java_class)

      return proxy_class
    end
  end

  def remove_package(package)
    if defined? @included_packages
      @included_packages.delete(package)
    end
  end
end


if __FILE__ == $0

  class Froboz
    include_package "java.util"
  end

  p Froboz::Random

  r = Froboz::Random.new

  p r.to_s
  p r.type.instance_methods

  p r.nextInt
  p r.nextInt
  p r.nextInt
end
