# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle

  module Interop

    def self.import_method(name)
      method = import(name.to_s)

      Object.class_eval do
        define_method(name.to_sym) do |*args|
          Truffle::Interop.execute(method, *args)
        end
      end
    end

    def self.export_method(name)
      export(name.to_s, Object.method(name.to_sym))
    end

  end

end
