require 'benchmark'

str = "Content-Type: text/html; charset=utf-8\r\nSet-Cookie: aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa "

TIMES=100_000

puts "each_line on small string with several lines"
10.times do 
  puts(Benchmark.measure{TIMES.times { str.each_line{} }})
end

str = "abc" * 15

puts "each_line on short string with no line divisions"
10.times do 
  puts(Benchmark.measure{TIMES.times { str.each_line{} }})
end

str = "abc" * 4000

puts "each_line on large string with no line divisions"
10.times do 
  puts(Benchmark.measure{TIMES.times { str.each_line{} }})
end
