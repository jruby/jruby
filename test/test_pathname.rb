require 'test/unit'
require 'pathname'
require 'rbconfig'
require 'fileutils'
require 'tempfile'

class TestPathname < Test::Unit::TestCase
  WINDOWS = RbConfig::CONFIG['host_os'] =~ /Windows|mswin/

  def test_dup
    assert_equal 'some/path', Pathname.new('some/path').dup.to_s
  end

  unless WINDOWS # Don't have symlinks on Windows.
    def test_realpath_symlink
      target = Tempfile.new 'target'
      link = Tempfile.new 'link'
      File.symlink(target, link)
      assert_equal Pathname.new(target.path).realpath, Pathname.new(link).realpath
    ensure
      target.unlink if target
      link.unlink if link
    end
  end
end
