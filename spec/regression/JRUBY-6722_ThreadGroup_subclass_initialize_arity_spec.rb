require 'rspec'

describe "A ThreadGroup subclass" do
  it "may define an initialize method with different arity" do
    cls = Class.new(ThreadGroup) do
      def initialize(a, b, c)
        super()
        @a, @b, @c = a, b, c
      end
      attr_accessor :a, :b, :c
    end

    obj = cls.new(1,2,3)
    [obj.a, obj.b, obj.c].should == [1,2,3]
  end
end
