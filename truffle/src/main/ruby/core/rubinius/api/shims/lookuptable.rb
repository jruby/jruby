# Copyright (c) 2014, 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  class LookupTable < Hash
    alias_method :lookup_orig, :[]

    def [](key)
      lookup_orig(key_to_sym(key))
    end

    private

    # Taken from Rubinius LookupTable.
    def key_to_sym(key)
      key.kind_of?(String) ? key.to_sym : key
    end
  end
end
