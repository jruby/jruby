require 'test/minirunit'

test_check "Test Dir"

test_equal(Dir.pwd, Dir.getwd)

begin
  Dir.delete("./testDir_1")
rescue
end
Dir.mkdir("./testDir_1")

d = Dir.new("./testDir_1")
test_ok(d.kind_of?(Enumerable))
test_equal(['.', '..'], d.entries)

(1..2).each {|i|
  File.open("./testDir_1/file" + i.to_s, "w") {|f|
    f.write("hello")
  }
}

test_equal(['.', '..', "file1", "file2"], Dir.entries('./testDir_1').sort)
test_equal(['.', '..', "file1", "file2"], Dir.new('./testDir_1').entries.sort)
Dir.chdir("./testDir_1")
test_equal(['.', '..', "file1", "file2"], Dir.entries('.').sort)
Dir.chdir("..")

files = []
Dir.foreach('./testDir_1') {|f|
  files << f
}
test_equal(['.', '..', "file1", "file2"], files.sort)

(1..2).each {|i|
  File.delete("./testDir_1/file" + i.to_s)
}
Dir.delete("./testDir_1")
