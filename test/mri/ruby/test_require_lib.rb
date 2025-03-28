# frozen_string_literal: true
require 'test/unit'

class TestRequireLib < Test::Unit::TestCase
  # path adjusted to match JRuby's lib location
  libdir = __dir__ + '/../../../lib/ruby/stdlib'

  # .rb files at lib
  scripts = Dir.glob('*.rb', base: libdir).map {|f| f.chomp('.rb')}

  # .rb files in subdirectories of lib without same name script
  dirs = Dir.glob('*/', base: libdir).map {|d| d.chomp('/')}
  dirs -= scripts
  scripts.concat(Dir.glob(dirs.map {|d| d + '/*.rb'}, base: libdir).map {|f| f.chomp('.rb')})

  # skip some problems
  scripts -= %w[bundler bundled_gems rubygems mkmf]

  # additional problems for JRuby
  scripts -= %w[win32/resolv win32/registry jars/lock_down_pom psych_jars jars/output_jars_pom win32api win32ole rake/ant jars/post_install_hook jar_install_post_install_hook ant jars/gemspec_pom]

  scripts.each do |lib|
    define_method "test_thread_size:#{lib}" do
      assert_separately(['-W0'], "#{<<~"begin;"}\n#{<<~"end;"}")
      begin;
        n = Thread.list.size
        require #{lib.dump}
        assert_equal n, Thread.list.size
      end;
    end
  end
end
