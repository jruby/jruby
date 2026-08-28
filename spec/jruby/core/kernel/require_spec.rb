require 'tempfile'

describe "Kernel#require" do
  before :each do
    @backup = defined?(ScratchPad) ? ScratchPad : nil
    ScratchPad ||= []
  end

  after :each do
    ScratchPad = @backup if @backup
  end

  it "serializes requires even after raising an exception from required file (JRUBY-6278)" do
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
    expect(ScratchPad).to eq([:required, :required])
  end
end

describe "Kernel#require" do
  it "should load a file from inside a path with a '#' symbol with the current directory set as the load path (JRUBY-6339)" do
    Dir.chdir(File.expand_path("../fixtures/dir#with##hashes", __FILE__)) do
      begin
        $LOAD_PATH.unshift "."

        require("foo")
        expect($LOADED_FEATURES.pop).to match(/foo\.rb$/)
      ensure
        $LOAD_PATH.shift
      end
    end
  end
end

