require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/common', __FILE__)
require File.expand_path('../shared/glob', __FILE__)

describe "Dir.glob" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_glob, :glob
end

describe "Dir.glob" do
  before :all do
    DirSpecs.create_mock_dirs
  end

  after :all do
    DirSpecs.delete_mock_dirs
  end

  it_behaves_like :dir_glob_recursive, :[]
end

describe "Dir.glob" do
  before :all do
    DirSpecs.create_mock_dirs

    @cwd = Dir.pwd
    Dir.chdir DirSpecs.mock_dir
  end

  after :all do
    Dir.chdir @cwd

    DirSpecs.delete_mock_dirs
  end

  with_feature :encoding do
    describe "with encoding" do
      it "returns Strings in the encoding of the pattern" do
        a = "file_one*".force_encoding Encoding::IBM437
        b = "file_two*".force_encoding Encoding::EUC_JP
        files = Dir.glob([a, b])

        files.first.encoding.should equal(Encoding::IBM437)
        files.last.encoding.should equal(Encoding::EUC_JP)
      end
    end
  end

  it "can take an array of patterns" do
    Dir.glob(["file_o*", "file_t*"]).should ==
               %w!file_one.ext file_two.ext!
  end

  it "matches both dot and non-dotfiles with '*' and option File::FNM_DOTMATCH" do
    Dir.glob('*', File::FNM_DOTMATCH).sort.should == DirSpecs.expected_paths
  end

  it "matches files with any beginning with '*<non-special characters>' and option File::FNM_DOTMATCH" do
    Dir.glob('*file', File::FNM_DOTMATCH).sort.should == %w|.dotfile nondotfile|.sort
  end

  it "matches any files in the current directory with '**' and option File::FNM_DOTMATCH" do
    Dir.glob('**', File::FNM_DOTMATCH).sort.should == DirSpecs.expected_paths
  end

  it "recursively matches any subdirectories except './' or '../' with '**/' from the current directory and option File::FNM_DOTMATCH" do
    expected = %w[
      .dotsubdir/
      brace/
      deeply/
      deeply/nested/
      deeply/nested/directory/
      deeply/nested/directory/structure/
      dir/
      special/
      special/test{1}/
      subdir_one/
      subdir_two/
    ]

    Dir.glob('**/', File::FNM_DOTMATCH).sort.should == expected
  end

  # This is a seperate case to check **/ coming after a constant
  # directory as well.
  it "recursively matches any subdirectories except './' or '../' with '**/' and option File::FNM_DOTMATCH" do
    expected = %w[
      ./
      ./.dotsubdir/
      ./brace/
      ./deeply/
      ./deeply/nested/
      ./deeply/nested/directory/
      ./deeply/nested/directory/structure/
      ./dir/
      ./special/
      ./special/test{1}/
      ./subdir_one/
      ./subdir_two/
    ]

    Dir.glob('./**/', File::FNM_DOTMATCH).sort.should == expected
  end

  it "accepts a block and yields it with each elements" do
    ary = []
    ret = Dir.glob(["file_o*", "file_t*"]) { |t| ary << t }
    ret.should be_nil
    ary.should == %w!file_one.ext file_two.ext!
  end

  it "ignores non-dirs when traversing recursively" do
    touch "spec"
    Dir.glob("spec/**/*.rb").should == []
  end

  platform_is_not(:windows) do
    it "matches the literal character '\\' with option File::FNM_NOESCAPE" do
      Dir.mkdir 'foo?bar'

      begin
        Dir.glob('foo?bar', File::FNM_NOESCAPE).should == %w|foo?bar|
        Dir.glob('foo\?bar', File::FNM_NOESCAPE).should == []
      ensure
        Dir.rmdir 'foo?bar'
      end

      Dir.mkdir 'foo\?bar'

      begin
        Dir.glob('foo\?bar', File::FNM_NOESCAPE).should == %w|foo\\?bar|
      ensure
        Dir.rmdir 'foo\?bar'
      end
    end

    it "returns nil for directories current user has no permission to read" do
      Dir.mkdir('no_permission')
      File.chmod(0, 'no_permission')

      begin
        Dir.glob('no_permission/*').should == []
      ensure
        Dir.rmdir('no_permission')
      end
    end
  end
end
