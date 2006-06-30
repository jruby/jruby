require "test/minirunit"
test_check("Test Ruby-Init")

$ruby_init = false
file = __FILE__
if (File::Separator == '\\')
	file.gsub!('\\\\', '/')
end
# Load jar file RubyInitTest.java
require File::dirname(file) + "/RubyInitTest"
test_ok($ruby_init)

# Yes, the following line is supposed to appear twice
test_exception(LoadError) { require 'NonExistantRequriedFile'}
test_exception(LoadError) { require 'NonExistantRequriedFile'}

test_ok require('test/requireTarget')
test_ok !require('test/requireTarget')
