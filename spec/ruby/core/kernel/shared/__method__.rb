def f
  send(@method)
end
alias g f

describe :kernel___method__, :shared => true do

  it "returns the current method, even when aliased" do
    f.should == :f
  end

  ruby_version_is ""..."2.0" do
    it "returns the original name when aliased method" do
      g.should == :f
    end
  end

  ruby_version_is "2.0" do
    it "returns the aliased name when aliased method" do
      g.should == :g
    end
  end

  it "returns the caller from blocks too" do
    def h
      (1..2).map { send(@method) }
    end
    h.should == [:h, :h]
  end

  it "returns the caller from define_method too" do
    klass = Class.new {define_method(:f) {__method__}}
    klass.new.f.should == :f
  end

  it "returns the caller from block inside define_method too" do
    klass = Class.new {define_method(:f) { 1.times{break __method__}}}
    klass.new.f.should == :f
  end

  it "returns the caller from a define_method called from the same class" do
    class C
      define_method(:f) { 1.times{break __method__}}
      def g; f end
    end
    C.new.g.should == :f
  end

  it "returns method name even from eval" do
    def h
      eval @method.to_s
    end
    h.should == :h
  end

  it "returns nil when not called from a method" do
    send(@method).should == nil
  end

  it "returns the caller when sent as a string" do
    def h
      send(@method.to_s)
    end
    h.should == :h
  end
end
