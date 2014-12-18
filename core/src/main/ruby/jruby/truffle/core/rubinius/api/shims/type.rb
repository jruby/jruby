# Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  module Type

    # I think Rubinius defines this in C++

    def coerce_to_collection_index(index)
      if object_kind_of? index, Fixnum
        index
      else
        raise TypeError, "no implicit conversion of #{index.class.name} into Integer"
      end
    end

    module_function :coerce_to_collection_index

  end
end
