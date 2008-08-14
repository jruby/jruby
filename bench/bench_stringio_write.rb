require 'benchmark'
require 'stringio'

long_string = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" * 1000

Benchmark.bm(30) {|bm|
10.times {
  sio = StringIO.new
  bm.report("short string * 5000") {
    5000.times {
      sio.write("this is a short string to be appended to the stringio")
    }
  }
  bm.report("long string * 5000") {
    5000.times {
      sio = StringIO.new
      sio.write(long_string)
    }
  }
}
}
