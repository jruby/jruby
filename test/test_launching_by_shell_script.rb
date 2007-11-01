require 'test/unit'
require 'rbconfig'
require 'jruby'

class TestLaunchingByShellScript < Test::Unit::TestCase
  RUBY = File.join(Config::CONFIG['bindir'], Config::CONFIG['ruby_install_name'])
  def jruby(*args)
    prev_in_process = JRuby.runtime.instance_config.run_ruby_in_process
    JRuby.runtime.instance_config.run_ruby_in_process = false
    `#{RUBY} #{args.join(' ')}`
  ensure
    JRuby.runtime.instance_config.run_ruby_in_process = prev_in_process
  end

  def test_minus_e
    assert_equal "true", jruby('-e "puts true"').chomp
    assert_equal 0, $?.exitstatus
  end

  def test_launch_script
    jruby "test/fib.rb"
    assert_equal 0, $?.exitstatus
  end
end