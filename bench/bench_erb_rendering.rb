require 'erb'
require 'benchmark'

str1 = <<TEMPLATE
abc
foo bar
<blah>
TEMPLATE

str2 = <<TEMPLATE
Hoho
foo bar
<%= "123" %>

<% i = "one" %>

<%= i*2 %>
TEMPLATE

large_str1 = str1*100
large_str2 = str2*100
  

puts "Small with no stuff"
10.times do 
  puts(Benchmark.measure do 
         e = ERB.new(str1)
         10_000.times do 
           e.result
         end
       end)
end

puts "Small with eval stuff"
10.times do 
  puts(Benchmark.measure do 
         e = ERB.new(str2)
         10_000.times do 
           e.result
         end
       end)
end

puts "Large with no stuff"
10.times do 
  puts(Benchmark.measure do 
         e = ERB.new(large_str1)
         1_000.times do 
           e.result
         end
       end)
end

puts "Large with eval stuff"
10.times do 
  puts(Benchmark.measure do 
         e = ERB.new(large_str2)
         100.times do 
           e.result
         end
       end)
end

