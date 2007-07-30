require 'test/minirunit'
test_check "rbconfig"

require 'rbconfig'

test_equal("1", Config::CONFIG['MAJOR'])
test_equal("8", Config::CONFIG['MINOR'])

test_ok(["jruby.rb", "jruby", "jruby.bat"].include?(Config::CONFIG['RUBY_INSTALL_NAME']))
