require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is "2.3" do
  describe "NameError#receiver" do
    it "returns the object that raised the exception" do
      receiver = Object.new
      -> { receiver.instance_eval { doesnt_exist } }.should(
        raise_error(NameError) do |error|
          error.receiver.should equal(receiver)
        end
      )
    end
    
    it "raises an ArgumentError when the receiver is none" do
      -> { NameError.new.receiver }.should raise_error(ArgumentError)
    end
  end
end
