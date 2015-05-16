#!/usr/bin/env ruby

# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

puts "        // Generated from tool/truffle/translate_rubinius_config.rb < ../rubinius/runtime/platform.conf"

ARGF.each do |line|
  match = line.match(/(?'var'rbx(\.\w+)*) = (?'value'.+)/)
  next unless match
  var = match[:var]
  value = match[:value]
  if /.*\.(offset|size|sizeof)$/ =~ var
    code = value.to_s
  else
    code = "context.makeString(\"#{value}\")"
  end
  puts "        configuration.config(\"#{var}\", #{code});"
end
