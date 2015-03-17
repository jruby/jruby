describe :dir_pwd, :shared => true do
  with_feature :encoding do
    before :each do
      @external = Encoding.default_external
      @internal = Encoding.default_internal

      Encoding.default_external = Encoding::IBM437
    end

    after :each do
      Encoding.default_external = @external
      Encoding.default_internal = @internal
    end
  end

  it "returns the current working directory" do
    # On ubuntu gutsy, for example, /bin/pwd does not
    # understand -P. With just `pwd -P`, /bin/pwd is run.

    # The following uses inode rather than file names to account for
    # case insensitive file systems like default OS/X file systems
    platform_is_not :windows do
    File.stat(Dir.send(@method)).ino.should == File.stat(`/bin/sh -c "pwd -P"`.chomp).ino
    end
    platform_is :windows do
      File.stat(Dir.send(@method)).ino.should == File.stat(File.expand_path(`cd`.chomp)).ino
    end
  end

  with_feature :encoding do
    it "returns a String with Encoding.default_external encoding" do
      Dir.send(@method).encoding.should equal(Encoding::IBM437)
    end

    it "does not transcode to Encoding.default_internal" do
      Encoding.default_internal = Encoding::EUC_JP
      Dir.send(@method).encoding.should equal(Encoding::IBM437)
    end
  end
end
