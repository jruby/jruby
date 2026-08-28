require 'java'

describe "A Java object's java_alias method" do
  before :all do
    class java::util::ArrayList
      java_alias :add_int_object, :add, [Java::int, java.lang.Object]
    end
  end
  
  before :each do
    @list = java.util.ArrayList.new
  end

  it "allows wiring a no-arg method with just name" do
    expect do
      class java::util::ArrayList
        java_alias :blah, :toString
      end
    end.not_to raise_error
  end

  it "allows wiring a no-arg method with name plus empty array" do
    expect do
      class java::util::ArrayList
        java_alias :blah, :toString, []
      end
    end.not_to raise_error
  end
  
  it "allows calling a specific overload" do
    @list.add_int_object 0, 'foo'
    expect(@list.to_s).to eq("[foo]")
  end
  
  it "raises NameError if the method can't be found" do
    expect do
      class java::util::ArrayList
        java_alias :blah, :foobar
      end
    end.to raise_error(NameError)
  end
  
  it "raises ArgumentError if type count doesn't match arg count" do
    expect do
      @list.add_int_object 0, 'foo', 'bar'
    end.to raise_error(ArgumentError)
  end

  context 'constructor' do

    before(:all) do
      Java::java_integration.fixtures::AnArrayList.class_eval do
        java_alias :init, :'<init>', [Java::int] # AnArrayList(int)

        def initialize(num)
          init(num) if num
          @@last_instance = self
        end

        def self.last_instance; @@last_instance end
      end
    end

    before(:each) { klass::CREATED_INSTANCES.clear }

    let(:klass) { Java::java_integration.fixtures::AnArrayList }

    it 'is supported' do
      a_list = klass.new(3)
      expect( a_list ).to be klass::last_instance
      expect( klass::CREATED_INSTANCES.size ).to be 1
      expect( klass::CREATED_INSTANCES[0].size ).to eql(3)

      a_list = klass.allocate
      a_list.init(10) # useful to bypass :initialize
      expect( klass::CREATED_INSTANCES.size ).to be 2
      expect( a_list ).to_not be klass::last_instance
    end

    it 'raises on invalid arguments' do
      expect { klass.new(:foo) }.to raise_error(TypeError)
    end

    it 'raises on unexpected arguments' do
      a_list = klass.new(nil)
      expect { a_list.init(1, 2) }.to raise_error(ArgumentError)
    end

  end

  context 'interface' do

    before(:all) do
      java.lang.Iterable.module_eval do
        java_alias :__do_for_each, :forEach, [java.util.function.Consumer]
      end
    end

    let(:iterable) do
      java.util.concurrent.ConcurrentLinkedQueue.new.tap { |coll| coll.add(111) }
    end


    it "allows calling alias" do
      last_element = nil
      iterable.__do_for_each { |elem| last_element = elem }
      expect(last_element).to eql 111
    end

  end
end
