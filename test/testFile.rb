require 'test/minirunit'

test_check "Test File"

# join
[
  ["a", "b", "c", "d"],
  ["a"],
  [],	
  ["a", "b", "..", "c"]
].each do |a|
  test_equal(a.join("/"), File.join(*a))
end

# dirname
test_equal("/", File.dirname(File.join("/tmp")))
test_equal("/tmp", File.dirname(File.join("/tmp/")))
test_equal("g/f/d/s/a", File.dirname(File.join(*["g", "f", "d", "s", "a", "b"])))
test_equal("b", File.basename(File.join(*["g", "f", "d", "s", "a", "b"])))
test_equal("/", File.dirname("/"))
test_equal(".", File.dirname("\\")) # ?
test_equal(".", File.dirname("wahoo"))

# IO#readlines, IO::readlines, open, close, delete, ...

f = open("testFile_tmp", "w")
f.write("one\ntwo\nthree\n")
f.close

f = open("testFile_tmp")
test_equal(["one", "two", "three"],
           f.readlines.collect {|l| l.strip })
f.close

test_equal(["one", "two", "three"],
           IO.readlines("testFile_tmp").collect {|l| l.strip })

File.delete("testFile_tmp")

if File.exists?("/tmp")
  stat = File.lstat("/tmp")
  test_ok(stat.directory?)
end
