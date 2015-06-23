# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  module RubyPrimitives

    def self.module_mirror(obj)
      if obj.is_a?(::Numeric)
        Rubinius::Mirror::Numeric
      else
        begin
          Rubinius::Mirror.const_get(obj.class.name.to_sym, false)
        rescue NameError
          ancestor = obj.class.superclass

          until ancestor.nil?
            begin
              return Rubinius::Mirror.const_get(ancestor.name.to_sym, false)
            rescue NameError
              ancestor = ancestor.superclass
            end
          end

          nil
        end
      end
    end

    Truffle::Primitive.install_rubinius_primitive self, :module_mirror

  end
end
