# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

$trace = []

$trace_proc = proc { |*args|
  args[4] = args[4].dup
  $trace << args
}

class MockBinding

  def local_variables
    []
  end

end

def check(file)
  expected = nil
  
  File.open('test/truffle/integration/tracing/' + file) do |f|
    expected = f.each_line.map { |line| eval(line) }
  end
  
  actual = $trace
  
  empty_binding = MockBinding.new
  
  while actual.size < expected.size
    actual.push ['missing', 'missing', :missing, :missing, empty_binding, :missing]
  end
  
  while expected.size < actual.size
    expected.push ['missing', 'missing', :missing, :missing, empty_binding, :missing]
  end
  
  success = true
  
  expected.zip(actual).each do |e, a|
    unless a[0] == e[0]
      puts "Expected #{e[0].inspect}, actually #{a[0].inspect}"
      success = false
    end
    
    unless a[1].end_with?(e[1])
      puts "Expected #{e[1].inspect}, actually #{a[1].inspect}"
      success = false
    end
  
    unless a[2] == e[2]
      puts "Expected #{e[2].inspect}, actually #{a[2].inspect}"
      success = false
    end
  
    unless a[3] == e[3]
      puts "Expected #{e[3].inspect}, actually #{a[3].inspect}"
      success = false
    end
    
    ab = Hash[a[4].local_variables.sort.map { |v| [v, a[4].local_variable_get(v)] }]
  
    unless ab == e[4]
      puts "Expected Binding, actually #{ab.inspect}"
      success = false
    end
  
    unless a[5] == e[5]
      puts "Expected #{e[5].inspect}, actually #{a[5].inspect}"
      success = false
    end
  end
  
  unless success
    exit 1
  end
end
