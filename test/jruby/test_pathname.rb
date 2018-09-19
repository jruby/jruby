# encoding: UTF-8
require 'test/unit'
require 'pathname'
require 'rbconfig'

class TestPathname < Test::Unit::TestCase
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

  def test_dup
    assert_equal 'some/path', Pathname.new('some/path').dup.to_s
  end

  # GH-3392
  def test_dirname_ending_in_!
    x = "joe"
    y = "joe/pete!/bob"
    assert Pathname.new(y).relative_path_from(Pathname.new(x)).to_s == 'pete!/bob'
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

  def test_unicode_name
    x = "joe"
    y = "joe/⸀䐀攀氀攀琀攀䴀攀/fred"
    p = Pathname.new(y).relative_path_from(Pathname.new(x))
    assert_equal "⸀䐀攀氀攀琀攀䴀攀/fred", p.to_s
  end

  def test_pathname_windows
    assert_equal(Pathname('foo.bar.rb').expand_path.relative_path_from(Pathname(Dir.pwd)), Pathname('foo.bar.rb'))
  end if WINDOWS

  # GH-3150
  def test_expand_path_with_pathname_and_uri_path
    assert_equal 'uri:classloader://foo', File.expand_path('foo', Pathname.new('uri:classloader:/'))
  end if defined?(JRUBY_VERSION)

end
