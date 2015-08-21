require 'stringio'

describe "Marshal load behavior: JRUBY-5223" do
  before(:each) do
    @obj = [1, 2, 3]
    @src = Marshal.dump(@obj)
  end

  it "should load string" do
    obj = Marshal.load(@src)
    expect(obj).to eq(@obj)
    expect(obj.tainted?).to eq(false)
  end

  it "should propagate taintness" do
    @src.taint
    obj = Marshal.load(@src)
    expect(obj).to eq(@obj)
    expect(obj.tainted?).to eq(true)
  end

  it "should load IO" do
    obj = Marshal.load(StringIO.new(@src))
    expect(obj).to eq(@obj)
    expect(obj.tainted?).to eq(true)
  end

  it "should load string if it responds to :read" do
    def @src.read; end
    obj = Marshal.load(@src)
    expect(obj).to eq(@obj)
  end

  it "should try stringify with to_str" do
    dummy_src = Object.new
    def dummy_src.to_str
      Marshal.dump([1, 2, 3])
    end
    obj = Marshal.load(dummy_src)
    expect(obj).to eq(@obj)
  end

  it "should try to set binmode if it seems IO" do
    dummy_src = StringIO.new(@src)
    def dummy_src.binmode; raise; end
    expect {
      Marshal.load(dummy_src)
    }.to raise_error RuntimeError
  end
end
