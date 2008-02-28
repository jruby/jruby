require 'benchmark'
require 'stringio'

long_string = "xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx" * 1000

Benchmark.bm(30) {|bm|
10.times {
  sio = StringIO.new
  bm.report("short string * 5000") {
    5000.times {
      sio.puts("this is a short string to be appended to the stringio")
    }
  }
  bm.report("long string * 5000") {
    5000.times {
      sio = StringIO.new
      sio.puts(long_string)
    }
  }
  bm.report("no string * 5000") {
    5000.times {
      sio = StringIO.new
      sio.puts()
    }
  }
}
}
