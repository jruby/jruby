require File.expand_path('../../../spec_helper', __FILE__)

describe "File#flock" do
  before :each do
    @name = tmp("flock_test")
    touch(@name)

    @file = File.open @name, "w+"
  end

  after :each do
    @file.flock File::LOCK_UN
    @file.close

    rm_r @name
  end

  it "exclusively locks a file" do
    @file.flock(File::LOCK_EX).should == 0
    @file.flock(File::LOCK_UN).should == 0
  end

  it "non-exclusively locks a file" do
    @file.flock(File::LOCK_SH).should == 0
    @file.flock(File::LOCK_UN).should == 0
  end

  it "returns false if trying to lock an exclusively locked file" do
    @file.flock File::LOCK_EX

    File.open(@name, "w") do |f2|
      f2.flock(File::LOCK_EX | File::LOCK_NB).should == false
    end
  end

  it "returns 0 if trying to lock a non-exclusively locked file" do
    @file.flock File::LOCK_SH

    File.open(@name, "r") do |f2|
      f2.flock(File::LOCK_SH | File::LOCK_NB).should == 0
      f2.flock(File::LOCK_UN).should == 0
    end
  end

  platform_is :solaris, :java do
    before :each do
      @read_file = File.open @name, "r"
      @write_file = File.open @name, "w"
    end

    after :each do
      @read_file.flock File::LOCK_UN
      @read_file.close
      @write_file.flock File::LOCK_UN
      @write_file.close
    end

    it "fails with EBADF acquiring exclusive lock on read-only File" do
      lambda do
        @read_file.flock File::LOCK_EX
      end.should raise_error(Errno::EBADF)
    end

    it "fails with EBADF acquiring shared lock on read-only File" do
      lambda do
        @write_file.flock File::LOCK_SH
      end.should raise_error(Errno::EBADF)
    end
  end
end
