# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
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
        :'gc.young.last.ms' => 0,
        :'gc.young.total.ms' => 0,
        :'gc.immix.concurrent.last.ms' => 0,
        :'gc.immix.concurrent.total.ms' => 0,
        :'gc.immix.count' => Truffle::Primitive.gc_count,
        :'gc.immix.stop.last.ms' => 0,
        :'gc.immix.stop.total.ms' => Truffle::Primitive.gc_time,
        :'gc.large.sweep.total.ms' => 0,
        :'memory.young.bytes.current' => 0,
        :'memory.young.bytes.total' => 0,
        :'memory.young.objects.total' => 0,
        :'memory.immix.bytes.current' => 0,
        :'memory.immix.bytes.total' => 0,
        :'memory.immix.objects.total' => 0,
        :'memory.large.bytes.current' => 0,
        :'memory.promoted.bytes.total' => 0,
        :'memory.promoted.objects.total' => 0,
        :'memory.symbols.bytes' => 0,
        :'memory.code.bytes' => 0
      }
    end

  end

end
