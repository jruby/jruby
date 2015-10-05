require 'test/unit'
require 'pathname'

class TestPathname < Test::Unit::TestCase
  def test_dup
    p = Pathname.new("some/path")
    assert_equal "some/path", p.dup.to_s
  end

  def test_root_and_absolute
    [:root?, :absolute?].each do |method|
      assert Pathname.new('uri:classloader:/').send method
      assert Pathname.new('uri:classloader://').send method
      assert Pathname.new('uri:file:/').send method
      assert Pathname.new('uri:file://').send method
      assert Pathname.new('classpath:/').send method
      assert Pathname.new('classpath://').send method
      assert Pathname.new('file:/').send method
      assert Pathname.new('file://').send method
      assert Pathname.new('jar:file:/my.jar!/').send method
      assert Pathname.new('jar:file://my.jar!/').send method
      assert Pathname.new('jar:/my.jar!/').send method
      assert Pathname.new('jar://my.jar!/').send method
      assert Pathname.new('file:/my.jar!/').send method
      assert Pathname.new('file://my.jar!/').send method
      assert Pathname.new('my.jar!/').send method
    end
  end

  def test_absolute
    assert Pathname.new('uri:classloader:/asd').absolute?
    assert Pathname.new('uri:classloader://asd').absolute?
    assert Pathname.new('uri:file:/asd').absolute?
    assert Pathname.new('uri:file://asd').absolute?
    assert Pathname.new('classpath:/asd').absolute?
    assert Pathname.new('classpath://asd').absolute?
    assert Pathname.new('file:/asd').absolute?
    assert Pathname.new('file://asd').absolute?
    assert Pathname.new('jar:file:/my.jar!/asd').absolute?
    assert Pathname.new('jar:file://my.jar!/asd').absolute?
    assert Pathname.new('jar:/my.jar!/asd').absolute?
    assert Pathname.new('jar://my.jar!/asd').absolute?
    assert Pathname.new('file:/my.jar!/asd').absolute?
    assert Pathname.new('file://my.jar!/asd').absolute?
    assert Pathname.new('my.jar!/asd').absolute?
  end

end
