require 'test/unit'

class TestRequireOnce < Test::Unit::TestCase
  # This test repeatedly requires the same file across several threads, reverting $" each time.
  # It passes when requires are only firing once, regardless of concurrency.
  # See JRUBY-3078.
  def test_require_fires_once_across_threads
    $foo = 0
    100.times do
      [1,2,3,4,5].map do |x|
        Thread.new {require "test/foo_for_test_require_once"}
      end.each {|t| t.join}
      raise "Concurrent requires caused double-loading" if $foo != 1
      $foo = 0
      $".pop
    end
  end

  # Ensure that features are getting registered with the full name + ext so subsequent requires
  # do not fire.
  # See JRUBY-3234.
  def test_feature_has_full_name
    load File.dirname(__FILE__) + "/test_require_once_jruby_3234/all.rb"
    assert_equal 1, $jruby_3234
  end
end
