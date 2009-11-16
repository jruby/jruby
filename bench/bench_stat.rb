require 'benchmark'
tmp = ENV['TMP'] || ENV['TEMP'] || ENV['TMPDIR'] || ENV["USERPROFILE"] || '/tmp'

puts "10k File.stat(dir)"
10.times {
  puts Benchmark.measure {
    i = 0
    while i < 10_000
      File.stat(tmp)
      i = i + 1
    end
  }
}
