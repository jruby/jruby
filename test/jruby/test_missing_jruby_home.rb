require 'test/unit'

class TestMissingJRubyHome < Test::Unit::TestCase

  def setup; require 'java'
    @jruby_home = java.lang.System.get_property('jruby.home')
    java.lang.System.clear_property('jruby.home')
  end

  def teardown
    java.lang.System.set_property('jruby.home', @jruby_home) if @jruby_home
  end

  # JRUBY-3253: Lack of default jruby.home in embedded usage causes NPE
  def test_missing_jruby_home
    runtime = org.jruby.Ruby.new_instance
    assert_equal('[0, 1, 2]', runtime.eval_scriptlet('3.times.to_a.inspect'))
    runtime.eval_scriptlet('begin require "rbconfig"; rescue LoadError; end')
    # NOTE: used to work at some point when JRuby's load service had special handling
    # for loading of libraries, such as rbconfig, which are implemented natively.
    #
    # This was changed in 9.2.6 (due bootsnap) for every stdlib library to have a .rb
    # @see https://github.com/jruby/jruby/issues/6362
  end

end
