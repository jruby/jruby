#
# Copyright (C) 2002 Anders Bengtsson <ndrsbngtssn@yahoo.se>
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

class Module

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
        super
      end

      # Create proxy class
      self.module_eval("class " + constant.to_s + "; end")
      proxy_class = eval(self.name + '::' + constant.to_s)
      proxy_class.class_eval("@java_class = java_class")
      proxy_class.class_eval("def runtime_self; self; end")

      p proxy_class.instance_methods

      # FIXME: take types into consideration, like the old javasupport,
      #        and do the searching long before call-time.
      def proxy_class.new(*args)
        arity = args.length
        constructor = @java_class.constructors.detect {|c| c.arity == arity }
        if constructor.nil?
          raise NameError.new("wrong # of arguments for constructor")
        end
        constructor.new_instance(*args)
      end

      # FIXME: look though all the public methods and create suitable
      # proxy methods for them.
      def proxy_class.create_methods(java_class)
        toString_method = java_class.java_method(:toString)
        define_method(:to_s) {
          toString_method.invoke(runtime_self)
        }
      end
      proxy_class.create_methods(java_class)

      return proxy_class
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

end
