# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

def check(file)
  expected = nil
  
  File.open('test/truffle/integration/backtraces/' + file) do |f|
    expected = f.each_line.map(&:strip)
  end
  
  begin
    yield
  rescue Exception => exception
    actual = exception.backtrace
  end
  
  while actual.size < expected.size
    actual.push '(missing)'
  end
  
  while expected.size < actual.size
    expected.push '(missing)'
  end
  
  success = true
  
  print = []
  expected.zip(actual).each do |e, a|
    unless a.end_with?(e)
      print << "E #{a} Expected: #{e}"
      success = false
    else
      print << ". #{a}"
    end
  end
  
  unless success
    puts print.join("\n")
    exit 1
  end
end
