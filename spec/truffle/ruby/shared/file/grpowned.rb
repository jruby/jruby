describe :file_grpowned, :shared => true do
  before :each do
    @file = tmp('i_exist')
    touch(@file) { |f| f.puts "file_content" }
    File.chown(nil, Process.gid, @file) rescue nil
  end

  after :each do
    rm_r @file
  end

  platform_is_not :windows do
    it "returns true if the file exist" do
      @object.send(@method, @file).should be_true
    end

    ruby_version_is "1.9" do
      it "accepts an object that has a #to_path method" do
        @object.send(@method, mock_to_path(@file)).should be_true
      end
    end
  end

  platform_is :windows do
    it "returns false if the file exist" do
      @object.send(@method, @file).should be_false
    end
  end
end
