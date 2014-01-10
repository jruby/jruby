require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe "UNIXServer#for_fd" do
  before :each do
    @unix_path = tmp("unix_socket")
    @unix = UNIXServer.new(@unix_path)
  end

  after :each do
    # UG. We can't use the new_fd helper, because we need fds that are
    # associated with sockets. But for_fd has the same issue as IO#new, it
    # creates a fd aliasing issue with closing, causing EBADF errors.
    #
    # Thusly, the rescue for EBADF here. I'd love a better solution, but
    # I'm not aware of one.

    begin
      @unix.close unless @unix.closed?
    rescue Errno::EBADF
      # I hate this API too
    end

    rm_r @unix_path
  end

  it "can calculate the path" do
    b = UNIXServer.for_fd(@unix.fileno)

    b.path.should == @unix_path
    b.close
  end
end
