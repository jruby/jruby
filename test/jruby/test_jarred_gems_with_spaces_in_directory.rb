require 'test/unit'
require 'test/jruby/test_helper'

class TestJarredGemsWithSpacesInDirectory < Test::Unit::TestCase
  include TestHelper

  def test_list_gem_from_jar_with_spaces_in_directory
    out = jruby(%q{-r"test/jruby/dir with spaces/testgem.jar" -S jgem list})
    assert(out =~ /testgem/)
  end
end