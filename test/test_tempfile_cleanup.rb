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

  def windows?
    File.directory?("\\")
  end

  def test_cleanup_jars_from_jruby_class_loader
    return if windows?
    # run only in embedded case
    skip unless ENV['RUBY']
    cmd = ENV['RUBY'].sub(/^java/, 'java -Djruby.home=uri:classloader://META-INF/jruby.home') + ' -ropenssl -e "sleep(1230)"'
    # with such a command it uses bash to start java
    pid = Process.spawn(*cmd) + 1
    # give jruby sometime to start
    sleep 10

    tmpfiles = File.join( ENV_JAVA['java.io.tmpdir'], "jruby-#{pid}", 'jruby*.jar' )
    assert Dir[tmpfiles].size > 0

    #Process.kill(9, pid)
    system("kill -9 #{pid}")
    sleep 1

    JRuby.cleanup_stale_tempfiles

    assert Dir[tmpfiles].size == 0
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
