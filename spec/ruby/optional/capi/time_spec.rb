require File.expand_path('../spec_helper', __FILE__)

load_extension("time")

describe "CApiTimeSpecs" do
  before :each do
    @s = CApiTimeSpecs.new
  end

  describe "rb_time_new" do
    it "creates a Time from the sec and usec" do
      usec = CAPI_SIZEOF_LONG == 8 ? 4611686018427387903 : 1413123123
      @s.rb_time_new(1232141421, usec).should == Time.at(1232141421, usec)
    end
  end

  ruby_version_is "1.9" do
    describe "TIMET2NUM" do
      it "returns an Integer" do
        @s.TIMET2NUM.should be_kind_of(Integer)
      end
    end
  end
end
