require 'test/minirunit'
test_check "Test backquote:"

if File.exists?("/bin/echo")
  output = `/bin/echo hello`
  test_equal("hello\n", output)
end
