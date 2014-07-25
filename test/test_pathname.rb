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
end
