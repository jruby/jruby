require File.expand_path('../../fixtures/classes.rb', __FILE__)
describe :iconv_initialize_exceptions, :shared => true do
  it "raises a TypeError when encoding names are not Strings or string-compatible" do
    lambda { Iconv.send @method, Object.new, "us-ascii", @object }.should raise_error(TypeError)
    lambda { Iconv.send @method, "us-ascii", Object.new, @object }.should raise_error(TypeError)
  end

  it "raises an Iconv::InvalidEncoding exception when an encoding cannot be found" do
    lambda {
      Iconv.send @method, "x-nonexistent-encoding", "us-ascii", @object
    }.should raise_error(Iconv::InvalidEncoding)
  end
end
