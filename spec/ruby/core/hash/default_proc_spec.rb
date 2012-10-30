require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

describe "Hash#default_proc" do
  it "returns the block passed to Hash.new" do
    h = new_hash { |i| 'Paris' }
    p = h.default_proc
    p.call(1).should == 'Paris'
  end

  it "returns nil if no block was passed to proc" do
    new_hash.default_proc.should == nil
  end
end

describe "Hash#default_proc=" do
  ruby_version_is "1.9" do
    it "replaces the block passed to Hash.new" do
      h = new_hash { |i| 'Paris' }
      h.default_proc = Proc.new { 'Montreal' }
      p = h.default_proc
      p.call(1).should == 'Montreal'
    end

    it "uses :to_proc on its argument" do
      h = new_hash { |i| 'Paris' }
      obj = mock('to_proc')
      obj.should_receive(:to_proc).and_return(Proc.new { 'Montreal' })
      (h.default_proc = obj).should equal(obj)
      h[:cool_city].should == 'Montreal'
    end

    it "overrides the static default" do
      h = new_hash(42)
      h.default_proc = Proc.new { 6 }
      h.default.should be_nil
      h.default_proc.call.should == 6
    end

    it "raises an error if passed stuff not convertible to procs" do
      lambda{new_hash.default_proc = 42}.should raise_error(TypeError)
    end

    ruby_version_is "1.9"..."2.0" do
      it "raises an error if passed nil" do
        lambda{new_hash.default_proc = nil}.should raise_error(TypeError)
      end
    end

    ruby_version_is "2.0" do
      it "clears the default proc if passed nil" do
        h = new_hash { |i| 'Paris' }
        h.default_proc = nil
        h.default_proc.should == nil
        h[:city].should == nil
      end
    end

    it "accepts a lambda with an arity of 2" do
      h = new_hash
      lambda do
        h.default_proc = lambda {|a,b| }
      end.should_not raise_error(TypeError)
    end

    it "raises a TypeError if passed a lambda with an arity other than 2" do
      h = new_hash
      lambda do
        h.default_proc = lambda {|a| }
      end.should raise_error(TypeError)
      lambda do
        h.default_proc = lambda {|a,b,c| }
      end.should raise_error(TypeError)
    end
  end
end
