# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

module Truffle

  # Attach blocks to specific lines of existing code. Like
  # `Kernel#set_trace_func`, but for specific lines only, and fast enough to
  # be used for hot-code-path monkey patching if you want.
  module Attachments

    # Attach a block to be called each time before a given line in a given file
    # is executed, passing the binding at that point to the block.
    # @return [Attachment]
    #
    # # Examples
    #
    # ```
    # attachment = Truffle::Attachments.attach(__FILE__, 21) do |binding|
    #   # Double the value of local variable foo before this line runs
    #   binding.local_variable_set(:foo, binding.local_variable_get(:foo) * 2)
    # end
    # ...
    # attachment.detach
    # ```
    def self.attach(file, line, &block)
      Attachment.new(attach_internal(file, line, &block))
    end

    # Represents a block which has been installed.
    class Attachment

      def initialize(handle)
        @handle = handle
      end

      # Detach the code which was attached.
      def detach
        Attachments.detach_internal @handle
      end

    end

  end

end
