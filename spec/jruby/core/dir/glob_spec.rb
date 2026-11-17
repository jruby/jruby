require 'rspec'
require 'fileutils'
require 'tmpdir'

describe "Dir.glob" do
  let(:dir_name) { 'test_dir!' }
  let(:file_name) { 'test_file%' }

  before :each do
    FileUtils.mkdir(dir_name)
    FileUtils.touch("#{dir_name}/#{file_name}")
  end

  after :each do
    FileUtils.rm_r(dir_name) if Dir.exist?(dir_name)
  end

  it 'can glob directories with URI special chars in it (GH-2264)' do
    expect(Dir.glob("#{dir_name}/**/*").size).to eq 1
  end

  # We have common option processing code and a logic mistake made
  # us process argv[2] as both a kwarg then try to convert it to an
  # integer.
  it "does not think the third arg should be an integer when it is kwargs" do
    expect { Dir["*", "*", base: "."] }.not_to raise_error
  end

  describe "with a uri:classloader: scheme" do
    before :all do
      # FIXME: unsanitary because we can't clear entries from $CLASSPATH
      $CLASSPATH << __dir__
    end

    it "can find a specific file" do
      test1_files = Dir.glob("uri:classloader:/fixtures/test1.txt")
      expect(test1_files[0]).to eq "uri:classloader:/fixtures/test1.txt"
      expect(File.read(test1_files[0])).to eq "test1"
    end

    it "can find a set of files for a wildcard filename" do
      test_txt_files = Dir.glob("uri:classloader:/fixtures/test*.txt")
      expect(test_txt_files).to eq %w[test1 test2 test3].map { "uri:classloader:/fixtures/#{_1}.txt" }
      test_txt_files.each { expect(File.read(_1)).to eq File.basename(_1).split(".").first }

      test_txt_files = Dir.glob("uri:classloader:/fixtures/test?.txt")
      expect(test_txt_files).to eq %w[test1 test2 test3].map { "uri:classloader:/fixtures/#{_1}.txt" }
      test_txt_files.each { expect(File.read(_1)).to eq File.basename(_1).split(".").first }
    end

    it "can find a specific file in a double star path" do
      test5_files = Dir.glob("uri:classloader:/fixtures/**/test4.txt")
      expect(test5_files[0]).to eq "uri:classloader:/fixtures/testdir/test4.txt"
      expect(File.read(test5_files[0])).to eq "test4"
    end
  end

  describe "with a file: scheme" do
    it "can find a specific file" do
      test1_files = Dir.glob("file:#{__dir__}/fixtures/test1.txt")
      expect(test1_files[0]).to eq "file:#{__dir__}/fixtures/test1.txt"
      expect(File.read(test1_files[0])).to eq "test1"
    end

    it "can find a set of files for a wildcard filename" do
      test_txt_files = Dir.glob("file:#{__dir__}/fixtures/test*.txt")
      expect(test_txt_files).to eq %w[test1 test2 test3].map { "file:#{__dir__}/fixtures/#{_1}.txt" }
      test_txt_files.each { expect(File.read(_1)).to eq File.basename(_1).split(".").first }

      test_txt_files = Dir.glob("file:#{__dir__}/fixtures/test?.txt")
      expect(test_txt_files).to eq %w[test1 test2 test3].map { "file:#{__dir__}/fixtures/#{_1}.txt" }
      test_txt_files.each { expect(File.read(_1)).to eq File.basename(_1).split(".").first }
    end

    it "can find a specific file in a double star path" do
      test5_files = Dir.glob("file:#{__dir__}/fixtures/**/test4.txt")
      expect(test5_files[0]).to eq "file:#{__dir__}/fixtures/testdir/test4.txt"
      expect(File.read(test5_files[0])).to eq "test4"
    end
  end

  describe "with a jar:file: scheme" do
    before :all do
      tmpdir = Dir.mktmpdir
      @test_files_jar = "#{tmpdir}/spec_jruby_dir_glob.jar"
      system "jar cf #{@test_files_jar} -C #{__dir__} fixtures"
    end

    after :all do
      FileUtils.rm_f @test_files_jar
    end

    it "can find a specific file" do
      test1_files = Dir.glob("jar:file:#{@test_files_jar}!fixtures/*")
      expect(test1_files[0]).to eq "jar:file:#{@test_files_jar}!fixtures/test1.txt"
      expect(File.read(test1_files[0])).to eq "test1"
    end

    it "can find a set of files for a wildcard filename" do
      test_txt_files = Dir.glob("jar:file:#{@test_files_jar}!fixtures/test*.txt")
      expect(test_txt_files).to eq %w[test1 test2 test3].map { "jar:file:#{@test_files_jar}!fixtures/#{_1}.txt" }
      test_txt_files.each { expect(File.read(_1)).to eq File.basename(_1).split(".").first }

      test_txt_files = Dir.glob("jar:file:#{@test_files_jar}!fixtures/test?.txt")
      expect(test_txt_files).to eq %w[test1 test2 test3].map { "jar:file:#{@test_files_jar}!fixtures/#{_1}.txt" }
      test_txt_files.each { expect(File.read(_1)).to eq File.basename(_1).split(".").first }
    end

    it "can find a specific file in a double star path" do
      test5_files = Dir.glob("jar:file:#{@test_files_jar}!fixtures/**/test4.txt")
      expect(test5_files[0]).to eq "jar:file:#{@test_files_jar}!fixtures/testdir/test4.txt"
      expect(File.read(test5_files[0])).to eq "test4"
    end

    it "can find a specific file in a \".\" relative path" do
      test1_files = Dir.glob("jar:file:#{@test_files_jar}!./fixtures/test1.txt")
      expect(test1_files[0]).to eq "jar:file:#{@test_files_jar}!./fixtures/test1.txt"
      expect(File.read(test1_files[0])).to eq "test1"
    end

    it "treats a bare \".\" path as its own entry" do
      test1_files = Dir.glob("jar:file:#{@test_files_jar}!.")
      expect(test1_files[0]).to eq "jar:file:#{@test_files_jar}!."
    end
  end
end


