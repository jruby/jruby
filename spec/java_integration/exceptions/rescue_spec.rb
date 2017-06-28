require File.dirname(__FILE__) + "/../spec_helper"

java_import java.lang.OutOfMemoryError
java_import "java_integration.fixtures.ThrowExceptionInInitializer"
java_import "java_integration.fixtures.ExceptionRunner"

describe "A non-wrapped Java error" do
  it "can be rescued using the Java type" do
    exception = OutOfMemoryError.new
    begin
      raise exception
    rescue OutOfMemoryError => oome
    end

    expect(oome).to eq(exception)
  end

  it "can be rescued using Object" do
    begin
      raise OutOfMemoryError.new
    rescue Object => e
      expect(e).to be_kind_of(OutOfMemoryError)
    end
  end

  it "can be rescued using Exception" do
    exception = OutOfMemoryError.new
    begin
      raise exception
    rescue Exception => e
    end

    expect(e).to eq(exception)
  end

  it "cannot be rescued using StandardError" do
    exception = OutOfMemoryError.new
    expect do
      begin
        raise exception
      rescue => e
      end
    end.to raise_error(exception)
  end

  it "cannot be rescued inline" do
    obj = Object.new
    def obj.go
      raise OutOfMemoryError.new
    end

    expect do
      obj.go rescue 'foo'
    end.to raise_error(OutOfMemoryError)
  end
end

describe "A non-wrapped Java exception" do
  it "can be rescued using the Java type" do
    exception = java.lang.NullPointerException.new
    begin
      raise exception
    rescue java.lang.NullPointerException => npe
    end

    expect(npe).to eq(exception)
  end

  it "can be rescued using Object" do
    begin
      raise java.lang.NullPointerException.new
    rescue Object => e
      expect(e).to be_kind_of(java.lang.NullPointerException)
    end
  end

  it "can be rescued using Exception" do
    begin
      raise java.lang.NullPointerException.new
    rescue Exception => e
      expect(e).to be_kind_of(java.lang.NullPointerException)
    end
  end

  it "can be rescued using StandardError" do
    begin
      raise java.lang.NullPointerException.new
    rescue StandardError => e
      expect(e).to be_kind_of(java.lang.NullPointerException)
    end
  end

  it "can be rescued inline" do
    obj = Object.new
    def obj.go
      raise java.lang.NullPointerException.new
    end

    expect { obj.go rescue 'foo' }.not_to raise_error
  end

  it "can be rescued dynamically using Module" do
    mod = Module.new
    (class << mod; self; end).instance_eval do
      define_method(:===) { |exception| true }
    end
    begin
      raise java.lang.NullPointerException.new
    rescue mod => e
      expect(e).to be_kind_of(java.lang.NullPointerException)
    end
  end

  it "can be rescued dynamically using Class" do
    cls = Class.new
    (class << cls; self; end).instance_eval do
      define_method(:===) { |exception| puts "=== called: #{exception.inspect}"; true }
    end
    begin
      raise java.lang.NullPointerException.new
    rescue cls => e
      expect(e).to be_kind_of(java.lang.NullPointerException)
    end
  end
end

describe "A Ruby-level exception" do
  it "carries its message along to the Java exception" do
    java_ex = JRuby.runtime.new_runtime_error("error message");
    expect(java_ex.message).to eq("(RuntimeError) error message")

    java_ex = JRuby.runtime.new_name_error("error message", "name");
    expect(java_ex.message).to eq("(NameError) error message")
  end
end

describe "A native exception wrapped by another" do
  it "gets the first available message from the causes' chain" do
    begin
      ThrowExceptionInInitializer.new.test
    rescue NativeException => e
      expect(e.message).to match(/lets cause an init exception$/)
    end
  end

  it "can be re-raised" do
    expect {
      begin
        ThrowExceptionInInitializer.new.test
      rescue NativeException => e
        raise e.exception("re-raised")
      end
    }.to raise_error(NativeException)
  end
end

describe "A Ruby subclass of a Java exception" do

  before :all do
    @ex_class = Class.new(java.lang.RuntimeException)
  end

  let(:exception) { @ex_class.new }

  it "is rescuable with all Java superclasses" do
    begin
      raise exception
      fail
    rescue java.lang.Throwable
      expect($!).to eq(exception)
    end

    begin
      raise exception
      fail
    rescue java.lang.Exception
      expect($!).to eq(exception)
    end

    begin
      raise exception
      fail
    rescue java.lang.RuntimeException
      expect($!).to eq(exception)
    end
  end

  it "presents its Ruby nature when rescued" do
    begin
      raise exception
      fail
    rescue java.lang.Throwable => t
      expect(t.class).to eq(@ex_class)
      expect(t).to equal(exception)
    end
  end

  class MyError1 < Java::JavaLang::RuntimeException; end
  class MyError2 < Java::JavaLang::RuntimeException; end

  it 'rescues correct type with multiple sub-classes' do
    expect {
      begin
        raise MyError1.new('my_error_1')
      rescue MyError2 => e
        fail 'rescued MyError2 => ' + e.inspect
      end
    }.to raise_error(MyError1)

    begin
      raise MyError2.new('my_error_2')
    rescue MyError1 => e
      fail 'rescued MyError1: ' + e.inspect
    rescue MyError2 => e
      expect(e.message).to eql 'my_error_2'
    rescue java.lang.RuntimeException => e
      fail 'rescued java.lang.RuntimeException => ' + e.inspect
    end

    begin
      raise MyError2.new('my_error_2')
    rescue MyError1, RuntimeError => e
      fail 'rescued MyError1, RuntimeError => ' + e.inspect
    rescue LoadError, MyError2 => e
      expect(e.message).to eql 'my_error_2'
    end
  end

end

describe "Ruby exception raised through Java and back to Ruby" do

  it "preserves its class and message" do
    begin
      ExceptionRunner.new.do_it_now do
        raise "it comes from ruby"
      end
      fail
    rescue RuntimeError => e
      expect(e.message).to eq("it comes from ruby")
    end
  end

  context "(via a different thread)"  do

    it "preserves its class and message" do
      begin
        ExceptionRunner.new.do_it_threaded do
          raise "it comes from ruby"
        end
        fail
      rescue RuntimeError => e
        expect(e.message).to eq("it comes from ruby")
      end
    end

  end

  SampleTask = Struct.new(:time) do
    include Comparable

    def <=>(that)
      raise ArgumentError.new("unexpected #{self.inspect}") unless self.time
      raise ArgumentError.new("unexpected #{that.inspect}") unless that.time
      self.time <=> that.time
    end
  end

  it 'does not swallow Ruby errors on compareTo' do
    queue = java.util.PriorityQueue.new(10)
    queue.add t2 = SampleTask.new(2)
    queue.add t1 = SampleTask.new(1)
    begin
      queue.add SampleTask.new(nil)
    rescue ArgumentError => e
      expect( e.message ).to start_with 'unexpected #<struct SampleTask'
    else
      fail 'compareTo did not raise'
    end
    expect( queue.first ).to be t1
  end

end
