# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 2.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

Dir.entries('core/target/generated-sources/org/jruby/gen').each do |file|
  populator_name = file.gsub(/\.java$/, '')
  class_name = populator_name.gsub(/\$/, '.').gsub(/\.POPULATOR/, '')
  puts "        populators.put(#{class_name}.class, new org.jruby.gen.#{populator_name}());"
end
