# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Rubinius
  class Mirror
    class Range < Mirror

      # Rubinius's definition for the `excl` method relies on its internal object representation.  We use a different
      # representation, so we must override the method with a compatible implementation.
      def excl
        @object.exclude_end?
      end

      # Local fix until Rubinius is fixed upstream.
      def step_float_iterations_size(first, last, step_size)
        err = (first.abs + last.abs + (last - first).abs) / step_size.abs * Float::EPSILON
        err = 0.5 if err > 0.5

        if excl
          iterations = ((last - first) / step_size - err).floor
          iterations += 1 if iterations * step_size + first < last
        else
          iterations = ((last - first) / step_size + err).floor + 1
        end

        iterations
      end

    end
  end
end
