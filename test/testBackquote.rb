require 'minirunit'
test_check "Test backquote and config:"
require "rbconfig"
interpreter = File.join(Config::CONFIG["bindir"], Config::CONFIG["RUBY_INSTALL_NAME"])
version = `#{interpreter} --version`

test_ok(version =~ /ruby (\d+\.\d+\.\d+)\s+\((.*?)\)\s+\[(.*?)\]/)
test_equal("1.6.7", $1)
test_equal("java", $3)

