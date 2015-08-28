#!/usr/bin/env ruby
# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# Serves a single request then shuts down. Useful with client.rb to make that
# request.

require 'webrick'

server = WEBrick::HTTPServer.new(Port: 8000)

server.mount_proc '/hello' do |req, res|
  res.status = 200
  res['Content-Type'] = 'text/plain'
  res.body = 'Hello, World!'
  server.shutdown
end

server.start
