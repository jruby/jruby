# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Implementation of Rubinius::Channel using a Queue
module Rubinius
  class Channel
    def initialize
      @queue = Queue.new
    end

    def send(obj)
      @queue << obj
    end
    alias_method :<<, :send

    def receive
      @queue.pop
    end

    def receive_timeout(duration)
      Rubinius.privately do
        @queue.receive_timeout(duration)
      end
    end

    def try_receive
      begin
        @queue.pop(true)
      rescue ThreadError # queue empty
        nil
      end
    end
  end
end
