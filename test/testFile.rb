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
test_equal("b", File.basename("a/b.c", ".*"))
test_equal(".c", File.basename("a/.c", ".*"))


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

##### fnmatch #####
test_equal(true, File.fnmatch('cat', 'cat'))
test_equal(false, File.fnmatch('cat', 'category'))
test_equal(false, File.fnmatch('c{at,ub}s', 'cats'))
test_equal(false, File.fnmatch('c{at,ub}s', 'cubs'))
test_equal(false, File.fnmatch('c{at,ub}s', 'cat'))

test_equal(true, File.fnmatch('c?t', 'cat'))
test_equal(false, File.fnmatch('c\?t', 'cat'))
test_equal(true, File.fnmatch('c\?t', 'c?t'))
test_equal(false, File.fnmatch('c??t', 'cat'))
test_equal(true, File.fnmatch('c*', 'cats'));
test_equal(true, File.fnmatch('c*t', 'cat'))
test_equal(true, File.fnmatch('c\at', 'cat'))
test_equal(false, File.fnmatch('c\at', 'cat', File::FNM_NOESCAPE))
test_equal(true, File.fnmatch('a?b', 'a/b'))
#test_equal(false, File.fnmatch('a?b', 'a/b', File::FNM_PATHNAME))
#test_equal(false, File.fnmatch('a?b', 'a/b', File::FNM_PATHNAME))

test_equal(false, File.fnmatch('*', '.profile'))
#test_equal(true, File.fnmatch('*', '.profile', File::FNM_DOTMATCH))
test_equal(true, File.fnmatch('*', 'dave/.profile'))
#test_equal(true, File.fnmatch('*', 'dave/.profile', File::FNM_DOTMATCH))
#test_equal(false, File.fnmatch('*', 'dave/.profile', File::FNM_PATHNAME))

# join
[
  ["a", "b", "c", "d"],
  ["a"],
  [],
  ["a", "b", "..", "c"]
].each do |a|
  test_equal(a.join(File::SEPARATOR), File.join(*a))
end

test_equal("////heh////bar/heh", File.join("////heh////bar", "heh"))
test_equal("/heh/bar/heh", File.join("/heh/bar/", "heh"))
test_equal("/heh/bar/heh", File.join("/heh/bar/", "/heh"))
test_equal("/heh//bar/heh", File.join("/heh//bar/", "/heh"))
test_equal("/heh/bar/heh", File.join("/heh/", "/bar/", "/heh"))
test_equal("/HEH/BAR/FOO/HOH", File.join("/HEH", ["/BAR", "FOO"], "HOH"))
test_equal("/heh/bar", File.join("/heh//", "/bar"))
test_equal("/heh///bar", File.join("/heh", "///bar"))

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

test_ok(File.exist?('test'))

test_ok(File.size?('build.xml'))

filename = "__test__file"
File.open(filename, "w") {|f| }
File.utime(0, 0, filename)
test_equal(Time.at(0), File.mtime(filename))
File.unlink(filename)

# File::Stat tests
stat = File.stat('test');
stat2 = File.stat('build.xml');
test_ok(stat.directory?)
test_ok(stat2.file?)
test_equal("directory", stat.ftype)
test_equal("file", stat2.ftype)
test_ok(stat2.readable?)