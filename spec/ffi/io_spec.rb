require 'ffi'

if false # disabled for #390
  describe "FFI::IO.for_fd" do
    it "produces an IO wrapping the specified file descriptor" do
      expect do
        FFI::IO.for_fd(2, "r")
      end.to_not raise_error
    end
  end
end