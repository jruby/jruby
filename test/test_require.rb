require 'test/unit'

class TestRequire < Test::Unit::TestCase
  def test_require_jar_should_make_its_scripts_accessible
    require 'jar_with_ruby_files'
    require 'hello_from_jar'
    assert "hi", $hello
  end
end