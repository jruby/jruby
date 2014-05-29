require File.expand_path('../initialize_exceptions.rb', __FILE__)
require File.expand_path('../../fixtures/classes.rb', __FILE__)

describe :iconv_new, :shared => true do
  it "creates a new encoding converter" do
    obj = Iconv.send(@method, "us-ascii", "us-ascii")
    begin
      obj.should be_kind_of(Iconv)
    ensure
      obj.close
    end
  end

  it "when called from a subclass of Iconv instantiates an object of that class" do
    obj = IconvSpecs::IconvSubclass.send(@method, "us-ascii", "us-ascii")
    begin
      obj.should be_kind_of(IconvSpecs::IconvSubclass)
    ensure
      obj.close
    end
  end

  it "raises a TypeError when encoding names are not Strings or string-compatible" do
    lambda { Iconv.send @method, Object.new, "us-ascii" }.should raise_error(TypeError)
    lambda { Iconv.send @method, "us-ascii", Object.new }.should raise_error(TypeError)
  end

  it "raises an Iconv::InvalidEncoding exception when an encoding cannot be found" do
    lambda {
      Iconv.send @method, "x-nonexistent-encoding", "us-ascii"
    }.should raise_error(Iconv::InvalidEncoding)
  end
end
