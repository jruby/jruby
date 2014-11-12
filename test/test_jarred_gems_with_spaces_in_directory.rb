require 'test/unit'
require 'test/test_helper'

class TestJarredGemsWithSpacesInDirectory < Test::Unit::TestCase
  include TestHelper

  def test_list_gem_from_jar_with_spaces_in_directory
    out = jruby(%q{-r"test/dir with spaces/testgem.jar" -S jgem list})
    assert(out =~ /testgem/)

    cp = ENV['CLASSPATH']
    begin
      if RUBY.match( /.jar/) # uses jruby-complete.jar
        ENV['CLASSPATH'] = cp.to_s + File::PATH_SEPARATOR + '"test/dir with spaces/testgem.jar"'
        out = jruby(%q{-e "p require 'testgem'"})
      else
        out = jruby(%q{-J-cp "test/dir with spaces/testgem.jar" -e "p require 'testgem'"})
      end
    ensure
      ENV['CLASSPATH'] = cp if cp
    end
    assert(out =~ /true/)
  end
end
