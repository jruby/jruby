require 'ffi'

describe "FFI::IO.for_fd" do
  it "produces an IO wrapping the specified file descriptor" do
    lambda do
      FFI::IO.for_fd(2, "r")
    end.should_not raise_error
  end
end
