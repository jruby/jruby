# Copyright (c) 2015, 2016 Oracle and/or its affiliates. All rights reserved. This
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

    end
  end
end
