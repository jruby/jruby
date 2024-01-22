require 'test/unit'

class TestJRubyInternals < Test::Unit::TestCase
  def setup
    @dir = File.join(Dir.pwd, "dir with spaces")
    @file = File.join(@dir, "test_file")
    Dir.mkdir(@dir)
    File.open(@file, "w") {|f| f << "hello"}
  end

  def teardown
    File.unlink @file
    Dir.rmdir @dir
  end

  def test_CONFIG; require 'jruby'
    assert JRuby::CONFIG
    assert_equal false, JRuby::CONFIG.rubygems_disabled?
  end

  def test_smart_split_paths
    require 'org/jruby/kernel/path_helper'
    assert_equal %w(foo bar blah),
      JRuby::PathHelper.smart_split_command("foo bar blah")
    assert_equal [@file, *(%w{foo bar blah})],
      JRuby::PathHelper.smart_split_command("#@file foo bar blah")
  end

  def test_split_command_around_quotes
    assert_equal %w(foo bar baz quux),
      JRuby::PathHelper.quote_sensitive_split(%{foo bar baz quux})
    assert_equal ["foo", "bar baz", "quux"],
      JRuby::PathHelper.quote_sensitive_split(%{foo "bar baz" quux})
    assert_equal ["foo", "bar \" baz", "quux"],
      JRuby::PathHelper.quote_sensitive_split(%{foo "bar \\\" baz" quux})
    assert_equal ["foo", "bar baz", "quux"],
      JRuby::PathHelper.quote_sensitive_split(%{"foo" "bar baz" quux})
    assert_equal ["foo", "bar \" baz", "quux"],
      JRuby::PathHelper.quote_sensitive_split(%{"foo" 'bar " baz' quux})
  end

  def test_split_typical_ruby_command_line
    assert_equal ["ruby", "-e", "require 'java'; puts java.lang.System.getProperty('java.class.path')"],
      JRuby::PathHelper.quote_sensitive_split(
        "ruby -e   \"require 'java'; puts java.lang.System.getProperty('java.class.path')\""
      )
  end

  def test_unquoted_executable_with_quoted_args
    cmd = "#@file -Ilib;test \"C:/Projects/space name/jruby-1_0/lib/ruby/gems/1.8/gems/rake-0.7.3/lib/rake/rake_test_loader.rb\" \"test/functional/hello_controller_test.rb\""
    assert_equal [@file, "-Ilib;test",
      "C:/Projects/space name/jruby-1_0/lib/ruby/gems/1.8/gems/rake-0.7.3/lib/rake/rake_test_loader.rb",
      "test/functional/hello_controller_test.rb"],
      JRuby::PathHelper.smart_split_command(cmd)
  end
end
