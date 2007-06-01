require 'test/unit'

class FileInfoTest < Test::Unit::TestCase
  def setup
    setupTestDir

    @file1 = "_test/_touched1"
    @file2 = "_test/_touched2"

    [ @file1, @file2 ].each { |file|
      File.delete file if File.exist?(file)
    }

    touch("-a -t 122512341999 #@file1")
    @aTime1 = Time.local(1999, 12, 25, 12, 34, 00)

    touch("-m -t 010112341997 #@file1")
    @mTime1 = Time.local(1997,  1,  1, 12, 34, 00)

    File.chown(Process.euid, Process.egid, @file1)

    # File two is before file 1 in access time, and
    # after in modification time

    touch("-a -t 010212342000 #@file2")
    @aTime2 = Time.local(2000, 1, 2, 12, 34, 00)

    touch("-m -t 020312341995 #@file2")
    @mTime2 = Time.local(1995,  2,  3, 12, 34, 00)
  end

  def teardown
    [ @file1, @file2 ].each { |file|
      if File.exist?(file)
        File.chmod(0666, file) # needed on Windows
        File.delete file 
      end
    }
    teardownTestDir
  end
end
