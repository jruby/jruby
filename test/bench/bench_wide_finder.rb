require 'benchmark'

# Get test data for this benchmark at http://www.tbray.org/tmp/o10k.ap
if (ARGV.size == 0)
  puts "Get test data for this benchmark at http://www.tbray.org/tmp/o10k.ap"
  exit
end
file = ARGV[0]

5.times {
  counts = {}
  counts.default = 0
  puts Benchmark.measure {
    File.open(file) { |f|
      f.each_line {|line|
        if line =~ %r{GET /ongoing/When/\d\d\dx/(\d\d\d\d/\d\d/\d\d/[^ .]+) }
          counts[$1] += 1
        end
      }
    }
  }
}

#ARGF.each_line do |line|
#  if line =~ %r{GET /ongoing/When/\d\d\dx/(\d\d\d\d/\d\d/\d\d/[^ .]+) }
#    counts[$1] += 1
#  end
#end

#keys_by_count = counts.keys.sort { |a, b| counts[b] <=> counts[a] }
#keys_by_count[0 .. 9].each do |key|
#  puts "#{counts[key]}: #{key}"
#end
