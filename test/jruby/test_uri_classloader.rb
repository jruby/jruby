# -*- coding: utf-8 -*-
require 'test/unit'
require 'test/jruby/test_helper'

class TestURIClassloader < Test::Unit::TestCase
  include TestHelper

  def setup
    $CLASSPATH << File.expand_path( '../test_uri_classloader.jar', __FILE__ )
  end

  def ensure_cwd
    pwd = Dir.pwd
    begin
      yield
    ensure
      Dir.chdir pwd
    end
  end

  def test_dir_glob_on_uri_classloader_path
    all_files = Dir[ 'uri:classloader://*' ]
    assert_include all_files, 'uri:classloader://Rakefile'

    ensure_cwd do
      Dir.chdir( 'uri:classloader://' )
      assert_include Dir[ '*' ], 'Rakefile'
      assert_equal 'uri:classloader:/', Dir.pwd
    end

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://'
      assert_include Dir[ '*' ], 'Rakefile'
      #assert_equal 'uri:classloader://', Dir.pwd
    end
  end

  def test_dir_glob_on_uri_classloader_path_with_dot
    assert_include Dir[ 'uri:classloader://./*' ], 'uri:classloader://./Rakefile'

    ensure_cwd do
      Dir.chdir( 'uri:classloader://.' )
      assert_include Dir[ '*' ], 'Rakefile'
      assert_equal 'uri:classloader:/', Dir.pwd
    end

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://.'
      assert_include Dir[ '*' ], 'Rakefile'
    end
  end

  def test_dir_glob_on_uri_classloader_path_with_dot_dot
    assert_include Dir[ 'uri:classloader://lib/../*' ], 'uri:classloader://lib/../Rakefile'

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://lib/..'
      assert_include Dir[ '*' ], 'Rakefile'
    end

    ensure_cwd do
      Dir.chdir 'uri:classloader://lib/..'
      assert_include Dir[ '*' ], 'Rakefile'
      assert_equal 'uri:classloader:/', Dir.pwd
    end

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://'
      assert_include Dir[ 'lib/../*' ], 'lib/../Rakefile'
    end

    ensure_cwd do
      Dir.chdir 'uri:classloader://'
      assert_include Dir[ 'lib/../*' ], 'lib/../Rakefile'
    end
  end

  # GH-5127: subprocess cwd should be real cwd if JRuby cwd is set to classloader URI
  def test_subprocess_cwd_from_uri_classloader_cwd
    cwd = Dir.pwd
    ensure_cwd do
      Dir.chdir( 'uri:classloader:/' )
      assert_true system(WINDOWS ? 'cd > $nul' : "pwd > /dev/null")
      assert_equal cwd, (WINDOWS ? `cd` : `pwd`).chomp.tr('\\', '/') # due Windows
    end
  end

  # GH-6045: don't double up slashes when canonicalizing a URI path
  def test_uri_expand_path_does_not_double_slash
    assert_equal "uri:classloader:/foo/bar", File.expand_path("uri:classloader://foo/bar")
    assert_equal "uri:classloader:/foo/bar", File.expand_path("uri:classloader:/foo/bar");
    assert_equal "uri:classloader:/foo/bar", File.expand_path("foo/bar", "uri:classloader://")
    assert_equal "uri:classloader:/foo/bar", File.expand_path("foo/bar", "uri:classloader:/")
  end
end
