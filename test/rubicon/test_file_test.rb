require 'stat'
require 'file_info_testcase.rb'
require 'socket'

class TestFileTest < FileInfoTest


  # compares modified times
  def test_CMP # '<=>'
    assert(test(?=, @file1, @file1))
    assert(test(?=, @file2, @file2))
    assert(test(?>, @file1, @file2))
    assert(test(?<, @file2, @file1))
  end

  def test_hlink
    File.link(@file1, "_link")
    begin
      assert(!test(?-, @file1,  @file2))
      assert(test(?-,  @file1,  @file1))
      assert(test(?-,  @file1, "_link"))
      assert(test(?-,  "_link", @file1))
    ensure
      File.unlink("_link")
    end
  end

  def test_skipped 
    skipping "Tests for O R W X "
  end

  def test_test
    fileg = "_test/_fileg"
    File.open(fileg, File::CREAT, 02755) { }
    
    filek = "_test/_filek"
    Dir.mkdir(filek, 01644)
    File.chmod(01644, filek)

    filel = filep = filer = fileu = nil

    Windows.dont do
      filel = "_test/_filel"
      File.symlink(@file1, filel)
      
      filep = "_test/_filep"
      system "mkfifo #{filep}"
      assert_equal(0, $?)

      filer = "_test/_filer"
      File.open(filer, File::CREAT, 0222) { }

      fileu = "_test/_fileu"
      File.open(fileu, File::CREAT, 0644) { }
      File.chmod(04644, fileu)  # set-uid bit
    end

    
    filew = "_test/_filew"
    File.open(filew, File::CREAT, 0444) { }
    
    filez = "_test/_filez"
    File.open(filez, File::CREAT|File::WRONLY, 0644) { |f| f.puts "hi" }
    filez_size = $os <= WindowsNative ? 4 : 3

    filesock = sock = nil
    MsWin32.dont do
      filesock = "_test/_filesock"
      sock = UNIXServer.open(filesock)
      filesock = nil unless sock
    end

    atime = Time.at(RubiconStat::atime(@file1))
    ctime = Time.at(RubiconStat::ctime(@file1))
    mtime = Time.at(RubiconStat::mtime(@file1))

    begin
      tests = [
        [ nil,          ?A,    @file1,              atime ],
        [ :blockdev?,   ?b,    ".",                 false ],
        [ :chardev?,    ?c,    ".",                 false ],
        [ nil,          ?C,    @file1,              ctime ],
        [ :directory?,  ?d,    ".",                 true  ],
        [ :directory?,  ?d,    "/dev/fd0",          false ],
        [ :exist?,      ?e,    filez,               true  ],
        [ :exist?,      ?e,    "wombat",            false ],
        [ :file?,       ?f,    ".",                 false ],
        [ :file?,       ?f,    "/dev/fd0",          false ],
        [ :file?,       ?f,    @file1,              true  ],
        [ :setgid?,     ?g,    @file1,              false ],
        [ :symlink?,    ?l,    ".",                 false ],
        [ :symlink?,    ?l,    @file1,              false ],
        [ nil,          ?M,    @file1,              mtime ],
        [ :owned?,      ?o,    @file1,              true  ],
        [ :pipe?,       ?p,    ".",                 false ],
        [ :readable?,   ?r,    @file1,              true  ],
        [ :size?,       ?s,    filez,               filez_size ],
        [ :size?,       ?s,    @file2,              nil   ],
        [ :socket?,     ?S,    ".",                 false ],
        [ :socket?,     ?S,    @file1,              false ],
        [ :setuid?,     ?u,    @file1,              false ],
        [ :writable?,   ?w,    @file2,              true  ],
        [ :executable?, ?x,    "/dev/fd0",          false ],
        [ :zero?,       ?z,    filez,               false ],
        [ :zero?,       ?z,    @file2,              true  ],
      ]

      Solaris.dont do
        tests << [ :symlink?,    ?l,    "/dev/tty",          false ]
      end

      Windows.dont do
        tests << [ :pipe?,       ?p,    filep,               true  ]
        tests << [ :symlink?,    ?l,    filel,               true  ]
        tests << [ :readable?,   ?r,    filer,               Process.euid == 0 ]
        tests << [ :setuid?,     ?u,    fileu,               true  ]
        tests << [ :owned?,      ?o,    "/etc/passwd",       Process.euid == 0 ]
      end

      MsWin32.dont do 
	tests << [ :socket?,     ?S,    filesock,            true  ]
      end

      Unix.or_variant do
        tests << [ :blockdev?,   ?b,    "/dev/tty",          false ]
        tests << [ :chardev?,    ?c,    "/dev/tty",          true  ]
        tests << [ :directory?,  ?d,    "/dev/tty",          false ]
        tests << [ :file?,       ?f,    "/dev/tty",          false ]
        tests << [ :exist?,      ?e,    "/dev/tty",          true  ]
        tests << [ :sticky?,     ?k,    "/dev/tty",          false ]
        tests << [ :pipe?,       ?p,    "/dev/tty",          false ]
        tests << [ :socket?,     ?S,    "/dev/tty",          false ]
	tests << [ :executable?, ?x,    "/dev/tty",          false ]
        tests << [ :executable?, ?x,    "/bin/echo",         true  ]
# HACK - requires super on osx?        tests << [ :setgid?,     ?g,    fileg,               true  ]
        tests << [ :sticky?,     ?k,    ".",                 false ]
        tests << [ :sticky?,     ?k,    @file1,              false ]
        tests << [ :sticky?,     ?k,    filek,               true  ]
      end

      Linux.only do
        tests << [ :chardev?,    ?c,    "/dev/fd0",          false ]
        tests << [ :blockdev?,   ?b,    "/dev/fd0",          true  ]
        tests << [ :grpowned?,   ?G,    @file1,              true  ]
        tests << [ :grpowned?,   ?G,    "/etc/passwd",       Process.egid == 0 ]
      end

      for meth, t, file, result in tests
        if file
          assert_equal(result, test(t, file), "test(?#{t.chr}, #{file})")
          if meth
            assert_equal(result, FileTest.send(meth, file), 
                         "FileTest.#{meth}(#{file})")
          end
        end
      end

      Windows.dont do
        # Windows has no superuser semantics like UN*X, so we skip these tests.
        # TODO: check if these tests actually do the right thing.
        assert_equal(Process.euid == 0, test(?w, filew), "test(?#{t.chr}, #{filew})")
        assert_equal(Process.euid == 0, FileTest.send(:writable?, filew),
                     "FileTest.writable?(#{filew})")
      end

    ensure
      sock.close if sock
    end
  end
end
