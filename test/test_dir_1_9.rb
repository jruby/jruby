require 'test/unit'
require 'pathname'

class DirTest19 < Test::Unit::TestCase

  # JRUBY-5399
  def test_dir_aref
    path = Pathname('test')
    assert_equal 'test', Dir[path].first
    assert_equal 'test', Dir.glob(path).first
  end
end
