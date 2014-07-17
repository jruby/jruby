require 'test/unit'
require 'rubygems'
require 'rake'
require 'ant'

class TestAnt < Test::Unit::TestCase
  def test_ant_import
    Dir.chdir(File.dirname(__FILE__)) do
      ant_import 'ant_example.xml'
      assert_equal 'surely', ant.properties['set_from_ant']
    end
  end
end
