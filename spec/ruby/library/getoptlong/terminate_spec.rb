require File.expand_path('../../../spec_helper', __FILE__)
require 'getoptlong'

describe "GetoptLong#terminate" do
  before(:each) do
    @opts = GetoptLong.new(
      [ '--size', '-s',             GetoptLong::REQUIRED_ARGUMENT ],
      [ '--verbose', '-v',          GetoptLong::NO_ARGUMENT ],
      [ '--query', '-q',            GetoptLong::NO_ARGUMENT ],
      [ '--check', '--valid', '-c', GetoptLong::NO_ARGUMENT ]
    )
  end

  it "terminates option proccessing" do
    begin
      old_argv = ARGV
      ARGV = [ "--size", "10k", "-v", "-q", "a.txt", "b.txt" ]

      @opts.get.should == [ "--size", "10k" ]
      @opts.terminate
      @opts.get.should == nil
    ensure
      ARGV = old_argv
    end
  end

  it "returns self when option processsing is terminated" do
    @opts.terminate.should == @opts
  end

  it "returns nil when option processing was already terminated" do
    @opts.terminate
    @opts.terminate.should == nil
  end
end
