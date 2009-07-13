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

puts "Measure java.lang.Thread#name, String entering Ruby"
JThread = java::lang::Thread
class JThread
  def bench
    x = nil
    1000000.times {
      x = name
    }
  end
end

5.times {
  puts Benchmark.measure {
    JThread.currentThread.bench
  }
}

puts "Measure Fixnum#to_s, String being constructed"
5.times {
  puts Benchmark.measure {
    x = 1
    1000000.times {
      y = x.to_s
    }
  }
}

puts "Measure Integer.valueOf, overloaded call with a primitive"
5.times {
  puts Benchmark.measure {
    integer = java.lang.Integer
    x = 1
    1000000.times {
      integer.valueOf(x)
    }
  }
}
    
puts "Measure ArrayList<Object>.get, non-coercible type entering Ruby"
5.times {
  puts Benchmark.measure {
    list = java.util.ArrayList.new
    list.add(java.lang.Object.new)
    1000000.times {
      list.get(0)
    }
  }
}
    
puts "Measure java.lang.Object.new"
5.times {
  puts Benchmark.measure {
    1000000.times {
      Object.new
    }
  }
}
    
