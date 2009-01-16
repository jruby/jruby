require 'benchmark'
require 'tempfile'

COUNT = 1_000
WRITE_COUNT = 30

puts "#{COUNT} Tempfile.new(file)"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < COUNT
      tf = Tempfile.new("heh")
      i = i + 1
    end
  }
}

puts "#{COUNT} Tempfile.new(file);write;close"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < COUNT
      tf = Tempfile.new("heh")
      j = 0
      while j < WRITE_COUNT
        tf.write "hehheheheheheheehehe\n"
        j = j + 1
      end
      tf.close
      i = i + 1
    end
  }
}

