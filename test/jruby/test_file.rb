# -*- coding: utf-8 -*-
require 'test/unit'
require 'test/jruby/test_helper'
require 'fileutils'
require 'tempfile'

class TestFile < Test::Unit::TestCase
  include TestHelper

  def setup
    @teardown_blocks = []
  end

  def teardown
    @teardown_blocks.each do |b|
      b.call
    end
  end

  def test_file_separator_constants_defined
    assert(File::SEPARATOR)
    assert(File::PATH_SEPARATOR)
  end

  def test_basename
    assert_equal("", File.basename(""))
    assert_equal("a", File.basename("a"))
    assert_equal("b", File.basename("a/b"))
    assert_equal("c", File.basename("a/b/c"))

    assert_equal("b", File.basename("a/b", ""))
    assert_equal("b", File.basename("a/bc", "c"))
    assert_equal("b", File.basename("a/b.c", ".c"))
    assert_equal("b", File.basename("a/b.c", ".*"))
    assert_equal(".c", File.basename("a/.c", ".*"))


    assert_equal("a", File.basename("a/"))
    assert_equal("b", File.basename("a/b/"))
    assert_equal("/", File.basename("/"))
  end

  def test_basename_uri_like_paths
    assert_equal('uri:classloader:/', File.basename('uri:classloader:/'))
    assert_equal('uri:classloader://', File.basename('uri:classloader://'))
    assert_equal('uri:file:/', File.basename('uri:file:/'))
    assert_equal('uri:file://', File.basename('uri:file://'))
    assert_equal('classpath:/', File.basename('classpath:/'))
    assert_equal('classpath://', File.basename('classpath://'))
    assert_equal('file:/', File.basename('file:/'))
    assert_equal('file://', File.basename('file://'))
    assert_equal('jar:file:/my.jar!/', File.basename('jar:file:/my.jar!/'))
    assert_equal('jar:file://my.jar!/', File.basename('jar:file://my.jar!/'))
    assert_equal('jar:/my.jar!/', File.basename('jar:/my.jar!/'))
    assert_equal('jar://my.jar!/', File.basename('jar://my.jar!/'))
    assert_equal('file:/my.jar!/', File.basename('file:/my.jar!/'))
    assert_equal('file://my.jar!/', File.basename('file://my.jar!/'))
    assert_equal('my.jar!/', File.basename('my.jar!/'))
  end if IS_JRUBY

  # Added to add more flexibility on windows.  Depending on subsystem (msys,
  # cygwin, cmd) environment sometimes we have mixed case drive letters.  Since
  # windows is still case insensitive, downcasing seems a simple solution.
  def paths_equal(expected, actual)
    if WINDOWS
      assert_equal(expected.downcase, actual.downcase)
    else
      assert_equal(expected, actual)
    end
  end


  def test_expand_path_cross_platform
    paths_equal(Dir.pwd, File.expand_path(""))
    paths_equal(Dir.pwd, File.expand_path("."))
    paths_equal(Dir.pwd, File.expand_path(".", "."))
    paths_equal(Dir.pwd, File.expand_path("", "."))
    paths_equal(Dir.pwd, File.expand_path(".", ""))
    paths_equal(Dir.pwd, File.expand_path("", ""))

    paths_equal(File.join(Dir.pwd, "x/y/z/a/b"), File.expand_path("a/b", "x/y/z"))
    paths_equal(File.join(Dir.pwd, "bin"), File.expand_path("../../bin", "tmp/x"))

    # JRUBY-2143
    assert_nothing_raised {
      File.expand_path("../../bin", "/tmp/x")
      File.expand_path("/bin", "/tmp/x")
      File.expand_path("../../bin", "C:/tmp/x")
      File.expand_path("../bin", "C:/tmp/x")
    }
  end

  # JRUBY-3849
  def test_expand_path_encoding; require 'pathname'
    assert_equal(Encoding.find('UTF-8'), File.expand_path('/'.force_encoding('UTF-8')).encoding)
    assert_equal(Encoding.find('US-ASCII'), File.expand_path('/'.force_encoding('US-ASCII')).encoding)
    assert_equal(Encoding.find('UTF-8'), File.expand_path(Pathname.new('/'.force_encoding('UTF-8'))).encoding)
    assert_equal(Encoding.find('US-ASCII'), File.expand_path(Pathname.new('/'.force_encoding('US-ASCII'))).encoding)
  end

  def test_expand_path_nil
    assert_raise(TypeError) { File.expand_path(nil) }
    assert_raise(TypeError) { File.expand_path(nil, "/") }
    assert_raise(TypeError) { File.expand_path(nil, nil) }
  end

  def test_copy_file_and_preserve
    FileUtils.touch("original")
    FileUtils.copy_file('original', 'copy', true)
    assert(File.exist?('original'))
    assert(File.exist?('copy'))
  ensure
    FileUtils.rm_rf %w[ original copy ]
  end

  # JRUBY-1116: these are currently broken on windows
  # what are these testing anyway?!?!
  if WINDOWS
    def test_windows_basename
      assert_equal "", File.basename("c:")
      assert_equal "\\", File.basename("c:\\")
      assert_equal "abc", File.basename("c:abc")
    end

    def test_windows_expand_file_nul
      assert_equal "//./nul", File.expand_path("nul:")
      assert_equal "//./nul", File.expand_path("nul")
      assert_equal "//./NUL", File.expand_path("NUL")
      assert_equal "//./NUL", File.expand_path("NUL:")
    end

    # JRUBY-2052: this is important for fileutils
    def test_windows_dirname
      assert_equal("C:/", File.dirname("C:/"))
      assert_equal("C:\\", File.dirname("C:\\"))
      assert_equal("C:/", File.dirname("C:///////"))
      assert_equal("C:\\", File.dirname("C:\\\\\\\\"))
      assert_equal("C:/", File.dirname("C:///////blah"))
      assert_equal("C:\\", File.dirname("C:\\\\\\\\blah"))
      assert_equal("C:.", File.dirname("C:blah"))
      assert_equal "C:/", File.dirname("C:/temp/")
      assert_equal "c:\\", File.dirname('c:\\temp')
      assert_equal "C:.", File.dirname("C:")
      assert_equal "C:/temp", File.dirname("C:/temp/foobar.txt")
    end

    def test_windows_network_path
      assert_equal("\\\\network\\share", File.dirname("\\\\network\\share\\file.bat"))
      assert_equal("\\\\network\\share", File.dirname("\\\\network\\share"))
      assert_equal("\\\\localhost\\c$", File.dirname("\\\\localhost\\c$\\boot.bat"))
      assert_equal("\\\\localhost\\c$", File.dirname("\\\\localhost\\c$"))
    end

    def test_expand_path_windows
      assert_equal("C:/", File.expand_path("C:/"))
      assert_equal("C:/dir", File.expand_path("C:/dir"))
      assert_equal("C:/dir", File.expand_path("C:/dir/two/../"))

      assert_equal("C:/dir/two", File.expand_path("C:/dir/two/", "D:/"))
      assert_equal("C:/", File.expand_path("C:/", nil))

      # JRUBY-2161
      assert_equal("C:/", File.expand_path("C:/dir/../"))
      assert_equal("C:/", File.expand_path("C:/.."))
      assert_equal("C:/", File.expand_path("C:/../../"))
      assert_equal("C:/", File.expand_path("..", "C:/"))
      assert_equal("C:/", File.expand_path("..", "C:"))
      assert_equal("C:/", File.expand_path("C:/dir/two/../../"))
      assert_equal("C:/", File.expand_path("C:/dir/two/../../../../../"))

      # JRUBY-546
      current_drive_letter = Dir.pwd[0..2]
      paths_equal(current_drive_letter, File.expand_path(".", "/"))
      paths_equal(current_drive_letter, File.expand_path("..", "/"))
      paths_equal(current_drive_letter, File.expand_path("/", "/"))
      paths_equal(current_drive_letter, File.expand_path("../..", "/"))
      paths_equal(current_drive_letter, File.expand_path("../..", "/dir/two"))
      paths_equal(current_drive_letter + "dir", File.expand_path("../..", "/dir/two/three"))
      paths_equal(current_drive_letter, File.expand_path("/../..", "/"))
      paths_equal(current_drive_letter + "hello", File.expand_path("hello", "/"))
      paths_equal(current_drive_letter, File.expand_path("hello/..", "/"))
      paths_equal(current_drive_letter, File.expand_path("hello/../../..", "/"))
      paths_equal(current_drive_letter + "three/four", File.expand_path("/three/four", "/dir/two"))
      paths_equal(current_drive_letter + "two", File.expand_path("/two", "/one"))
      paths_equal(current_drive_letter + "three/four", File.expand_path("/three/four", "/dir/two"))

      assert_equal("C:/two", File.expand_path("/two", "C:/one"))
      assert_equal("C:/two", File.expand_path("/two", "C:/one/.."))
      assert_equal("C:/", File.expand_path("/two/..", "C:/one/.."))
      assert_equal("C:/", File.expand_path("/two/..", "C:/one"))

      assert_equal("//two", File.expand_path("//two", "/one"))
      assert_equal("//two", File.expand_path("//two", "//one"))
      assert_equal("//two", File.expand_path("//two", "///one"))
      assert_equal("//two", File.expand_path("//two", "////one"))
      assert_equal("//", File.expand_path("//", "//one"))

      # Corner cases that fail with JRuby on Windows (but pass with MRI 1.8.6)
      #
      ### assert_equal("///two", File.expand_path("///two", "/one"))
      ### assert_equal("///two", File.expand_path("///two/..", "/one"))
      ### assert_equal("////two", File.expand_path("////two", "/one"))
      ### assert_equal("////two", File.expand_path("////two", "//one"))
      ### assert_equal("//two", File.expand_path("//two/..", "/one"))
      ### assert_equal("////two", File.expand_path("////two/..", "/one"))
      #
      ### assert_equal("//bar/foo", File.expand_path("../foo", "//bar"))
      ### assert_equal("///bar/foo", File.expand_path("../foo", "///bar"))
      ### assert_equal("//one/two", File.expand_path("/two", "//one"))
    end

    # JRUBY-3132
    def test_realpath_windows
      assert_equal('C:/', File.realpath('C:/'))
      assert_equal('C:/', File.realpath('C:\\'))
    end
  else
    def test_expand_path
      assert_equal("/bin", File.expand_path("../../bin", "/foo/bar"))
      assert_equal("/foo/bin", File.expand_path("../bin", "/foo/bar"))
      assert_equal("//abc/def/jkl/mno", File.expand_path("//abc//def/jkl//mno"))
      assert_equal("//abc/def/jkl/mno/foo", File.expand_path("foo", "//abc//def/jkl//mno"))
      begin
        File.expand_path("~nonexistent")
        assert(false)
      rescue ArgumentError => e
        assert(true)
      rescue Exception => e
        assert(false)
      end

      assert_equal("/bin", File.expand_path("../../bin", "/tmp/x"))
      assert_equal("/bin", File.expand_path("../../bin", "/tmp"))
      assert_equal("/bin", File.expand_path("../../bin", "/"))
      assert_equal("/bin", File.expand_path("../../../../../../../bin", "/"))
      assert_equal(File.join(Dir.pwd, "x/y/z/a/b"), File.expand_path("a/b", "x/y/z"))
      assert_equal(File.join(Dir.pwd, "bin"), File.expand_path("../../bin", "tmp/x"))
      assert_equal("/bin", File.expand_path("./../foo/./.././../bin", "/a/b"))

      # JRUBY-2160
      assert_equal("/dir1/subdir", File.expand_path("/dir1/subdir/stuff/../"))
      assert_equal("///fedora/stuff/blah/MORE", File.expand_path("///fedora///stuff/blah/again//..////MORE/"))
      assert_equal("/", File.expand_path("/dir1/../"))
      assert_equal("/", File.expand_path("/"))
      assert_equal("/", File.expand_path("/.."))
      assert_equal("/hello", File.expand_path("/hello/world/three/../../"))

      assert_equal("/dir/two", File.expand_path("", "/dir/two"))
      assert_equal("/dir", File.expand_path("..", "/dir/two"))

      assert_equal("/file/abs", File.expand_path("/file/abs", '/abs/dir/here'))

      assert_equal("/", File.expand_path("/", nil))

      assert_equal("//two", File.expand_path("//two", "//one"))
      assert_equal("/", File.expand_path("/two/..", "//one"))
    end

    def test_expand_path_with_slashes
      assert_equal '///x/bar', File.expand_path("..///bar/", '///x/////y///')
      assert_equal '/oo/bar', File.expand_path("/oo///bar/", '///x/////y///')
      # Corner cases that fail with JRuby on Linux (but pass with MRI 2.5)
      #
      #assert_equal("/", File.expand_path("//two/..", "//one"))
      #assert_equal("/", File.expand_path("///two/..", "//one"))
      #assert_equal("/blah", File.expand_path("///two/../blah", "//one"))
    end

    def test_expand_path_with_file_prefix_slash
      assert_equal "file:/foo/bar", File.expand_path("file:/foo/bar")
      assert_equal "file:/bar", File.expand_path("file:/foo/../bar")
      assert_equal "file:/foo/bar/baz", File.expand_path("baz", "file:/foo/bar")
      assert_equal "file:/foo/bar", File.expand_path("file:/foo/bar", "file:/baz/quux")
      assert_equal "file:/foo/bar", File.expand_path("../../foo/bar", "file:/baz/quux")
      assert_equal "file:/foo/bar", File.expand_path("../../../foo/bar", "file:/baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_file_prefix_slash_slash
      assert_equal "file://foo/bar", File.expand_path("file://foo/bar")
      assert_equal "file://bar", File.expand_path("file://foo/../bar")
      assert_equal "file://foo/bar/baz", File.expand_path("baz", "file://foo/bar")
      # TODO
      #assert_equal "file://foo/bar", File.expand_path("file://foo/bar", "file://baz/quux")
      assert_equal "file://baz/foo/bar", File.expand_path("../../foo/bar", "file://baz/quux")
      assert_equal "file://baz/foo/bar", File.expand_path("../../../foo/bar", "file://baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_file_prefix_slash_slash_slash
      assert_equal "file:///foo/bar", File.expand_path("file:///foo/bar")
      assert_equal "file:///bar", File.expand_path("file:///foo/../bar")
      assert_equal "file:///foo/bar/baz", File.expand_path("baz", "file:///foo/bar")
      assert_equal "file:///foo/bar", File.expand_path("file:///foo/bar", "file:///baz/quux")
      assert_equal "file:///foo/bar", File.expand_path("../../foo/bar", "file:////baz/quux")
      assert_equal "file:///foo/bar", File.expand_path("../../../foo/bar", "file:///baz/quux")
    end if IS_JRUBY

    def test_expand_path_as_jar_with_file_prefix
      assert_equal "file:/my.jar!/foo/bar", File.expand_path("file:/my.jar!/foo/bar")
      assert_equal "file:/my.jar!/foo/bar", File.expand_path("file:/my.jar!/../foo/bar")
      assert_equal "file:/my.jar!/bar", File.expand_path("file:/my.jar!/foo/../bar")
      assert_equal "file:/my.jar!/foo/bar/baz", File.expand_path("baz", "file:/my.jar!/foo/bar")
      assert_equal "file:/my.jar!/foo/bar", File.expand_path("file:/my.jar!/foo/bar", "file:/my.jar!/baz/quux")
      assert_equal "file:/my.jar!/foo/bar", File.expand_path("../../foo/bar", "file:/my.jar!/baz/quux")
      #assert_equal "file:/my.jar!/foo/bar", File.expand_path("../../../foo/bar", "file:/my.jar!/baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_jar_file_prefix
      assert_equal "jar:file:/my.jar!/foo/bar", File.expand_path("jar:file:/my.jar!/foo/bar")
      assert_equal "jar:file:/my.jar!/bar", File.expand_path("jar:file:/my.jar!/foo/../bar")
      assert_equal "jar:file:/my.jar!/foo/bar/baz", File.expand_path("baz", "jar:file:/my.jar!/foo/bar")
      assert_equal "jar:file:/my.jar!/foo/bar", File.expand_path("jar:file:/my.jar!/foo/bar", "file:/my.jar!/baz/quux")
      assert_equal "jar:file:/my.jar!/foo/bar", File.expand_path("../../foo/bar", "jar:file:/my.jar!/baz/quux")
      #assert_equal "jar:file:/my.jar!/foo/bar", File.expand_path("../../../foo/bar", "jar:file:/my.jar!/baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_uri_file_prefix_slash
      # file-protocol file://host/path, treated as file:/path
      assert_equal "uri:file:/foo/bar", File.expand_path("uri:file:/foo/bar")
      #assert_equal "uri:file:/bar", File.expand_path("uri:file:/foo/../bar")
      # TODO why uri:file:// ?
      assert_equal "uri:file://foo/bar/baz", File.expand_path("baz", "uri:file:/foo/bar")
      assert_equal "uri:file:/foo/bar", File.expand_path("uri:file:/foo/bar", "uri:file:/baz/quux")
      # TODO why uri:file:// ?
      assert_equal "uri:file://foo/bar", File.expand_path("../../foo/bar", "uri:file:/baz/quux")
      assert_equal "uri:file:/foo/bar", File.expand_path("../../../foo/bar", "uri:file:/baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_uri_file_prefix_slash_slash
      # file-protocol file://host/path
      assert_equal "uri:file://foo/bar", File.expand_path("uri:file://foo/bar")
      assert_equal "uri:file://bar", File.expand_path("uri:file://foo/../bar")
      assert_equal "uri:file://foo/bar/baz", File.expand_path("baz", "uri:file://foo/bar")
      assert_equal "uri:file://foo/bar", File.expand_path("uri:file://foo/bar", "uri:file://baz/quux")
      assert_equal "uri:file://foo/bar", File.expand_path("../../foo/bar", "uri:file://baz//quux")
      # TODO remove the use of JRubyFile
      #assert_equal "uri:file://foo/bar", File.expand_path("../../../foo/bar", "uri:file://baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_uri_file_prefix_slash_slash_slash
      # file-protocol file://host/path i.e. host is empty string here
      assert_equal "uri:file:///foo/bar", File.expand_path("uri:file:///foo/bar")
      assert_equal "uri:file:///bar", File.expand_path("uri:file:///foo/../bar")
      # TODO remove the use of JRubyFile
      #assert_equal "uri:file:///foo/bar/baz", File.expand_path("baz", "uri:file:///foo/bar")
      assert_equal "uri:file:///foo/bar", File.expand_path("uri:file:///foo/bar", "uri:file://baz/quux")
      #assert_equal "uri:file:///foo/bar", File.expand_path("../../foo/bar", "uri:file:///baz//quux")
      #assert_equal "uri:file:///foo/bar", File.expand_path("../../../foo/bar", "uri:file:///baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_uri_classloader_prefix
      assert_equal "uri:classloader:/foo/bar", File.expand_path("uri:classloader:/foo/bar")
      assert_equal "uri:classloader:/bar", File.expand_path("uri:classloader:/foo/../bar")
      assert_equal "uri:classloader:/foo/bar/baz", File.expand_path("baz", "uri:classloader:/foo/bar")
      assert_equal "uri:classloader:/foo/bar", File.expand_path("uri:classloader:/foo/bar", "uri:classloader:/baz/quux")
      assert_equal "uri:classloader:/foo/bar", File.expand_path("../../foo/bar", "uri:classloader:/baz/quux")
      assert_equal "uri:classloader:/foo/bar", File.expand_path("../../../foo/bar", "uri:classloader:/baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_classpath_prefix
      assert_equal "classpath:/foo/bar", File.expand_path("classpath:/foo/bar")
      assert_equal "classpath:/bar", File.expand_path("classpath:/foo/../bar")
      assert_equal "classpath:/foo/bar/baz", File.expand_path("baz", "classpath:/foo/bar")
      assert_equal "classpath:/foo/bar", File.expand_path("classpath:/foo/bar", "classpath:/baz/quux")
      assert_equal "classpath:/foo/bar", File.expand_path("../../foo/bar", "classpath:/baz/quux")
      assert_equal "classpath:/foo/bar", File.expand_path("../../../foo/bar", "classpath:/baz/quux")
    end if IS_JRUBY

    def test_expand_path_with_file_url_relative_path
      assert_equal "file:#{Dir.pwd}/foo/bar", File.expand_path("file:foo/bar")
    end if IS_JRUBY

    # GH-3176
    def test_expand_path_with_relative_reference_and_inside_uri_classloader
      Dir.chdir( 'uri:classloader:/') do
        assert_equal 'uri:classloader:/something/foo', File.expand_path('foo', 'something')
        assert_equal 'uri:classloader:/foo', File.expand_path('foo', '.')
      end
    end if IS_JRUBY

    # JRUBY-5219
    def test_expand_path_looks_like_url
      assert_equal "classpath:/META-INF/jruby.home", File.expand_path("classpath:/META-INF/jruby.home")
      # file-protocol file://host/path
      assert_equal "uri:file://META-INF/jruby.home", File.expand_path("uri:file://META-INF/jruby.home")
      assert_equal "http://example.com/a.jar", File.expand_path("http://example.com/a.jar")
      assert_equal "http://example.com/", File.expand_path("..", "http://example.com/a.jar")
      assert_equal "classpath:/foo/bar/baz", File.expand_path("baz", "classpath:/foo/bar")
      assert_equal "classpath:/foo/bar", File.expand_path("classpath:/foo/bar", "classpath:/baz/quux")
      assert_equal "classpath:/foo", File.expand_path("..", "classpath:/foo/bar")
      assert_equal "uri:jar://foo/bar/baz", File.expand_path("baz", "uri:jar://foo/bar")
      # file-protocol file://host/path
      assert_equal "uri:file://foo/bar", File.expand_path("uri:file://foo/bar", "uri:file://baz/quux")
      assert_equal "uri:jar://foo", File.expand_path("..", "uri:jar://foo/bar")
    end if IS_JRUBY

    def test_mkdir_with_non_file_uri_raises_error
      assert_raises(Errno::ENOTDIR) { FileUtils.mkdir_p("classpath:/META-INF/MANIFEST.MF") }
      assert !File.directory?("classpath:/META-INF/MANIFEST.MF")
    end if IS_JRUBY

    def test_mkdir_with_file_uri_works_as_expected
      FileUtils.mkdir("file:test_mkdir_with_file_uri_works_as_expected")
      assert File.directory?("test_mkdir_with_file_uri_works_as_expected")
      assert File.directory?("file:test_mkdir_with_file_uri_works_as_expected")
    ensure
      FileUtils.rm_rf("test_mkdir_with_file_uri_works_as_expected")
    end if IS_JRUBY

    # GH-2972
    def test_mkdir_with_file_uri_and_absolute_path_works_as_expected
      # the extra slashes were the problem
      FileUtils.mkdir("file://#{Dir.pwd}/test_mkdir_with_file_uri_works_as_expected")
      assert File.directory?("#{Dir.pwd}/test_mkdir_with_file_uri_works_as_expected")
    ensure
      FileUtils.rm_rf("#{Dir.pwd}/test_mkdir_with_file_uri_works_as_expected")
    end if IS_JRUBY

    def test_expand_path_corner_case
      # this would fail on MRI 1.8.6 (MRI returns "/foo").
      assert_equal("//foo", File.expand_path("../foo", "//bar"))
    end

    def test_realpath
      assert_equal('/', File.realpath('/'))
    end
  end # if WINDOWS else ... end

  def test_paths_do_not_get_normalized_on_non_windows
    # Linux doesn't mind '\' in file/dir names :
    Dir.mkdir backslash = '_back\\slash'
    path = File.expand_path backslash
    assert path.end_with?(backslash), "path: #{path.inspect} does not end with: #{backslash.inspect}"
    path = File.realpath path
    assert path.end_with?(backslash), "path: #{path.inspect} does not end with: #{backslash.inspect}"
    File.realpath backslash
  ensure
    Dir.rmdir '_back\\slash' rescue nil
  end unless WINDOWS

  def test_dirname
    assert_equal(".", File.dirname(""))
    assert_equal(".", File.dirname("."))
    assert_equal(".", File.dirname(".."))
    assert_equal(".", File.dirname("a"))
    assert_equal(".", File.dirname("./a"))
    assert_equal("./a", File.dirname("./a/b"))
    assert_equal("/", File.dirname("/"))
    assert_equal("/", File.dirname("/a"))
    assert_equal("/a", File.dirname("/a/b"))
    assert_equal("/a", File.dirname("/a/b/"))
    assert_equal("/", File.dirname("/"))
  end

  def test_dirname_file_protocol
    assert_equal("file:/a", File.dirname("file:/a/b"))
    assert_equal("file:/", File.dirname("file:/a"))
    assert_equal("file:/", File.dirname("file:/"))
    assert_equal("uri:file:/a", File.dirname("uri:file:/a/b"))
    assert_equal("uri:file:/", File.dirname("uri:file:/a"))
    assert_equal("uri:file:/", File.dirname("uri:file:/"))
  end if IS_JRUBY

  def test_dirname_uri_classloader_protocol
    assert_equal("uri:classloader:/a", File.dirname("uri:classloader:/a/b"))
    assert_equal("uri:classloader:/", File.dirname("uri:classloader:/a"))
    assert_equal("uri:classloader:/", File.dirname("uri:classloader:/"))
  end if IS_JRUBY

  def test_dirname_jar_protocol
    assert_equal("/my.jar!/a", File.dirname("/my.jar!/a/b"))
    assert_equal("/my.jar!", File.dirname("/my.jar!/a"))
    assert_equal("/my.jar!", File.dirname("/my.jar!/"))
    assert_equal("file:/my.jar!/a", File.dirname("file:/my.jar!/a/b"))
    assert_equal("file:/my.jar!", File.dirname("file:/my.jar!/a"))
    assert_equal("file:/my.jar!", File.dirname("file:/my.jar!/"))
    assert_equal("jar:file:/my.jar!/a", File.dirname("jar:file:/my.jar!/a/b"))
    assert_equal("jar:file:/my.jar!", File.dirname("jar:file:/my.jar!/a"))
    assert_equal("jar:file:/my.jar!", File.dirname("jar:file:/my.jar!/"))
  end if IS_JRUBY

  def test_extname
    assert_equal("", File.extname(""))
    assert_equal("", File.extname("abc"))
    assert_equal(".foo", File.extname("abc.foo"))
    assert_equal(".foo", File.extname("abc.bar.foo"))
    assert_equal("", File.extname("abc.bar/foo"))

    assert_equal("", File.extname(".bashrc"))
    assert_equal("", File.extname("."))
    assert_equal("", File.extname("/."))
    assert_equal("", File.extname(".."))
    assert_equal("", File.extname(".foo."))
    assert_equal("", File.extname("foo."))
  end

  def test_fnmatch
    assert_equal(true, File.fnmatch('cat', 'cat'))
    assert_equal(false, File.fnmatch('cat', 'category'))
    assert_equal(false, File.fnmatch('c{at,ub}s', 'cats'))
    assert_equal(false, File.fnmatch('c{at,ub}s', 'cubs'))
    assert_equal(false, File.fnmatch('c{at,ub}s', 'cat'))

    assert_equal(true, File.fnmatch('c?t', 'cat'))
    assert_equal(false, File.fnmatch('c\?t', 'cat'))
    assert_equal(true, File.fnmatch('c\?t', 'c?t'))
    assert_equal(false, File.fnmatch('c??t', 'cat'))
    assert_equal(true, File.fnmatch('c*', 'cats'));
    assert_equal(true, File.fnmatch('c*t', 'cat'))
    #assert_equal(true, File.fnmatch('c\at', 'cat')) # Doesn't work correctly on both Unix and Win32
    assert_equal(false, File.fnmatch('c\at', 'cat', File::FNM_NOESCAPE))
    assert_equal(true, File.fnmatch('a?b', 'a/b'))
    assert_equal(false, File.fnmatch('a?b', 'a/b', File::FNM_PATHNAME))
    assert_equal(false, File.fnmatch('a?b', 'a/b', File::FNM_PATHNAME))

    assert_equal(false, File.fnmatch('*', '.profile'))
    assert_equal(true, File.fnmatch('*', '.profile', File::FNM_DOTMATCH))
    assert_equal(true, File.fnmatch('*', 'dave/.profile'))
    assert_equal(true, File.fnmatch('*', 'dave/.profile', File::FNM_DOTMATCH))
    assert_equal(false, File.fnmatch('*', 'dave/.profile', File::FNM_PATHNAME))

    assert_equal(false, File.fnmatch("/.ht*",""))
    assert_equal(false, File.fnmatch("/*~",""))
    assert_equal(false, File.fnmatch("/.ht*","/"))
    assert_equal(false, File.fnmatch("/*~","/"))
    assert_equal(false, File.fnmatch("/.ht*",""))
    assert_equal(false, File.fnmatch("/*~",""))
    assert_equal(false, File.fnmatch("/.ht*","/stylesheets"))
    assert_equal(false, File.fnmatch("/*~","/stylesheets"))
    assert_equal(false, File.fnmatch("/.ht*",""))
    assert_equal(false, File.fnmatch("/*~",""))
    assert_equal(false, File.fnmatch("/.ht*","/favicon.ico"))
    assert_equal(false, File.fnmatch("/*~","/favicon.ico"))

    # JRUBY-1986, make sure that fnmatch is sharing aware
    assert_equal(true, File.fnmatch("foobar"[/foo(.*)/, 1], "bar"))
  end

  # JRUBY-2196
  def test_fnmatch_double_star
    assert(File.fnmatch('**/foo', 'a/b/c/foo', File::FNM_PATHNAME))
    assert(File.fnmatch('**/foo', '/foo', File::FNM_PATHNAME))
    assert(!File.fnmatch('**/foo', 'a/.b/c/foo', File::FNM_PATHNAME))
    assert(File.fnmatch('**/foo', 'a/.b/c/foo', File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(File.fnmatch('**/foo', '/root/foo', File::FNM_PATHNAME))
    assert(File.fnmatch('**/foo', 'c:/root/foo', File::FNM_PATHNAME))
    assert(File.fnmatch("lib/**/*.rb", "lib/a.rb", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(File.fnmatch("lib/**/*.rb", "lib/a/b.rb", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(File.fnmatch('**/b/**/*', 'c/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('c/**/b/**/*', 'c/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('c**/**/b/**/*', 'c/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('h**o/**/b/**/*', 'hello/a/b/c/t', File::FNM_PATHNAME))
    assert(!File.fnmatch('h**o/**/b/**', 'hello/a/b/c/t', File::FNM_PATHNAME))
    assert(File.fnmatch('**', 'hello', File::FNM_PATHNAME))
    assert(!File.fnmatch('**/', 'hello', File::FNM_PATHNAME))
    assert(File.fnmatch('**/*', 'hello', File::FNM_PATHNAME))
    assert(File.fnmatch("**/*", "one/two/three/four", File::FNM_PATHNAME))
    assert(!File.fnmatch("**", "one/two/three", File::FNM_PATHNAME))
    assert(!File.fnmatch("**/three", ".one/two/three", File::FNM_PATHNAME))
    assert(File.fnmatch("**/three", ".one/two/three", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(!File.fnmatch("*/**", "one/two/three", File::FNM_PATHNAME))
    assert(!File.fnmatch("*/**/", "one/two/three", File::FNM_PATHNAME))
    assert(File.fnmatch("*/**/*", "one/two/three", File::FNM_PATHNAME))
    assert(File.fnmatch("**/*", ".one/two/three/four", File::FNM_PATHNAME | File::FNM_DOTMATCH))
    assert(!File.fnmatch("**/.one/*", ".one/.two/.three/.four", File::FNM_PATHNAME | File::FNM_DOTMATCH))
  end

  # JRUBY-2199
  def test_fnmatch_bracket_pattern
    assert(!File.fnmatch('[a-z]', 'D'))
    assert(File.fnmatch('[a-z]', 'D', File::FNM_CASEFOLD))
    assert(!File.fnmatch('[a-zA-Y]', 'Z'))
    assert(File.fnmatch('[^a-zA-Y]', 'Z'))
    assert(File.fnmatch('[a-zA-Y]', 'Z', File::FNM_CASEFOLD))
    assert(File.fnmatch('[a-zA-Z]', 'Z'))
    assert(File.fnmatch('[a-zA-Z]', 'A'))
    assert(File.fnmatch('[a-zA-Z]', 'a'))
    assert(!File.fnmatch('[b-zA-Z]', 'a'))
    assert(File.fnmatch('[^b-zA-Z]', 'a'))
    assert(File.fnmatch('[b-zA-Z]', 'a', File::FNM_CASEFOLD))
  end

  def test_join
    [
      ["a", "b", "c", "d"],
      ["a"],
      [],
      ["a", "b", "..", "c"]
    ].each do |a|
      assert_equal(a.join(File::SEPARATOR), File.join(*a))
    end

    assert_equal("////heh////bar/heh", File.join("////heh////bar", "heh"))
    assert_equal("/heh/bar/heh", File.join("/heh/bar/", "heh"))
    assert_equal("/heh/bar/heh", File.join("/heh/bar/", "/heh"))
    assert_equal("/heh//bar/heh", File.join("/heh//bar/", "/heh"))
    assert_equal("/heh/bar/heh", File.join("/heh/", "/bar/", "/heh"))
    assert_equal("/HEH/BAR/FOO/HOH", File.join("/HEH", ["/BAR", "FOO"], "HOH"))
    assert_equal("/heh/bar", File.join("/heh//", "/bar"))
    assert_equal("/heh///bar", File.join("/heh", "///bar"))
  end

  def test_split
    assert_equal([".", ""], File.split(""))
    assert_equal([".", "."], File.split("."))
    assert_equal([".", ".."], File.split(".."))
    assert_equal(["/", "/"], File.split("/"))
    assert_equal([".", "a"], File.split("a"))
    assert_equal([".", "a"], File.split("a/"))
    assert_equal(["a", "b"], File.split("a/b"))
    assert_equal(["a/b", "c"], File.split("a/b/c"))
    assert_equal(["/", "a"], File.split("/a"))
    assert_equal(["/", "a"], File.split("/a/"))
    assert_equal(["/a", "b"], File.split("/a/b"))
    assert_equal(["/a/b", "c"], File.split("/a/b/c"))
    #assert_equal(["//", "a"], File.split("//a"))
    assert_equal(["../a/..", "b"], File.split("../a/../b/"))
  end

  def test_io_readlines
    # IO#readlines, IO::readlines, open, close, delete, ...
    assert_raises(Errno::ENOENT) { File.open("NO_SUCH_FILE_EVER") }
    f = open("testFile_tmp", "w")
    f.write("one\ntwo\nthree\n")
    f.close

    f = open("testFile_tmp")
    assert_equal(["one", "two", "three"],
               f.readlines.collect {|l| l.strip })
    f.close

    assert_equal(["one", "two", "three"],
               IO.readlines("testFile_tmp").collect {|l| l.strip })
    assert(File.delete("testFile_tmp"))
  end

  def test_mkdir
    begin
      Dir.mkdir("dir_tmp")
      assert(File.lstat("dir_tmp").directory?)
    ensure
      Dir.rmdir("dir_tmp")
    end
  end

  def test_directory_query # - directory?
    begin
      Dir.mkdir("dir_tmp")
      assert(File.directory?("dir_tmp"))
      assert(! File.directory?("test/jruby/test_file.rb"))
      assert(! File.directory?("dir_not_tmp"))
      result = jruby("-e \"print File.directory?('dir_not_tmp');print File.directory?('dir_tmp');print File.directory?('test/jruby/test_file.rb')\"", 'jruby.native.enabled' => 'false')
      assert_equal(result, 'falsetruefalse')
    ensure
      Dir.rmdir("dir_tmp") rescue nil
    end
  end

  def test_file_query # - file?
    assert(File.file?('test/jruby/test_file.rb'))
    assert(! File.file?('test'))
    assert(! File.file?('test_not'))
    result = jruby("-e \"print File.file?('test_not');print File.file?('test');print File.file?('test/jruby/test_file.rb')\"", 'jruby.native.enabled' => 'false' )
    assert_equal('falsefalsetrue', result)
  end

  def test_readable_query # - readable?
    assert(File.readable?('test/jruby/test_file.rb'))
    assert(File.readable?('test'))
    assert(! File.readable?('test_not'))
    result = jruby("-e \"print File.readable?('test_not');print File.readable?('test');print File.readable?('test/jruby/test_file.rb')\"", 'jruby.native.enabled' => 'false' )
    assert_equal('falsetruetrue', result)
  end

  [ :executable?, :executable_real? ].each do |method|
    define_method :"test_#{method}_query" do # - executable?/executable_real?
      if WINDOWS
        exec_file = 'bin/jruby.bat'
      else
        exec_file = 'bin/jruby.sh'
      end
      assert(File.send(method, exec_file))
      assert(!File.send(method, 'test/test_file.rb'))
      assert(File.send(method, 'test'))
      assert(!File.send(method, 'test_not'))
      result = jruby("-e \"print File.#{method}('#{exec_file}');print File.#{method}('test_not');print File.#{method}('test');print File.#{method}('test/test_file.rb')\"", 'jruby.native.enabled' => 'false' )
      assert_equal('truefalsetruefalse', result)
    end
  end

  def test_file_exist_query
    assert(File.exist?('test'))
    assert(! File.exist?('test_not'))
    assert(jruby("-e \"print File.exists?('test_not');print File.exists?('test')\"", 'jruby.native.enabled' => 'false' ) == 'falsetrue')
  end

  def test_file_exist_in_jar_file
    assert(File.exist?("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar") + "!/abc/foo.rb"))
    assert(File.exist?("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar") + "!/inside_jar.rb"))
    assert(!File.exist?("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar") + "!/inside_jar2.rb"))
    assert(File.exist?("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar") + "!/"))
  end if IS_JRUBY

  def with_load_path(entry)
    begin
      $LOAD_PATH.unshift entry
      yield
    ensure
      $LOAD_PATH.shift
    end
  end

  def test_require_from_jar_url_with_spaces_in_load_path
    assert_nothing_raised do
      with_load_path("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar") + "!/abc") do
        assert require('foo')
        assert $LOADED_FEATURES.pop =~ /foo\.rb$/
      end

      with_load_path("file:" + File.expand_path("test/jruby/dir with spaces/test_jar.jar") + "!") do
        assert require('abc/foo')
        assert $LOADED_FEATURES.pop =~ /foo\.rb$/
      end

      with_load_path(File.expand_path("test/jruby/dir with spaces/test_jar.jar!/")) do
        assert require('abc/foo')
        assert $LOADED_FEATURES.pop =~ /foo\.rb$/
      end
    end
  end if IS_JRUBY

  def test_file_read_in_jar_file
    assert_equal("foobarx\n", File.read("file:" + File.expand_path("test/jruby/test_jar2.jar") + "!/test_value.rb"))

    assert_raises(Errno::ENOENT) do
      File.read("file:" + File.expand_path("test/jruby/test_jar2.jar") + "!/inside_jar2.rb")
    end

    assert_raises(Errno::ENOENT) do
      File.read("file:" + File.expand_path("test/jruby/test_jar3.jar") + "!/inside_jar2.rb")
    end

    val = ""
    open("file:" + File.expand_path("test/jruby/test_jar2.jar") + "!/test_value.rb") do |f|
      val = f.read
    end
    assert_equal "foobarx\n", val

    values = ""
    File.open("file:" + File.expand_path("test/jruby/test_jar2.jar") + "!/test_value.rb") do |f|
      f.each do |s|
        values << s
      end
    end

    assert_equal "foobarx\n", values
  end if IS_JRUBY

  # JRUBY-2357
  def test_truncate_file_in_jar_file
    File.open("file:" + File.expand_path("test/jruby/test_jar2.jar") + "!/test_value.rb", "r+") do |f|
      assert_raise(Errno::EINVAL) { f.truncate(2) }
    end
  end if IS_JRUBY

  # JRUBY-1886
  def test_file_truncated_after_changing_directory
    subdir = "./testDir_1"
    Dir.mkdir(subdir)
    Dir.chdir(subdir) { |dir|
      begin
        file = File.open("__dummy_file.txt", "wb")
        file.write("dummy text")
        file.close
        assert_nothing_raised { File.truncate(file.path, 0) }
      ensure
        file.close rescue nil
        File.unlink(file.path)
      end
    }
  ensure
    Dir.rmdir(subdir)
  end

  def test_file_size_query
    assert(File.size?('pom.xml'))
  end

  # JRUBY-2275
  def test_file_size_empty_file
    filename = "__empty_test__file"
    File.open(filename, "w+") { }
    assert_equal(nil, File.size?(filename))
    assert_equal(nil, FileTest.size?(filename))
    assert_equal(0, File.size(filename))
    assert_equal(0, FileTest.size(filename))
  ensure
    File.delete(filename)
  end

  # JRUBY-2524
  def test_filetest_exists_uri_prefixes
    assert(!FileTest.exists?("file:/!"))
  end if IS_JRUBY

  def test_file_exists_uri_prefixes
    assert(File.exists?("file:test/jruby/dir with spaces/test_jar.jar!/abc/foo.rb"))
    assert(File.exists?("jar:file:test/jruby/dir with spaces/test_jar.jar!/abc/foo.rb"))
    assert(File.exists?("jar:file:test/jruby/dir with spaces/./test_jar.jar!/abc/./foo/../foo.rb"))
  end if IS_JRUBY

  # JRUBY-2524
  def test_file_stat_uri_prefixes
    assert_raise(Errno::ENOENT) do
      File.lstat("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.lstat("file:!")
    end

    assert_raise(Errno::ENOENT) do
      File.stat("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.stat("file:!")
    end
  end

  # JRUBY-2524
  def test_file_time_uri_prefixes
    assert_raise(Errno::ENOENT) do
      File.atime("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.atime("file:!")
    end

    assert_raise(Errno::ENOENT) do
      File.ctime("file:")
    end
    assert_raise(Errno::ENOENT) do
      File.ctime("file:!")
    end
  end

  def test_file_open_utime
    filename = "__test__file"
    File.open(filename, "w") {|f| }
    time = Time.now - 3600
    begin
    File.utime(time, time, filename)
  rescue Object => o
    o.printStackTrace
    o.getCause.printStackTrace
  end
    # File mtime resolution may not be sub-second on all platforms (e.g., windows)
    # allow for some slop
    assert((time.to_i - File.atime(filename).to_i).abs < 5)
    assert((time.to_i - File.mtime(filename).to_i).abs < 5)
    File.unlink(filename)
  end

  def test_file_utime_nil
    filename = '__test__file'
    File.open(filename, 'w') {|f| }
    time = File.mtime(filename)
    sleep 2
    File.utime(nil, nil, filename)
    assert((File.atime(filename).to_i - time.to_i) >= 2)
    assert((File.mtime(filename).to_i - time.to_i) >= 2)
    File.unlink(filename)
  end

  def test_file_utime_bad_time_raises_typeerror
    args = [ [], {}, '4000' ]
    filename = '__test__file'
    File.open(filename, 'w') {|f| }
    args.each do |arg|
      assert_raises(TypeError) {  File.utime(arg, nil, filename) }
      assert_raises(TypeError) {  File.utime(nil, arg, filename) }
      assert_raises(TypeError) {  File.utime(arg, arg, filename) }
    end
    time = Time.now
    assert_raises(TypeError) {  File.utime(time, nil, filename) }
    assert_raises(TypeError) {  File.utime(nil, time, filename) }
    File.unlink(filename)
  end

  # JRUBY-1982 and JRUBY-1983
  def test_file_mtime_after_fileutils_touch
    filename = '__test__file'
    File.open(filename, 'w') {|f| }
    time = File.mtime(filename)

    FileUtils.touch(filename)
    # File mtime resolution may not be sub-second on all platforms (e.g., windows)
    # allow for some slop
    assert((time.to_i - File.mtime(filename).to_i).abs < 2)
  end

  def test_file_stat # File::Stat tests
    stat = File.stat('test');
    stat2 = File.stat('pom.xml');
    assert(stat.directory?)
    assert(stat2.file?)
    assert_equal("directory", stat.ftype)
    assert_equal("file", stat2.ftype)
    assert(stat2.readable?)
  end

  if WINDOWS && JRuby::runtime.posix.native?
    # JRUBY-2351
    def test_not_implemented_methods_on_windows
      # the goal here is to make sure that those "weird"
      # POSIX methods don't break JRuby, since there were
      # numerous regressions in this area
      begin
        # TODO: See JRUBY-2818.
        File.readlink('pom.xml')
      rescue NotImplementedError
      rescue Errno::EINVAL  # TODO: this exception is wrong (see bug above)
      end

      begin
        # TODO: See JRUBY-2817.
        File.chown(100, 100, 'pom.xml')
      rescue NotImplementedError
      end

      assert_raise(NotImplementedError) { File.lchown(100, 100, 'pom.xml') }
      assert_raise(NotImplementedError) { File.lchmod(0644, 'pom.xml') }
    end
  end

  unless(WINDOWS) # no symlinks on Windows
    def test_file_symlink
      # Test File.symlink? if possible
      system("ln -s pom.xml pom.xml.link")
      if File.exist? "pom.xml.link"
        assert(File.symlink?("pom.xml.link"))
        # JRUBY-683 -  Test that symlinks don't effect Dir and File.expand_path
        assert_equal(['pom.xml.link'], Dir['pom.xml.link'])
        assert_equal(File.expand_path('.') + '/pom.xml.link', File.expand_path('pom.xml.link'))
        File.delete("pom.xml.link")
      end
    end
    def test_require_symlink
      # Create a ruby file that sets a global variable to its view of __FILE__
      f = File.open("real_file.rb", "w")
      f.write("$test_require_symlink_filename=__FILE__")
      f.close()
      system("ln -s real_file.rb linked_file.rb")
      assert(File.symlink?("linked_file.rb"))
      # JRUBY-5167 - Test that symlinks don't effect __FILE__ during load or require
      # Note: This bug only manifests for absolute paths that point to symlinks.
      abs_path = File.join(Dir.pwd, "real_file.rb")
      require abs_path
      assert_equal($test_require_symlink_filename, abs_path)
      abs_path_linked = File.join(Dir.pwd, "linked_file.rb")
    ensure
      File.delete("real_file.rb")
      File.delete("linked_file.rb")
    end
  end

  def test_file_times
    assert_nothing_raised do
      [ "pom.xml", "file:test/jruby/dir with spaces/test_jar.jar!/abc/foo.rb" ].each do |file|
        File.mtime(file)
        File.atime(file)
        File.ctime(file)
        File.new(file).mtime
        File.new(file).atime
        File.new(file).ctime
      end
    end if IS_JRUBY

    assert_raises(Errno::ENOENT) { File.mtime("NO_SUCH_FILE_EVER") }
  end

  def test_file_times_types
    assert_instance_of Time, File.mtime("pom.xml")
    assert_instance_of Time, File.atime("pom.xml")
    assert_instance_of Time, File.ctime("pom.xml")
    assert_instance_of Time, File.new("pom.xml").mtime
    assert_instance_of Time, File.new("pom.xml").atime
    assert_instance_of Time, File.new("pom.xml").ctime
  end

  def test_more_constants
    if Java::java.lang.System.get_property("file.separator") == '/'
      assert_equal(nil, File::ALT_SEPARATOR)
    else
      assert_equal("\\", File::ALT_SEPARATOR)
    end
  end if IS_JRUBY

  # JRUBY-2572
  def test_fnm_syscase_constant
    if (WINDOWS)
      assert_equal(File::FNM_CASEFOLD, File::FNM_SYSCASE)
      assert File.fnmatch?('cat', 'CAT', File::FNM_SYSCASE)
    else
      assert_not_equal(File::FNM_CASEFOLD, File::FNM_SYSCASE)
      assert !File.fnmatch?('cat', 'CAT', File::FNM_SYSCASE)
    end
  end

  def test_truncate
    # JRUBY-1025: negative int passed to truncate should raise EINVAL
    filename = "__truncate_test_file"
    assert_raises(Errno::EINVAL) {
      begin
        f = File.open(filename, 'w')
        f.truncate(-1)
      ensure
        f.close
      end
    }
    assert_raises(Errno::EINVAL) {
      File.truncate(filename, -1)
    }
  ensure
    File.delete(filename)
  end

  def test_file_utf8
    # name contains a German "umlaut", maybe this should be encoded as Unicode integer (\u00FC) somehow
    filename = 'jrüby'
    f = File.new(filename, File::CREAT)
    begin
      assert_equal(nil, f.read(1))

      assert File.file?(filename)
      assert File.exist?(filename)
    ensure
      f.close
      File.delete(filename)

      assert !File.file?(filename)
      assert !File.exist?(filename)
    end
  end

  # GH-3074
  def test_file_with_absolute_path_and_uri_path_as_cwd
    filename = File.expand_path( 'test_absolute_path_and_uri_path_as_cwd' )

    Dir.chdir('uri:classloader:/') do
      begin
        f = File.new(filename, File::CREAT)
        assert_equal(nil, f.read(1))

        assert File.file?(filename)
        assert File.exist?(filename)
      ensure
        f.close
        File.delete(filename)

        assert !File.file?(filename)
        assert !File.exist?(filename)
      end
    end
  end if IS_JRUBY

  def test_file_create
    filename = '2nnever'
    f = File.new(filename, File::CREAT)
    begin
      assert_equal(nil, f.read(1))
    ensure
      f.close
      File.delete(filename)
    end

    f = File.new(filename, File::CREAT)
    begin
      assert_raises(IOError) { f << 'b' }
    ensure
      f.close
      File.delete(filename)
    end
  end

  # http://jira.codehaus.org/browse/JRUBY-1023
  def test_file_reuse_fileno
    pend 'TODO: STDIN.fileno (0) is NOT being re-used on Windows' if WINDOWS
    fh = File.new(STDIN.fileno, 'r')
    assert_equal(STDIN.fileno, fh.fileno)
  end

  # http://jira.codehaus.org/browse/JRUBY-1231
  def test_file_directory_empty_name
    assert !File.directory?("")
    assert !FileTest.directory?("")
    assert_raises(Errno::ENOENT) { File::Stat.new("") }
  end

  def test_file_test
    assert(FileTest.file?('test/jruby/test_file.rb'))
    assert(! FileTest.file?('test'))
  end

  def test_flock
    filename = '__lock_test__'
    file = File.open(filename,'w')
    file.flock(File::LOCK_EX | File::LOCK_NB)
    assert_equal(0, file.flock(File::LOCK_UN | File::LOCK_NB))
    file.close
    File.delete(filename)
  end

  def test_truncate_doesnt_create_file
    name = "___foo_bar___"
    assert(!File.exists?(name))

    assert_raises(Errno::ENOENT) { File.truncate(name, 100) }

    assert(!File.exists?(name))
  end

  # JRUBY-2340
  def test_opening_readonly_file_for_write_raises_eacces
    filename = "__read_only__"

    begin
      File.open(filename, "w+", 0444) { }
      assert_raise(Errno::EACCES) { File.open(filename, "w") { } }
    ensure
      File.delete(filename)
    end
  end

  # JRUBY-2397
  unless(WINDOWS)
    def test_chown_accepts_nil_and_minus_one
      # chown
      assert_equal(1, File.chown(-1, -1, 'pom.xml'))
      assert_equal(1, File.chown(nil, nil, 'pom.xml'))
      # lchown
      assert_equal(1, File.lchown(-1, -1, 'pom.xml'))
      assert_equal(1, File.lchown(nil, nil, 'pom.xml'))

      File.open('pom.xml') { |file|
        # chown
        assert_equal(0, file.chown(-1, -1))
        assert_equal(0, file.chown(nil, nil))
      }
    end
  end

  unless WINDOWS
    # JRUBY-2491
    def test_umask_noarg_does_not_zero
      mask = 0200
      orig_mask = File.umask(mask)

      assert_equal(mask, File.umask)
      # Subsequent calls should still return the same umask, not zero
      assert_equal(mask, File.umask)
    ensure
      File.umask(orig_mask)
    end

    # JRUBY-4937
    def test_umask_respects_existing_umask_value
      orig_mask = File.umask
      # Cleanup old test files just in case
      FileUtils.rm_rf %w[ file_test_out.1.0644 file_test_out.2 ]

      File.umask( 0172 ) # Set umask to fixed weird test value
      open( "file_test_out.1.0644", 'w', 0707 ) { |f| assert_equal(0172, File.umask) }
      open( "file_test_out.2",      'w'       ) { |f| assert_equal(0172, File.umask) }

    ensure
      FileUtils.rm_rf %w[ file_test_out.1.0644 file_test_out.2 ]
      File.umask(orig_mask)
    end
  end

  unless WINDOWS
    def test_mode_of_tempfile_is_600
      t = Tempfile.new "tcttac.jpg"
      assert_equal 0100600, File.stat(t.path).mode
    end
  end

  # See JRUBY-2694; we don't have 1.8.7 support yet
  def test_tempfile_with_suffix
    Tempfile.open(['prefix', 'suffix']) { |f|
      assert_match(/^prefix/, File.basename(f.path))
      assert_match(/suffix$/, f.path)
    }
  end

  def test_file_size
    size = File.size('pom.xml')
    assert(size > 0)
    assert_equal(size, File.size(File.new('pom.xml')))
  end

  # JRUBY-4073
  def test_file_sizes
    filename = '100_bytes.bin'
    begin
      File.open(filename, 'wb+') { |f|
        f.write('0' * 100)
      }
      assert_equal(100, File.size(filename))
      assert_equal(100, File.stat(filename).size)
      assert_equal(100, File.stat(filename).size?)
      assert_match(/\ssize=100,/, File.stat(filename).inspect)
      assert_equal(100, File::Stat.new(filename).size)
      assert_equal(100, File::Stat.new(filename).size?)
      assert_match(/\ssize=100,/, File::Stat.new(filename).inspect)
    ensure
      File.unlink(filename)
    end
  end

  # JRUBY-4149
  def test_open_file_sizes
    filename = '100_bytes.bin'
    begin
      File.open(filename, 'wb+') { |f|
        f.write('0' * 100)
        f.flush; f.flush

        assert_equal(100, f.stat.size)
        assert_equal(100, f.stat.size?)
        assert_match(/\ssize=100,/, f.stat.inspect)

        assert_equal(100, File.size(filename))
        assert_equal(100, File.stat(filename).size)
        assert_equal(100, File.stat(filename).size?)
        assert_match(/\ssize=100,/, File.stat(filename).inspect)

        assert_equal(100, File::Stat.new(filename).size)
        assert_equal(100, File::Stat.new(filename).size?)
        assert_match(/\ssize=100,/, File::Stat.new(filename).inspect)
      }
    ensure
      File.unlink(filename)
    end
  end

  # JRUBY-4537: File.open raises Errno::ENOENT instead of Errno::EACCES
  def test_write_open_permission_denied
    t = Tempfile.new('tmp' + File.basename(__FILE__))
    t.close
    File.open(t.path, 'w') {}
    File.chmod(0555, t.path)
    # jruby 1.4 raises ENOENT here
    assert_raises(Errno::EACCES) do
      File.open(t.path, 'w') {}
    end
    # jruby 1.4 raises ENOENT here
    assert_raises(Errno::EACCES) do
      File.open(t.path, File::WRONLY) {}
    end
  end

  #JRUBY-4380: File.open raises IOError instead of Errno::ENOENT
  def test_open_with_nonexisting_directory
    file_path = "/foo/bar"
    assert(!File.exist?(file_path))
    assert_raises(Errno::ENOENT) { File.open(file_path, "wb") }
  end

  def test_open_file_in_jar
    File.open('file:test/jruby/dir with spaces/test_jar.jar!/abc/foo.rb'){}
    File.open('jar:file:test/jruby/dir with spaces/test_jar.jar!/abc/foo.rb'){}
  end if IS_JRUBY

  # JRUBY-3634: File.read or File.open with a url to a file resource fails with StringIndexOutOfBounds exception
  def test_file_url
    path = File.expand_path(__FILE__)
    expect = File.open(__FILE__, mode_string = "rb").read(100)
    got = File.open("file:///#{path}", mode_string = "rb").read(100)

    assert_equal(expect, got)
  end if IS_JRUBY

  def test_basename_unicode
    utf8_filename = "dir/glk\u00a9.pdf"
    assert_equal("glk\u00a9.pdf", File.basename(utf8_filename))
  end

  def test_file_stat_with_missing_path
    assert_raise(Errno::ENOENT) {
      File::Stat.new("file:" + File.expand_path("test/test_jar2.jar") + "!/foo_bar.rb").file?
    }
  end unless WINDOWS

  # JRUBY-4859
  def test_file_delete_directory
    Dir.mkdir("dir_tmp")
    assert_raise(Errno::EISDIR) { File.delete "dir_tmp" }
  ensure
    Dir.rmdir("dir_tmp")
  end

  unless WINDOWS
    # JRUBY-4927
    def test_chmod_when_chdir
      pwd = Dir.getwd
      path = Tempfile.new("somewhere").path
      FileUtils.rm_rf path
      FileUtils.mkpath path
      FileUtils.mkpath File.join(path, "src")
      Dir.chdir path

      1.upto(4) do |i|
        File.open("src/file#{i}", "w+") {|f| f.write "file#{i} raw"}
      end
      Dir['src/*'].each do |file|
        File.chmod(0o755, file)
        assert_equal 0o755, (File.stat(file).mode & 0o755)
      end
    ensure
      FileUtils.rm_rf(path)
      Dir.chdir(pwd)
    end
  end

  # JRUBY-5282
  def test_file_methods_with_closed_stream
    filename = 'test.txt'
    begin
      file = File.open(filename, 'w+')
      file.close

      %w{atime ctime lstat mtime stat}.each do |method|
        assert_raise(IOError) { file.send(method.to_sym) }
      end

      assert_raise(IOError) { file.truncate(0) }
      assert_raise(IOError) { file.chmod(777) }
      assert_raise(IOError) { file.chown(0, 0) }

    ensure
      File.unlink(filename)
    end
  end

  # JRUBY-5286
  def test_file_path_is_tainted
    filename = 'test.txt'
    io = File.new(filename, 'w')
    assert io.path.tainted?
  ensure
    io.close
    File.unlink(filename)
  end

  # jruby/jruby#2331
  def test_classpath_realpath
    assert_equal("classpath:/java/lang/String.class", File.realpath("classpath:/java/lang/String.class"))
  end if IS_JRUBY

  # GH-3401
  def test_realpath_with_uri_paths
    postfix = '!/jruby/jruby.rb'
    ['', 'uri:jar:file:', 'jar:file:', 'file:'].each do |prefix|
      result = File.realpath("#{prefix}lib/jruby.jar#{postfix}")
      # we do not care about slash or backslash
      assert_equal(File.expand_path(result), result.gsub('\\', '/'))
      assert_equal(result.start_with?(prefix), true, result + " starts with "  + prefix)
      assert_equal(result.end_with?(postfix), true, result + " ends with "  + postfix)
    end
  end if IS_JRUBY

  def test_file_exists_ending_with_slash
    assert File.exist?(__FILE__)
    assert_false File.exist?(__FILE__ + '/')

    assert File.exist?(File.dirname(__FILE__))
    assert File.exist?(File.dirname(__FILE__) + '/')
  end

  def test_inspect
    file = File.new('test/jruby/gem.jar')
    assert_equal "#<File:test/jruby/gem.jar>", file.inspect
    file.define_singleton_method(:tty?) { true }
    def file.path; 'zzz' end
    assert_equal "#<File:test/jruby/gem.jar>", file.inspect
  end

end
