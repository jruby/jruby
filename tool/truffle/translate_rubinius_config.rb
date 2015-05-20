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
  next unless /^(?<var>rbx(\.\w+)*) = (?<value>.+)$/ =~ line
  code = case value
  when /^-?\d+$/
    case Integer(value)
    when (-2**31...2**31)
      value
    when (-2**63...2**63)
      "#{value}L"
    else
      "newBignum(context, \"#{value}\")"
    end
  when "true"
    value
  else
    "context.makeString(\"#{value}\")"
  end
  puts "        configuration.config(\"#{var}\", #{code});"
end
