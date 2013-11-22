require 'test/unit'

require 'tmpdir'
require 'fileutils'

class TestDir < Test::Unit::TestCase

  ROOT = File.join(Dir.tmpdir, "__test_dir__#{$$}")

  def setup
    Dir.mkdir(ROOT)
    for i in ?a..?z
      if i % 2 == 0
        FileUtils.touch(File.join(ROOT, i.chr))
      else
        FileUtils.mkdir(File.join(ROOT, i.chr))
      end
    end
  end

  def teardown
    FileUtils.rm_rf ROOT if File.directory?(ROOT)
  end

  def test_seek
    dir = Dir.open(ROOT)
    begin
      cache = []
      loop do
        pos = dir.tell
        break unless name = dir.read
        cache << [pos, name]
      end
      for x in cache.sort_by {|x| x[0] % 3 } # shuffle
        dir.seek(x[0])
        assert_equal(x[1], dir.read)
      end
    ensure
      dir.close
    end
  end

  def test_symlinks_not_resolved
    Dir.mktmpdir do |dirname|
      FileUtils.cd(dirname) do
        FileUtils.mkdir_p('some-dir')
        File.write('some-dir/foo', 'some content')
        File.symlink('some-dir', 'dir-symlink')

        assert_equal [ 'dir-symlink', 'some-dir' ], Dir['*'].sort
        assert_equal [ 'dir-symlink', 'some-dir', 'some-dir/foo' ], Dir['**/*'].sort
      end
    end
  end
end
