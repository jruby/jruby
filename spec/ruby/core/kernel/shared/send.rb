describe :kernel_send, :shared => true do
  it "invokes the named method" do
    class KernelSpecs::Foo
      def bar
        'done'
      end
    end
    KernelSpecs::Foo.new.send(@method, :bar).should == 'done'
  end

  it "invokes a class method if called on a class" do
    class KernelSpecs::Foo
      def self.bar
        'done'
      end
    end
    KernelSpecs::Foo.send(@method, :bar).should == 'done'
  end

  it "raises a NameError if the corresponding method can't be found" do
    class KernelSpecs::Foo
      def bar
        'done'
      end
    end
    lambda { KernelSpecs::Foo.new.send(@method, :syegsywhwua) }.should raise_error(NameError)
  end

  it "raises a NameError if the corresponding singleton method can't be found" do
    class KernelSpecs::Foo
      def self.bar
        'done'
      end
    end
    lambda { KernelSpecs::Foo.send(@method, :baz) }.should raise_error(NameError)
  end

  it "raises an ArgumentError if no arguments are given" do
    lambda { KernelSpecs::Foo.new.send }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if called with more arguments than available parameters" do
    class KernelSpecs::Foo
      def bar; end
    end

    lambda { KernelSpecs::Foo.new.send(:bar, :arg) }.should raise_error(ArgumentError)
  end

  it "raises an ArgumentError if called with fewer arguments than required parameters" do
    class KernelSpecs::Foo
      def foo(arg); end
    end

    lambda { KernelSpecs::Foo.new.send(@method, :foo) }.should raise_error(ArgumentError)
  end

  it "succeeds if passed an arbitrary number of arguments as a splat parameter" do
    class KernelSpecs::Foo
      def baz(*args) args end
    end

    begin
      KernelSpecs::Foo.new.send(@method, :baz).should == []
      KernelSpecs::Foo.new.send(@method, :baz, :quux).should == [:quux]
      KernelSpecs::Foo.new.send(@method, :baz, :quux, :foo).should == [:quux, :foo]
    rescue
      fail
    end
  end

  it "succeeds when passing 1 or more arguments as a required and a splat parameter" do
    class KernelSpecs::Foo
      def foo(first, *rest) [first, *rest] end
    end

    begin
      KernelSpecs::Foo.new.send(@method, :baz, :quux).should == [:quux]
      KernelSpecs::Foo.new.send(@method, :baz, :quux, :foo).should == [:quux, :foo]
    rescue
      fail
    end
  end

  it "succeeds when passing 0 arguments to a method with one parameter with a default" do
    class KernelSpecs::Foo
      def foo(first = true) first end
    end

    begin
      KernelSpecs::Foo.new.send(@method, :foo).should == true
      KernelSpecs::Foo.new.send(@method, :foo, :arg).should == :arg
    rescue
      fail
    end
  end

  not_compliant_on :rubinius do
    # Confirm commit r24306
    it "has an arity of -1" do
      method(:__send__).arity.should == -1
    end
  end

  deviates_on :rubinius do
    it "has an arity of -2" do
      method(:__send__).arity.should == -2
    end
  end
end
