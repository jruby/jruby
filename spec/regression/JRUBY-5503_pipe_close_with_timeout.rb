require "rspec"
require 'timeout'

describe "A pipe from IO.pipe" do
  before :each do
    @abort = Thread.abort_on_exception = true
    @io_r, @io_w = IO.pipe
  end
  
  after :each do
    Thread.abort_on_exception = @abort
  end
  
  def start_read
    @read_thread = Thread.start do
      begin
        @io_r.read
      rescue => e
        e
      end
    end
    sleep(0.1) while @read_thread.status == "run"
  end
  
  it "can be closed when not being read from" do
    lambda do
      @io_r.close
    end.should_not raise_error
  end

  it "can be closed by a timeout thread when not being read from" do
    lambda do
      Timeout::timeout(2){ @io_r.close }
    end.should_not raise_error
  end
  
  it "can be closed by a timeout thread when being read from" do
    start_read()
    lambda do
      Timeout::timeout(2){ @io_r.close }
    end.should_not raise_error
  end
end
