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
end
