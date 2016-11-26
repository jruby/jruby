#!/usr/bin/env ruby
# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
# This code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

require 'json'

result = JSON.parse(File.read(ARGV[0]))
wait = ARGV[1] == '--wait'

any_failed = result['queries'].any? do |q|
  q.any? do |k, v|
    k == 'extra.error' && v == 'failed'
  end
end

if wait
  if any_failed
    STDERR.puts 'some benchmark failed, waiting to return failure...'
    File.write('failures', '')
  end
else
  if any_failed || File.exist?('failures')
    STDERR.puts 'some benchmark failed, returning failure'
    exit 1
  end
end
