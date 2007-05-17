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

test_equal(nil, autoload("Autoloaded", "#{File.dirname(__FILE__)}/autoloaded.rb"))
test_ok(Object.const_defined?("Autoloaded"))
test_equal("#{File.dirname(__FILE__)}/autoloaded.rb", Object.autoload?("Autoloaded"))
#test_equal("#{File.dirname(__FILE__)}/autoloaded.rb", Object.autoload?("Object::Autoloaded"))
test_equal(Class, Autoloaded.class)
# This should not really autoload since it is set for real
autoload("Autoloaded", "#{File.dirname(__FILE__)}/autoloaded2.rb")
test_equal(Class, Autoloaded.class)
# Set versus load (will not perform autoload)
autoload("Autoloaded2", "#{File.dirname(__FILE__)}/autoloaded3.rb")
Autoloaded2 = 3
test_equal(3, Autoloaded2)
autoload("Autoloaded4", "#{File.dirname(__FILE__)}/autoloaded4.rb")
test_equal(3, Object::Autoloaded4)
autoload("Autoloaded5", "#{File.dirname(__FILE__)}/autoloaded5.rb")
test_no_exception { require "#{File.dirname(__FILE__)}/autoloaded5.rb" }
autoload("Autoloaded6", "#{File.dirname(__FILE__)}/autoloaded6.rb")
test_no_exception { self.class.__send__(:remove_const, :Autoloaded6) }
test_exception(NameError) { Autoloaded6 }


