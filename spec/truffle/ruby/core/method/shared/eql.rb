require File.expand_path('../../../../spec_helper', __FILE__)
require File.expand_path('../../fixtures/classes', __FILE__)

describe :method_equal, :shared => true do
  before(:each) do
    @m = MethodSpecs::Methods.new
    @m_foo = @m.method(:foo)
    @m2 = MethodSpecs::Methods.new
    @a = MethodSpecs::A.new
  end

  it "returns true if methods are the same" do
    m2 = @m.method(:foo)

    @m_foo.send(@method, @m_foo).should be_true
    @m_foo.send(@method, m2).should be_true
  end

  it "returns true on aliased methods" do
    m_bar = @m.method(:bar)

    m_bar.send(@method, @m_foo).should be_true
  end

  ruby_version_is "1.9" do
    it "returns true if the two core methods are aliases" do
      s = "hello"
      a = s.method(:size)
      b = s.method(:length)
      a.send(@method, b).should be_true
    end
  end

  it "returns false on a method which is neither aliased nor the same method" do
    m2 = @m.method(:zero)

    @m_foo.send(@method, m2).should be_false
  end

  it "returns false for a method which is not bound to the same object" do
    m2_foo = @m2.method(:foo)
    a_baz = @a.method(:baz)

    @m_foo.send(@method, m2_foo).should be_false
    @m_foo.send(@method, a_baz).should be_false
  end

  it "returns false if the two methods are bound to the same object but were defined independently" do
    m2 = @m.method(:same_as_foo)
    @m_foo.send(@method, m2).should be_false
  end

  ruby_version_is "1.9" do
    it "returns true if a method was defined using the other one" do
      MethodSpecs::Methods.send :define_method, :defined_foo, MethodSpecs::Methods.instance_method(:foo)
      m2 = @m.method(:defined_foo)
      @m_foo.send(@method, m2).should be_true
    end

    ruby_version_is ""..."2.0" do
      it "returns true for methods defined using the same block/proc" do
        class MethodSpecs::Methods
          p = Proc.new { :cool }
          define_method :proc1, p
          define_method :proc2, p

          define_method :block1, &p
          define_method :block2, &p
        end
        proc1 = @m.method :proc1
        proc2 = @m.method :proc2
        block1 = @m.method :proc1
        block2 = @m.method :proc2

        proc1.send(@method, proc2).should be_true
        block1.send(@method, block2).should be_true
        proc1.send(@method, block1).should be_true
      end

      it "returns true for methods defined using equivalent blocks" do
        class MethodSpecs::Methods
          p1 = Proc.new { :cool }
          p2 = Proc.new { :cool }
          define_method :proc1, p1
          define_method :proc2, p2

          define_method :block1, &p1
          define_method :block2, &p2
        end
        proc1 = @m.method :proc1
        proc2 = @m.method :proc2
        block1 = @m.method :proc1
        block2 = @m.method :proc2

        proc1.send(@method, proc2).should be_true
        block1.send(@method, block2).should be_true
        proc1.send(@method, block1).should be_true
      end
    end

    describe 'missing methods' do
      it "returns true for the same method missing" do
        miss1 = @m.method(:handled_via_method_missing)
        miss1bis = @m.method(:handled_via_method_missing)
        miss2 = @m.method(:also_handled)

        miss1.send(@method, miss1bis).should be_true
        miss1.send(@method, miss2).should be_false
      end

      it 'calls respond_to_missing? with true to include private methods' do
        @m.should_receive(:respond_to_missing?).with(:some_missing_method, true).and_return(true)
        @m.method(:some_missing_method)
      end
    end
  end

  it "returns false if the two methods are bound to different objects, have the same names, and identical bodies" do
    a = MethodSpecs::Eql.instance_method(:same_body)
    b = MethodSpecs::Eql2.instance_method(:same_body)
    a.send(@method, b).should be_false
  end

  it "returns false if the argument is not a Method object" do
    String.instance_method(:size).send(@method, 7).should be_false
  end

  it "returns false if the argument is an unbound version of self" do
    method(:load).send(@method, method(:load).unbind).should be_false
  end
end
