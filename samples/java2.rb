include Java

import java.io.FileReader
import java.io.BufferedReader

filename = "./samples/java2.rb"
br = BufferedReader.new(FileReader.new(filename))

s = br.readLine

print "------ ", filename, "------\n"

while s
  puts s.to_s
  s = br.readLine
end

print "------ ", filename, " end ------\n";

br.close
