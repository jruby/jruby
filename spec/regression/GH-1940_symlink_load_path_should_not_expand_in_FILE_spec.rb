require 'tmpdir'
require 'fileutils'

describe "A file required from a load path entry with an embedded symlink" do

  before :all do

    @path = Dir.mktmpdir
    @realpath = "#{@path}/real"
    @linkpath = "#{@path}/link"
    @realfilepath = "#{@realpath}/GH1940_test.rb"
    @linkfilepath = "#{@linkpath}/GH1940_test.rb"
    Dir.mkdir @realpath
    File.symlink @realpath, @linkpath
    File.write(@realfilepath, "class GH1940; def self.__file__; __FILE__; end; end")

    $LOAD_PATH << @linkpath
    require "GH1940_test.rb"
  end

  after :all do
    FileUtils.rm_rf @realpath
    $LOAD_PATH.delete @linkpath
    $LOADED_FEATURES.delete @linkfilepath
  end

  it "leaves __FILE__ unexpanded" do
    expect(GH1940.__file__).to eq(@linkfilepath)
  end

  it "leaves LOADED_FEATURES unexpanded" do
    expect($LOADED_FEATURES).to include(@linkfilepath)
  end
  
end
