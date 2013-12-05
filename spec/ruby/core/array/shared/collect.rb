describe :array_collect, :shared => true do
  it "returns a copy of array with each element replaced by the value returned by block" do
    a = ['a', 'b', 'c', 'd']
    b = a.send(@method) { |i| i + '!' }
    b.should == ["a!", "b!", "c!", "d!"]
    b.object_id.should_not == a.object_id
  end

  it "does not return subclass instance" do
    ArraySpecs::MyArray[1, 2, 3].send(@method) { |x| x + 1 }.should be_an_instance_of(Array)
  end

  it "does not change self" do
    a = ['a', 'b', 'c', 'd']
    b = a.send(@method) { |i| i + '!' }
    a.should == ['a', 'b', 'c', 'd']
  end

  it "returns the evaluated value of block if it broke in the block" do
    a = ['a', 'b', 'c', 'd']
    b = a.send(@method) {|i|
      if i == 'c'
        break 0
      else
        i + '!'
      end
    }
    b.should == 0
  end

  ruby_version_is '' ... '1.9' do
    it "returns a copy of self if no block given" do
      a = [1, 2, 3]

      copy = a.send(@method)
      copy.should == a
      copy.should_not equal(a)
    end
  end
  ruby_version_is '1.9' do
    it "returns an Enumerator when no block given" do
      a = [1, 2, 3]
      a.send(@method).should be_an_instance_of(enumerator_class)
    end
  end

  it "does not copy tainted status" do
    a = [1, 2, 3]
    a.taint
    a.send(@method){|x| x}.tainted?.should be_false
  end

  ruby_version_is '1.9' do
    it "does not copy untrusted status" do
      a = [1, 2, 3]
      a.untrust
      a.send(@method){|x| x}.untrusted?.should be_false
    end
  end
end

describe :array_collect_b, :shared => true do
  it "replaces each element with the value returned by block" do
    a = [7, 9, 3, 5]
    a.send(@method) { |i| i - 1 }.should equal(a)
    a.should == [6, 8, 2, 4]
  end

  it "returns self" do
    a = [1, 2, 3, 4, 5]
    b = a.send(@method) {|i| i+1 }
    a.object_id.should == b.object_id
  end

  it "returns the evaluated value of block but its contents is partially modified, if it broke in the block" do
    a = ['a', 'b', 'c', 'd']
    b = a.send(@method) {|i|
      if i == 'c'
        break 0
      else
        i + '!'
      end
    }
    b.should == 0
    a.should == ['a!', 'b!', 'c', 'd']
  end

  ruby_version_is '' ... '1.8.7' do
    it "raises LocalJumpError if no block given" do
      a = [1, 2, 3]
      lambda { a.send(@method) }.should raise_error(LocalJumpError)
    end
  end

  ruby_version_is '1.8.7' do
    it "returns an Enumerator when no block given, and the enumerator can modify the original array" do
      a = [1, 2, 3]
      enum = a.send(@method)
      enum.should be_an_instance_of(enumerator_class)
      enum.each{|i| "#{i}!" }
      a.should == ["1!", "2!", "3!"]
    end
  end

  it "keeps tainted status" do
    a = [1, 2, 3]
    a.taint
    a.tainted?.should be_true
    a.send(@method){|x| x}
    a.tainted?.should be_true
  end

  ruby_version_is '1.9' do
    it "keeps untrusted status" do
      a = [1, 2, 3]
      a.untrust
      a.send(@method){|x| x}
      a.untrusted?.should be_true
    end
  end

  describe "when frozen" do
    ruby_version_is '' ... '1.9' do
      it "raises a TypeError" do
        lambda { ArraySpecs.frozen_array.send(@method) {} }.should raise_error(TypeError)
      end

      it "raises a TypeError when empty" do
        lambda { ArraySpecs.empty_frozen_array.send(@method) {} }.should raise_error(TypeError)
      end

      ruby_version_is '1.8.7' do
        it "raises a TypeError when calling #each on the returned Enumerator" do
          enumerator = ArraySpecs.frozen_array.send(@method)
          lambda { enumerator.each {|x| x } }.should raise_error(TypeError)
        end

        it "raises a TypeError when calling #each on the returned Enumerator when empty" do
          enumerator = ArraySpecs.empty_frozen_array.send(@method)
          lambda { enumerator.each {|x| x } }.should raise_error(TypeError)
        end
      end
    end

    ruby_version_is '1.9' do
      it "raises a RuntimeError" do
        lambda { ArraySpecs.frozen_array.send(@method) {} }.should raise_error(RuntimeError)
      end

      it "raises a RuntimeError when empty" do
        lambda { ArraySpecs.empty_frozen_array.send(@method) {} }.should raise_error(RuntimeError)
      end

      it "raises a RuntimeError when calling #each on the returned Enumerator" do
        enumerator = ArraySpecs.frozen_array.send(@method)
        lambda { enumerator.each {|x| x } }.should raise_error(RuntimeError)
      end

      it "raises a RuntimeError when calling #each on the returned Enumerator when empty" do
        enumerator = ArraySpecs.empty_frozen_array.send(@method)
        lambda { enumerator.each {|x| x } }.should raise_error(RuntimeError)
      end
    end
  end
end
