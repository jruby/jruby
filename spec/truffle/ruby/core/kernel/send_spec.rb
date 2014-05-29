require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/send', __FILE__)

describe "Kernel#send" do
  it "invokes the named public method" do
    class KernelSpecs::Foo
      def bar
        'done'
      end
    end
    KernelSpecs::Foo.new.send(:bar).should == 'done'
  end

  it "invokes the named alias of a public method" do
    class KernelSpecs::Foo
      alias :aka :bar
      def bar
        'done'
      end
    end
    KernelSpecs::Foo.new.send(:aka).should == 'done'
  end

  it "invokes the named protected method" do
    class KernelSpecs::Foo
      protected
      def bar
        'done'
      end
    end
    KernelSpecs::Foo.new.send(:bar).should == 'done'
  end

  it "invokes the named private method" do
    class KernelSpecs::Foo
      private
      def bar
        'done2'
      end
    end
    KernelSpecs::Foo.new.send(:bar).should == 'done2'
  end

  it "invokes the named alias of a private method" do
    class KernelSpecs::Foo
      alias :aka :bar
      private
      def bar
        'done2'
      end
    end
    KernelSpecs::Foo.new.send(:aka).should == 'done2'
  end

  it "invokes the named alias of a protected method" do
    class KernelSpecs::Foo
      alias :aka :bar
      protected
      def bar
        'done2'
      end
    end
    KernelSpecs::Foo.new.send(:aka).should == 'done2'
  end

  it_behaves_like(:kernel_send, :send)
end
