platform_is :windows do
  require 'win32ole'

  # TODO review: not sure how we should test this
  describe "WIN32OLE_EVENT.message_loop" do
    it "exists" do
      lambda { WIN32OLE_EVENT.message_loop }.should_not raise_error NoMethodError
    end
  end

end
