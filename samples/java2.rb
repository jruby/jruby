require "java"

module JavaIO
  include_package "java.io"
end

filename = "./samples/java2.rb"

fr = JavaIO::FileReader.new filename
br = JavaIO::BufferedReader.new fr

s = br.readLine

print "------ ", filename, "------\n"

while s
  puts s.to_s
  s = br.readLine
end

print "------ ", filename, " end ------\n";

br.close
