require 'tempfile'
require 'java' if defined?(JRUBY_VERSION)
require 'test/unit'
require 'fileutils'

class TestTempfilesCleanUp < Test::Unit::TestCase

  def setup
    @tmpdir = "tmp_#{$$}"
    FileUtils.rm_f @tmpdir rescue nil
    Dir.mkdir @tmpdir rescue nil
  end

  def teardown
    FileUtils.rm_f @tmpdir
  end

  def test_cleanup
    10.times { Tempfile.open('blah', @tmpdir) }

    # check that files were created
    assert Dir["#{@tmpdir}/*"].size > 0

    100.times do
      if defined?(JRUBY_VERSION)
        java.lang.System.gc
      else
        GC.start
      end
    end

    # test that the files are gone
    assert_equal 0, Dir["#{@tmpdir}/*"].size, 'Files were not cleaned up'
  end
end
