Java::import "java.io"
Java::name "java.io.File", "JavaFile"

filename = "./samples/java2.rb"

file = JavaFile.new filename

puts 1

fr = FileReader.new file

puts 2

br = BufferedReader.new fr

puts 3

s = br.readLine

print "------ ", filename, "------\n"

while s
  puts s.to_s
  s = br.readLine
end

print "------ ", filename, " end ------\n";

br.close
