require 'test/minirunit'
test_check "Test Java Integration:"

require 'java'

# Check that class_loader can be accessed
test_ok !org.jruby.Main.java_class.class_loader.nil?

