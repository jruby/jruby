# -*- coding: utf-8 -*-
require 'test/unit'
require 'test/test_helper'

class TestFile < Test::Unit::TestCase
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
    assert_equal ['uri:classloader://Rakefile'], Dir[ 'uri:classloader://*' ]

    ensure_cwd do
      Dir.chdir( 'uri:classloader://' )
      assert_equal ['Rakefile'], Dir[ '*' ]
    end

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://'
      assert_equal ['Rakefile'], Dir[ '*' ]
    end
  end

  def test_dir_glob_on_uri_classloader_path_with_dot
    assert_equal ['uri:classloader://./Rakefile'], Dir[ 'uri:classloader://./*' ]

    ensure_cwd do
      Dir.chdir( 'uri:classloader://.' )
      assert_equal ['Rakefile'], Dir[ '*' ]
    end

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://.'
      assert_equal ['Rakefile'], Dir[ '*' ]
    end
  end

  def test_dir_glob_on_uri_classloader_path_with_dot_dot
    assert_equal ['uri:classloader://lib/../Rakefile'], Dir[ 'uri:classloader://lib/../*' ]

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://lib/..'
      assert_equal ['Rakefile'], Dir[ '*' ]
    end

    ensure_cwd do
      Dir.chdir 'uri:classloader://lib/..'
      assert_equal ['Rakefile'], Dir[ '*' ]
    end

    ensure_cwd do
      JRuby.runtime.current_directory = 'uri:classloader://'
      assert_equal ['lib/../Rakefile'], Dir[ 'lib/../*' ]
    end

    ensure_cwd do
      Dir.chdir 'uri:classloader://'
      assert_equal ['lib/../Rakefile'], Dir[ 'lib/../*' ]
    end
  end
end
