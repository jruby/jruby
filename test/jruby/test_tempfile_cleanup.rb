require 'tempfile'
require 'java' if defined?(JRUBY_VERSION)
require 'test/unit'
require 'fileutils'

class TestTempfilesCleanUp < Test::Unit::TestCase

  def setup
    @tmpdir = Dir.mktmpdir(File.basename(__FILE__) + $$.to_s)
    FileUtils.rm_f @tmpdir rescue nil
    Dir.mkdir @tmpdir rescue nil
  end

  def teardown
    FileUtils.rm_rf @tmpdir
  end

  def test_cleanup
    10.times { Tempfile.open('blah', @tmpdir) }

    # check that files were created
    assert Dir["#{@tmpdir}/*"].size > 0

    # spin for up to 10 seconds, attempting to get finalization to trigger
    t = Time.now
    loop do
      if defined?(JRuby)
        JRuby.gc
      else
        GC.start
      end
      break if Time.now - t > 20 || Dir["#{@tmpdir}/*"].size == 0
      sleep(0.1)
    end

    # test that the files are gone
    assert_equal 0, Dir["#{@tmpdir}/*"].size, 'Files were not cleaned up'
  end
end
