require 'test/minirunit'
test_check "Test backquote:"

if File.exists?("/bin/echo")
  output = `/bin/echo hello`
  test_equal("hello\n", output)
end

if File.exists?("/bin/true")
  test_ok(system("/bin/true"))
  test_equal(0, $?.exitstatus)
end

if File.exists?("/bin/false")
  test_ok(! system("/bin/false"))
  test_ok($?.exitstatus > 0)
end
