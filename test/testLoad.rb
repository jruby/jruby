require "test/minirunit"

test_check("Test Ruby-Init")

$ruby_init = false
file = __FILE__
if (File::Separator == '\\')
	file.gsub!('\\\\', '/')
end
require File::dirname(file) + "/RubyInitTest.jar"
test_ok($ruby_init)

