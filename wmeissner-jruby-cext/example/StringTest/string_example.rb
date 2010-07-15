require File.expand_path(File.join(__FILE__, "..", "string"))

s = "ruby string"
puts "Ruby: String is #{s} with length #{s.length}"
puts CString.new.hello(s)
cs = CString.new
s2 = 0.chr * (50 * 1024)
s.freeze
1000_000.times do
  cs.hello(s) 
  cs.hello(s2 + " ") 
end

