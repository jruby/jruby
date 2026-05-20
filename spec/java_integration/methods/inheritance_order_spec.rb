require File.dirname(__FILE__) + "/../spec_helper"

java_import "java_integration.fixtures.inheritance.InheritanceBase"
java_import "java_integration.fixtures.inheritance.InheritanceSubclass"

describe "super order" do

  InheritanceBase.class_eval do
    def initialize(*args)
      opts = args.empty? ? nil : args[-1]
      super(opts ? opts[:arg] : "no options")
      trace.add "Ruby base constructor called with #{args.inspect}"
    end
  end

  # InheritanceSubclass.class_eval do
  #   # no constructor
  # end

  let(:subclass_with_ctor) do
    Class.new(InheritanceBase) do
      def initialize(*args)
        super(*args)
        trace.add "Ruby base-subclass constructor called with #{args.inspect}"
      end
    end
  end

  let(:subsubclass_with_ctor) do
    Class.new(InheritanceSubclass) do
      def initialize(*args)
        super(*args)
        trace.add "Ruby sub-subclass constructor called with #{args.inspect}"
      end
    end
  end

  shared_examples 'tests' do |args|
    java_arg = args.empty? ? "no options" : args[-1][:arg]

    it 'calls Ruby constructors first when constructing parent class' do
      obj = InheritanceBase.new(*args)
      expect(obj.trace).to eq(["Java base constructor called with #{java_arg}",
                               "Ruby base constructor called with #{args.inspect}"])
    end

    it 'calls Ruby constructors first when constructing Ruby subclass' do
      subclass = Class.new(InheritanceBase)
      obj = subclass.new(*args)
      expect(obj.trace).to eq(["Java base constructor called with #{java_arg}",
                               "Ruby base constructor called with #{args.inspect}"])
    end

    it 'calls Ruby constructors first when constructing Ruby subclass with constructor' do
      obj = subclass_with_ctor.new(*args)
      expect(obj.trace).to eq(["Java base constructor called with #{java_arg}",
                               "Ruby base constructor called with #{args.inspect}", # MISSING in 9.3 !!!
                               "Ruby base-subclass constructor called with #{args.inspect}"])
    end

    it 'calls Ruby constructors first when constructing Java subclass' do
      obj = InheritanceSubclass.new(*args)
      expect(obj.trace).to eq(["Java base constructor called with #{java_arg}",
                               "Java subclass constructor called with #{java_arg}",
                               "Ruby base constructor called with #{args.inspect}"])
    end

    it 'calls Ruby constructors first when constructing Ruby subclass of Java subclass' do
      subclass = Class.new(InheritanceSubclass)
      obj = subclass.new(*args)
      expect(obj.trace).to eq(["Java base constructor called with #{java_arg}",
                               "Java subclass constructor called with #{java_arg}",
                               "Ruby base constructor called with #{args.inspect}"])
    end

    it 'calls Ruby constructors first when constructing Ruby subclass of Java subclass with constructor' do
      obj = subsubclass_with_ctor.new(*args)
      expect(obj.trace).to eq(["Java base constructor called with #{java_arg}",
                               "Java subclass constructor called with #{java_arg}",
                               "Ruby base constructor called with #{args.inspect}",
                               "Ruby sub-subclass constructor called with #{args.inspect}"])
    end
  end

  context('with options hash') { include_examples 'tests', [{arg: 'foo'}] }

  context('with no args') { include_examples 'tests', [] }

end
