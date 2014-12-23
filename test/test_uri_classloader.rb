# -*- coding: utf-8 -*-
require 'test/unit'
require 'test/test_helper'

class TestFile < Test::Unit::TestCase
  include TestHelper

  def setup
    $CLASSPATH << File.expand_path( '../test_uri_classloader.jar', __FILE__ )
  end

  def test_dir_glob_on_uri_classloader_path
    assert_equal ['uri:classloader://Rakefile'], Dir[ 'uri:classloader://*' ]
  end

  def test_dir_glob_on_uri_classloader_path_with_dot
    assert_equal ['uri:classloader://./Rakefile'], Dir[ 'uri:classloader://./*' ]
  end

  def test_dir_glob_on_uri_classloader_path_with_dot_dot
    assert_equal ['uri:classloader://lib/../Rakefile'], Dir[ 'uri:classloader://lib/../*' ]
    assert_equal ['uri:classloader://lib/./../Rakefile'], Dir[ 'uri:classloader://lib/./../*' ]
  end
end
