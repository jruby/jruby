require 'tempfile'

describe "JRUBY-6278: Double require bug in the handling of concurrent requires" do
  before :each do
    @backup = defined?(ScratchPad) ? ScratchPad : nil
    ScratchPad ||= []
  end

  after :each do
    ScratchPad = @backup if @backup
  end

  it "serializes requires even after raising an exception from required file" do
    file = Tempfile.open(['sleeper', '.rb'])
    file.puts "sleep 0.5"
    file.puts "first = true if ScratchPad.empty?"
    file.puts "ScratchPad << :required"
    file.puts "raise if first"
    file.close
    
    t1 = Thread.new {
      begin
        require file.path
      rescue
        require file.path
      end
    }
    t2 = Thread.new {
      begin
        require file.path
      rescue
        require file.path
      end
    }
    [t1, t2].each(&:join)
    ScratchPad.should == [:required, :required]
  end
end
