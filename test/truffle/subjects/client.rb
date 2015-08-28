#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Repeatedly makes the same request, ignoring failures. Useful for server.rb
# where you may want to know how soon it can handle its first request.

require 'open-uri'

loop do
  begin
    open('http://localhost:8000/hello').read
  rescue
    next
  end
end
