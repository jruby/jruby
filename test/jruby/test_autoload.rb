require 'test/unit'

class TestAutoload < Test::Unit::TestCase
  def test_basic_autoload
    assert_equal(nil, Object.autoload("Autoloaded", "#{File.dirname(__FILE__)}/autoloaded.rb"))
    assert(Object.const_defined?("Autoloaded"))
    assert_nil(Object.autoload?("Object::Autoloaded"))
    assert_equal("#{File.dirname(__FILE__)}/autoloaded.rb", Object.autoload?("Autoloaded"))
    assert_equal(Class, Object::Autoloaded.class)
    # This should not really autoload since it is set for real
    Object.autoload("Autoloaded", "#{File.dirname(__FILE__)}/autoloaded2.rb")
    assert_equal(Class, Object::Autoloaded.class)
    # Set versus load (will not perform autoload)
    Object.autoload("Autoloaded2", "#{File.dirname(__FILE__)}/autoloaded3.rb")
    Object.class_eval "Autoloaded2 = 3"
    assert_equal(3, Object::Autoloaded2)
    Object.autoload("Autoloaded4", "#{File.dirname(__FILE__)}/autoloaded4.rb")
    assert_equal(3, Object::Autoloaded4)
    Object.autoload("Autoloaded5", "#{File.dirname(__FILE__)}/autoloaded5.rb")
    assert_nothing_raised { require "#{File.dirname(__FILE__)}/autoloaded5.rb" }
    Object.autoload("Autoloaded6", "#{File.dirname(__FILE__)}/autoloaded6.rb")
    assert_nothing_raised { Object.__send__(:remove_const, :Autoloaded6) }
    assert_raises(NameError) { Object::Autoloaded6 }
  end
  
  def test_overwrite_autoload
    assert_raise(LoadError) do
      Object.class_eval <<-EOS
        autoload :SomeClass, 'somefile'
        class SomeClass; def foo; true; end; end
      EOS
    end
  end
end
