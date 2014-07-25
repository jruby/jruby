require 'test/unit'

# JRUBY-4760

class TestDotDotInsideJar < Test::Unit::TestCase

  def test_access_file_with_dotdot
    assert_nothing_raised {
      jar = File.join(File.dirname(__FILE__), 'jar_with_relative_require1.jar')
      File.open("file:#{jar}!/test/../foo.rb")
    }
  end
end