require "java"

include_class "java.io.FileReader"
include_class "java.io.BufferedReader"

filename = "./samples/java2.rb"

fr = FileReader.new filename
br = BufferedReader.new fr

s = br.readLine

print "------ ", filename, "------\n"

while s
  puts s.to_s
  s = br.readLine
end

print "------ ", filename, " end ------\n";

br.close
