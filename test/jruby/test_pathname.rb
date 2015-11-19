require 'test/unit'
require 'pathname'
require 'rbconfig'
require 'tempfile'

class TestPathname < Test::Unit::TestCase
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

  def test_dup
    assert_equal 'some/path', Pathname.new('some/path').dup.to_s
  end

  unless WINDOWS # Don't have symlinks on Windows.
    def test_realpath_symlink
      target = Tempfile.new 'target'
      link = Dir::Tmpname.make_tmpname 'link', nil
      File.symlink(target, link)
      assert_equal Pathname.new(target).realpath, Pathname.new(link).realpath
    ensure
      target.close! if target
      File.delete(link) if link && File.exists?(link)
    end
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

end
