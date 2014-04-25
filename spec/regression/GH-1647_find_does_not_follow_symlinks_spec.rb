require 'tmpdir'
require 'find'
require 'set'
require 'rspec'

describe "Find.find" do
  it 'finds files in symlinks' do
    Dir.mktmpdir('jruby-file-find-test') do |fn|
      FileUtils.cd(fn) do
        # Create real dir with file.
        FileUtils.mkdir_p('dir')
        File.write('dir/foo.txt', 'Hello world!')

        # Create symlink.
        File.symlink('dir', 'dir-link')

        # Find files in symlinked dir.
        filenames = Set.new
        Find.find('dir-link/') do |fn|
          filenames << fn
        end

        expected_filenames = Set.new(['dir-link/', 'dir-link/foo.txt'])
        filenames.should == expected_filenames
      end
    end
  end
end
