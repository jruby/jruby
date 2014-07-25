require 'test/unit'

# This test is for JRUBY-3029, testing that each path is searched for .class and .rb before
# proceeding to the next path. The .class source loaded has different code from the .rb
# version, but to make them load and be testable in a specific way. I include the source
# of the .class version here...

=begin
$jruby3029 = 'class'
=end

class TestLoadClassBeforeRb < Test::Unit::TestCase
  def test_load_class_before_rb
    $LOAD_PATH << File.expand_path(File.dirname(__FILE__) + "/dir1")
    $LOAD_PATH << File.expand_path(File.dirname(__FILE__) + "/dir2")

    require 'target'
    assert_equal 'rb', $jruby3029
  end
end
