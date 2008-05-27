require 'test/unit'

class TestJarOnLoadPath < Test::Unit::TestCase
  def test_jar_on_load_path
    $LOAD_PATH << "test/test_jruby_1332.jar"
    require 'test_jruby_1332.rb'
    assert($jruby_1332)
  end
end
