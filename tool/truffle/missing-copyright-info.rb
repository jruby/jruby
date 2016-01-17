# Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
# 
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

failures = false

Dir.glob('truffle/**/*').each do |file|
  next if file.start_with? 'truffle/target/generated-sources'
  next if file == 'truffle/pom.rb'
  if file =~ /.*\.(rb|java)/
    lines = IO.readlines(file)
    unless (lines[0] + lines[1]) =~ /.*Copyright \(c\).*/
      puts 'These files are missing copyright information:' unless failures
      puts file
      failures = true
    end 
  end
end

exit 1 if failures
