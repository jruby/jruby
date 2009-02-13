require 'test/unit'
require 'tempfile'
require 'socket'
require 'rubicon_testcase'

class TestFileTest < RubiconTestCase

  def setup
    @empty = Tempfile.new 'empty'

    @full  = Tempfile.new 'full'
    @full << 'full'
    @full.flush

    nonexistent = Tempfile.new 'nonexistent'
    @nonexistent_path = nonexistent.path
    nonexistent.unlink

    File.symlink @empty.path, @empty.path + '.symlink'
    @symlink = File.open @empty.path + '.symlink'

    socket_file = Tempfile.new 'socket'
    socket_file.close true
    @socket = UNIXServer.new socket_file.path

    @pipe = Tempfile.new 'pipe'
    @pipe.close true
    system "mkfifo #{@pipe.path}"

    @real_tmp = Dir.tmpdir
    Dir.chdir '/' do
      @real_tmp = File.symlink?(@real_tmp) ?
                  File.expand_path(File.readlink(@real_tmp)) :
                  @real_tmp
    end
  end

  def teardown
    File.unlink @symlink.path
    @symlink.close

    File.unlink @socket.path
    @socket.close
  end

  def required_filetypes
    return [
      [:blockdev,    '/dev/disk0'],
      [:chardev,     '/dev/rdisk0'],
      [:directory,   '/bin'],
      [:empty,       @empty.path],
      [:full,        @full.path],
      [:nonexistent, @nonexistent_path],
      [:pipe,        @pipe.path],
      [:setgid,      '/usr/bin/write'],
      [:setuid,      '/sbin/ping'],
      [:socket,      @socket.path],
      [:sticky_dir,  @real_tmp],
    ]
  end

  def util_test(method, filetypes)
    required_filetypes.each do |filetype, path|
      unless filetypes.include? filetype then
        raise "Missing result for #{filetype.inspect}"
      end
      # TODO make this a def
      assert_equal filetypes[filetype], FileTest.send(method, path),
                   "Mismatch for #{filetype.inspect} calling #{method.inspect}"
    end
  end

  def test_class_blockdev_eh
    util_test :blockdev?, :blockdev    => true,
                          :chardev     => false,
                          :directory   => false,
                          :full        => false,
                          :empty       => false,
                          :nonexistent => false,
                          :pipe        => false,
                          :setgid      => false,
                          :setuid      => false,
                          :socket      => false,
                          :sticky_dir  => false,
                          :symlink     => true
  end

  def test_class_chardev_eh
    util_test :chardev?, :blockdev    => false,
                         :chardev     => true,
                         :directory   => false,
                         :empty       => false,
                         :full        => false,
                         :nonexistent => false,
                         :pipe        => false,
                         :setgid      => false,
                         :setuid      => false,
                         :socket      => false,
                         :sticky_dir  => false,
                         :symlink     => true
  end

  def test_class_directory_eh
    util_test :directory?, :blockdev    => false,
                           :chardev     => false,
                           :directory   => true,
                           :empty       => false,
                           :full        => false,
                           :nonexistent => false,
                           :pipe        => false,
                           :setgid      => false,
                           :setuid      => false,
                           :socket      => false,
                           :sticky_dir  => true,
                           :symlink     => true
  end

  def test_class_executable_eh
    # HACK this test sucks, doesn't test uid vs euid, ugo
    assert_equal false, FileTest.executable?(@empty.path)
    @empty.chmod 0100
    assert_equal true,  FileTest.executable?(@empty.path)
  end

  def test_class_executable_real_eh
    # HACK this test sucks, doesn't test uid vs euid, ugo
    assert_equal false, FileTest.executable_real?(@empty.path)
    @empty.chmod 0100
    assert_equal true,  FileTest.executable_real?(@empty.path)
  end

  def test_class_exist_eh
    util_test :exist?, :blockdev    => true,
                       :chardev     => true,
                       :directory   => true,
                       :empty       => true,
                       :full        => true,
                       :nonexistent => false,
                       :pipe        => true,
                       :setgid      => true,
                       :setuid      => true,
                       :socket      => true,
                       :sticky_dir  => true,
                       :symlink     => true
  end

  def test_class_exists_eh
    util_test :exists?, :blockdev    => true,
                        :chardev     => true,
                        :directory   => true,
                        :empty       => true,
                        :full        => true,
                        :nonexistent => false,
                        :pipe        => true,
                        :setgid      => true,
                        :setuid      => true,
                        :socket      => true,
                        :sticky_dir  => true,
                        :symlink     => true
  end

  def test_class_file_eh
    util_test :file?, :blockdev    => false,
                      :chardev     => false,
                      :directory   => false,
                      :empty       => true,
                      :full        => true,
                      :nonexistent => false,
                      :pipe        => false,
                      :setgid      => true,
                      :setuid      => true,
                      :socket      => false,
                      :sticky_dir  => false,
                      :symlink     => true
  end

  def test_class_grpowned_eh
    # HACK this test sucks, doesn't test euid
    assert_equal true,  FileTest.grpowned?(ENV['HOME'])
    assert_equal false, FileTest.grpowned?('/')
  end

  def test_class_identical_eh
    # TODO: raise NotImplementedError, 'Need to write test_class_identical_eh'
  end

  def test_class_owned_eh
    # HACK this test sucks, doesn't test euid
    assert_equal true,  FileTest.owned?(ENV['HOME'])
    assert_equal false, FileTest.owned?('/')
  end

  def test_class_pipe_eh
    util_test :pipe?, :blockdev    => false,
                      :chardev     => false,
                      :directory   => false,
                      :empty       => false,
                      :full        => false,
                      :nonexistent => false,
                      :pipe        => true,
                      :setgid      => false,
                      :setuid      => false,
                      :socket      => false,
                      :sticky_dir  => false,
                      :symlink     => true
  end

  def test_class_readable_eh
    # HACK this test sucks, doesn't test uid vs euid, ugo
    assert_equal true,  FileTest.readable?(@empty.path)
    @empty.chmod 0000
    assert_equal false, FileTest.readable?(@empty.path)
  end

  def test_class_readable_real_eh
    # HACK this test sucks, doesn't test uid vs euid, ugo
    assert_equal true,  FileTest.readable_real?(@empty.path)
    @empty.chmod 0000
    assert_equal false, FileTest.readable_real?(@empty.path)
  end

  def test_class_setgid_eh
    util_test :setgid?, :blockdev    => false,
                        :chardev     => false,
                        :directory   => false,
                        :empty       => false,
                        :full        => false,
                        :nonexistent => false,
                        :pipe        => false,
                        :setgid      => true,
                        :setuid      => false,
                        :socket      => false,
                        :sticky_dir  => false,
                        :symlink     => true
  end

  def test_class_setuid_eh
    util_test :setuid?, :blockdev    => false,
                        :chardev     => false,
                        :directory   => false,
                        :empty       => false,
                        :full        => false,
                        :nonexistent => false,
                        :pipe        => false,
                        :setgid      => false,
                        :setuid      => true,
                        :socket      => false,
                        :sticky_dir  => false,
                        :symlink     => true
  end

  def util_size(f)
    IO.read(f).size
  end

  def test_class_size # TODO directories, nonexistent
    filetypes = Hash[*required_filetypes.flatten]
    assert_equal 0,     FileTest.size(filetypes[:blockdev])
    assert_equal 0,     FileTest.size(filetypes[:chardev])
    # HACK need proper test dir
    #assert_equal 1360, FileTest.size(filetypes[:directory])
    assert_operator 0, :<, FileTest.size(filetypes[:directory])
    assert_equal 0,     FileTest.size(filetypes[:empty])
    assert_equal 4,     FileTest.size(filetypes[:full])
    assert_raises Errno::ENOENT do
      FileTest.size filetypes[:nonexistent]
    end
    assert_equal 0,     FileTest.size(filetypes[:pipe])
    assert_equal util_size(filetypes[:setgid]), FileTest.size(filetypes[:setgid])
    assert_equal util_size(filetypes[:setuid]), FileTest.size(filetypes[:setuid])
    assert_equal 0,     FileTest.size(filetypes[:socket])
    # HACK need proper sticky test dir
    #assert_equal 63988, FileTest.size(filetypes[:sticky_dir])
    assert_operator 0, :<, FileTest.size(filetypes[:sticky_dir])
  end

  def test_class_size_eh # TODO directories
    filetypes = Hash[*required_filetypes.flatten]
    assert_equal nil,   FileTest.size?(filetypes[:blockdev])
    assert_equal nil,   FileTest.size?(filetypes[:chardev])
    # HACK need proper test dir
    #assert_equal 1360,  FileTest.size?(filetypes[:directory])
    assert_operator 0, :<, FileTest.size?(filetypes[:directory])
    assert_equal nil,   FileTest.size?(filetypes[:empty])
    assert_equal 4,     FileTest.size?(filetypes[:full])
    assert_equal nil,   FileTest.size?(filetypes[:nonexistent])
    assert_equal nil,   FileTest.size?(filetypes[:pipe])
    assert_equal util_size(filetypes[:setgid]), FileTest.size?(filetypes[:setgid])
    assert_equal util_size(filetypes[:setuid]), FileTest.size?(filetypes[:setuid])
    assert_equal nil,   FileTest.size?(filetypes[:socket])
    # HACK need proper sticky test dir
    #assert_equal 63988, FileTest.size?(filetypes[:sticky_dir])
    assert_operator 0, :<, FileTest.size?(filetypes[:sticky_dir])
  end

  def test_class_socket_eh
    util_test :socket?, :blockdev    => false,
                        :chardev     => false,
                        :directory   => false,
                        :empty       => false,
                        :full        => false,
                        :nonexistent => false,
                        :pipe        => false,
                        :setgid      => false,
                        :setuid      => false,
                        :socket      => true,
                        :sticky_dir  => false,
                        :symlink     => true
  end

=begin This doesn't appear to pass for MRI or JRuby, at least on OS X
  def test_class_sticky_eh # TODO find a sticky file
    util_test :sticky?, :blockdev    => false,
                        :chardev     => false,
                        :directory   => false,
                        :empty       => false,
                        :full        => false,
                        :nonexistent => false,
                        :pipe        => false,
                        :setgid      => false,
                        :setuid      => false,
                        :socket      => false,
                        :sticky_dir  => true,
                        :symlink     => true
  end
=end

  def test_class_symlink_eh
    util_test :symlink?, :blockdev    => false,
                         :chardev     => false,
                         :directory   => false,
                         :empty       => false,
                         :full        => false,
                         :nonexistent => false,
                         :pipe        => false,
                         :setgid      => false,
                         :setuid      => false,
                         :socket      => false,
                         :sticky_dir  => false,
                         :symlink     => true
  end

  def test_class_writable_eh
    # HACK this test sucks, doesn't test uid vs euid, ugo
    assert_equal true,  FileTest.writable?(@empty.path)
    @empty.chmod 0000
    assert_equal false, FileTest.writable?(@empty.path)
  end

  def test_class_writable_real_eh
    # HACK this test sucks, doesn't test uid vs euid, ugo
    assert_equal true,  FileTest.writable_real?(@empty.path)
    @empty.chmod 0000
    assert_equal false, FileTest.writable_real?(@empty.path)
  end

  def test_class_zero_eh
    util_test :zero?, :blockdev    => true,
                      :chardev     => true,
                      :directory   => false,
                      :empty       => true,
                      :full        => false,
                      :nonexistent => false,
                      :pipe        => true,
                      :setgid      => false,
                      :setuid      => false,
                      :socket      => true,
                      :sticky_dir  => false,
                      :symlink     => true
  end

end

require 'test/unit' if $0 == __FILE__
