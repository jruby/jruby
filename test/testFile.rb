require 'minirunit'

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
test_equal("/", File.dirname("/"))
test_equal(".", File.dirname("wahoo"))
