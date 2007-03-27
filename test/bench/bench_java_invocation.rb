require 'benchmark'
require 'java'

puts "Measure bytelist appends (via Java integration)"
5.times { 
  puts Benchmark.measure { 
    sb = org.jruby.util.ByteList.new 
    foo = org.jruby.util.ByteList.plain("foo") 
    1000000.times { 
      sb.append(foo) 
    } 
  }
}

puts "Measure string appends (via normal Ruby)"
5.times { 
  puts Benchmark.measure { 
    str = "" 
    foo = "foo" 
    1000000.times { 
      str << foo 
    } 
  } 
}
