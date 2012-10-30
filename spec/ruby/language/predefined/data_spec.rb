require File.expand_path('../../../spec_helper', __FILE__)

describe "The DATA constant" do
  it "exists when the main script contains __END__" do
    ruby_exe(fixture(__FILE__, "data1.rb")).chomp.should == "true"
  end

  it "does not exist when the main script contains no __END__" do
    ruby_exe("puts Object.const_defined?(:DATA)").chomp.should == 'false'
  end

  it "does not exist when an included file has a __END__" do
    ruby_exe(fixture(__FILE__, "data2.rb")).chomp.should == "false"
  end

  it "does not change when an included files also has a __END__" do
    ruby_exe(fixture(__FILE__, "data3.rb")).chomp.should == "data 3"
  end

  it "is included in an otherwise empty file" do
    ap = fixture(__FILE__, "print_data.rb")
    str = ruby_exe(fixture(__FILE__, "data_only.rb"), :options => "-r#{ap}")
    str.chomp.should == "data only"
  end

  platform_is_not :windows do
    it "succeeds in locking the file DATA came from" do
      path = fixture(__FILE__, "data_flock.rb")

      data = ruby_exe(path)
      data.should == "0"

      begin
        file = File.open(path)
        file.flock(File::LOCK_EX)
        data = ruby_exe(path)
        data.should == "false"
      ensure
        file.flock(File::LOCK_UN) if file
      end
    end
  end
end
