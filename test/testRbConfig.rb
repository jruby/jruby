require 'test/minirunit'
test_check "rbconfig"

require 'rbconfig'

test_equal("1", Config::CONFIG['MAJOR'])
test_equal("8", Config::CONFIG['MINOR'])

test_equal(File.join(Dir.pwd, "bin"), Config::CONFIG['bindir'])
test_ok(["jruby.rb", "jruby.sh", "jruby.bat"].include?(Config::CONFIG['RUBY_INSTALL_NAME']))

test_print_report
