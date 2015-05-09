# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  module Type

    def self.const_get(mod, name, inherit=true, resolve=true)
      raise "unsupported" unless resolve
      mod.const_get name, inherit
    end

    def self.const_exists?(mod, name, inherit = true)
      mod.const_defined? name, inherit
    end

  end
end
