require 'benchmark'
require 'java'

import java.lang.System

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

puts "Measure System.currentTimeMillis, int becoming Fixnum"
def System.bench
  x = nil
  1000000.times {
    x = currentTimeMillis
  }
end

5.times {
  puts Benchmark.measure {
    System.bench
  }
}

puts "Measure string.length, integer length to fixnum"
5.times {
  puts Benchmark.measure {
    x = ""
    1000000.times {
      y = x.length
    }
  }
}
