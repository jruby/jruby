require 'win32ole'
require 'benchmark'

COUNT = (ARGV[0] || 250).to_i
ITERATIONS = (ARGV[1] || 20).to_i

dir = 'C:/opt/test_data'
fso = WIN32OLE.new('Scripting.FileSystemObject')
drives = fso.GetFolder(dir).Files

total = 0.0
ITERATIONS.times do
  printf "drives.each #{COUNT} of 120 calls: "
  time = Benchmark.measure {
    COUNT.times do 
      drives.each {|d| }
    end
  }
  puts time
  total += time.real
end
#sleep 1000

puts "Total time = #{total}"
