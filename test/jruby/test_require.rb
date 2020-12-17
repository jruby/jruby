require 'test/unit'

class TestRequire < Test::Unit::TestCase

  # This test repeatedly requires the same file across several threads, reverting $" each time.
  # It passes when requires are only firing once, regardless of concurrency.
  # See JRUBY-3078.
  def test_require_fires_once_across_threads
    $foo = 0
    100.times do |i|
      (1..(ENV['THREAD_COUNT'] || 5).to_i).map do
        Thread.new { require 'test/jruby/test_require_once_foo' }
      end.each { |t| t.join }
      raise "Concurrent requires caused double-loading in iteration #{i} with $foo == #{$foo}" if $foo != 1
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

  def test_concurrent_loading # GH-4091
    filename = File.join(File.dirname(__FILE__), 'gh4091-sample.rb')
    assert ! defined?(GH4091)
    File.open(filename, 'w') do |f|
      f.write <<OUT
module GH4091
  class Sample
    sleep 0.1
    1 + 2 + 3 # some boilerplate
    sleep 0.2
  end
end
OUT
    end

    current_dir = File.expand_path(File.dirname(__FILE__))
    if $LOAD_PATH.include?(current_dir)
      current_dir = false
    else
      $LOAD_PATH << current_dir
    end

    $gh4091_error = nil; threads = []
    (ENV['THREAD_COUNT'] || 5).to_i.times do
      threads << Thread.start {
        begin
          require 'gh4091-sample'
          GH4091::Sample.new # failed in GH-4091
        rescue Exception => e
          unless $gh4091_error
            $gh4091_error = e
          end
        end
      }
    end
    threads.each(&:join)

    e = $gh4091_error
    fail "concurrent loading failed: #{e.inspect}" if e

  ensure
    File.unlink(filename) rescue nil
    $gh4091_error = nil
    $LOAD_PATH.delete current_dir if current_dir
    Object.send(:remove_const, GH4091) rescue nil
    $LOADED_FEATURES.delete 'gh4091-sample.rb'
  end

  module ::ASimpleLib; end

  def test_define_class_type_mismatch
    ASimpleLib.const_set :Error, 1

    # MRI: TypeError: Error is not a class
    assert_raise(TypeError) do
      require_relative('a_simple_lib')
    end
  ensure
    ASimpleLib.send :remove_const, :Error
  end

  # module ::Zlib; end
  #
  # def test_define_class_type_mismatch_ext
  #   return if $LOADED_FEATURES.include?('zlib')
  #
  #   Zlib.const_set :Error, 1
  #
  #   # MRI: TypeError: Zlib::Error is not a class (Integer)
  #   assert_raise(TypeError) do
  #     require('zlib')
  #   end
  # ensure
  #   Zlib.send :remove_const, :Error
  # end

  # def test_define_class_type_mismatch_ext2
  #   return if $LOADED_FEATURES.include?('socket')
  #
  #   Object.const_set :BasicSocket, Class.new
  #
  #   assert_raise(TypeError) do # superclass mismatch for class BasicSocket
  #     require('socket')
  #   end
  # ensure
  #   Object.send :remove_const, :BasicSocket
  # end

end
