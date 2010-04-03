require 'test/unit'
require 'rubygems'
require 'rake'
require 'ant'

class TestAnt < Test::Unit::TestCase
  def test_ant_import
    ant_file = File.expand_path('../ant_example.xml', __FILE__)
    ant_import ant_file
    assert_equal 'absolutely', ant.properties['set_from_ant']
  end
end
