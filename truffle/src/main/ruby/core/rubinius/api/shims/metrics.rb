# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius

  module Metrics

    def self.data
      {
        :'gc.young.count' => 0,
        :'gc.young.ms' => 0,
        :'gc.immix.concurrent.ms' => 0,
        :'gc.immix.count' => Truffle.gc_count,
        :'gc.immix.stop.ms' => Truffle.gc_time,
        :'gc.large.sweep.us' => 0,
        :'memory.young.bytes' => 0,
        :'memory.young.objects' => 0,
        :'memory.immix.bytes' => 0,
        :'memory.immix.objects' => 0,
        :'memory.large.bytes' => 0,
        :'memory.promoted.bytes' => 0,
        :'memory.promoted.objects' => 0,
        :'memory.symbols.bytes' => 0,
        :'memory.code.bytes' => 0
      }
    end

  end

end
