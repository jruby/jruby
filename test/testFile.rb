require 'test/minirunit'

test_check "Test File"

# dry tests which don't access the file system

test_ok(File::SEPARATOR)
test_ok(File::PATH_SEPARATOR)

# basename
test_equal("", File.basename(""))
test_equal("a", File.basename("a"))
test_equal("b", File.basename("a/b"))
test_equal("c", File.basename("a/b/c"))

test_equal("b", File.basename("a/b", ""))
test_equal("b", File.basename("a/bc", "c"))
test_equal("b", File.basename("a/b.c", ".c"))

test_equal("a", File.basename("a/"))
test_equal("b", File.basename("a/b/"))
test_equal("/", File.basename("/"))

# dirname
test_equal(".", File.dirname(""))
test_equal(".", File.dirname("."))
test_equal(".", File.dirname(".."))
test_equal(".", File.dirname("a"))
test_equal(".", File.dirname("./a"))
test_equal("./a", File.dirname("./a/b"))
test_equal("/", File.dirname("/"))
test_equal("/", File.dirname("/a"))
test_equal("/a", File.dirname("/a/b"))
test_equal("/a", File.dirname("/a/b/"))
test_equal("/", File.dirname("/"))

# expand_path

# join
[
  ["a", "b", "c", "d"],
  ["a"],
  [],	
  ["a", "b", "..", "c"]
].each do |a|
  test_equal(a.join(File::SEPARATOR), File.join(*a))
end

# split
test_equal([".", ""], File.split(""))
test_equal([".", "."], File.split("."))
test_equal([".", ".."], File.split(".."))
test_equal(["/", "/"], File.split("/"))
test_equal([".", "a"], File.split("a"))
test_equal([".", "a"], File.split("a/"))
test_equal(["a", "b"], File.split("a/b"))
test_equal(["a/b", "c"], File.split("a/b/c"))
test_equal(["/", "a"], File.split("/a"))
test_equal(["/", "a"], File.split("/a/"))
test_equal(["/a", "b"], File.split("/a/b"))
test_equal(["/a/b", "c"], File.split("/a/b/c"))
#test_equal(["//", "a"], File.split("//a"))
test_equal(["../a/..", "b"], File.split("../a/../b/"))

# wet tests which do access the file system

# IO#readlines, IO::readlines, open, close, delete, ...

test_exception(Errno::ENOENT) { File.open("NO_SUCH_FILE_EVER") }
f = open("testFile_tmp", "w")
f.write("one\ntwo\nthree\n")
f.close

f = open("testFile_tmp")
test_equal(["one", "two", "three"],
           f.readlines.collect {|l| l.strip })
f.close

test_equal(["one", "two", "three"],
           IO.readlines("testFile_tmp").collect {|l| l.strip })

test_ok(File.delete("testFile_tmp"))

begin
  Dir.mkdir("dir_tmp")
  test_ok(File.lstat("dir_tmp").directory?)
ensure
  Dir.rmdir("dir_tmp")
end

# - file?
test_ok(File.file?('test/testFile.rb'))
test_ok(! File.file?('test'))

stat = File.stat('test');
test_ok(!stat.file?)

test_ok(File.exist?('test'))
