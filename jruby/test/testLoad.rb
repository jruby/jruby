require "test/minirunit"
test_check("Test Ruby-Init")

# Allow us to run MRI against non-Java dependent tests
if RUBY_PLATFORM=~/java/
  $ruby_init = false
  file = __FILE__
  if (File::Separator == '\\')
    file.gsub!('\\\\', '/')
  end
  # Load jar file RubyInitTest.java
  require File::dirname(file) + "/RubyInitTest"
  test_ok($ruby_init)
end

# Yes, the following line is supposed to appear twice
test_exception(LoadError) { require 'NonExistantRequriedFile'}
test_exception(LoadError) { require 'NonExistantRequriedFile'}

test_ok require('test/requireTarget')
test_ok !require('test/requireTarget')

$loaded_foo_bar = false
test_ok require('test/foo.bar')
test_ok $loaded_foo_bar


