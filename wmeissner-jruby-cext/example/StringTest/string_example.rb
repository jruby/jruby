require File.expand_path(File.join(__FILE__, "..", "string"))

s = "ruby string"
puts "Ruby: String is #{s} with length #{s.length}"
puts CString.new.hello(s)

