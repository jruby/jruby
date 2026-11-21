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

  let(:subclass_with_fixed_ctor) do
    Class.new(InheritanceBase) do
      def initialize(opts)
        super(opts)
        trace.add "Ruby fixed constructor called with #{opts.inspect}"
      end
    end
  end

  let(:subclass_with_reassigned_fixed_ctor) do
    Class.new(InheritanceBase) do
      def initialize(opts)
        opts = {arg: 'bar'}
        super(opts)
        trace.add "Ruby reassigned fixed constructor called with #{opts.inspect}"
      end
    end
  end

  let(:subclass_with_rest_ctor) do
    Class.new(InheritanceBase) do
      def initialize(first, *rest)
        super
        trace.add "Ruby rest constructor called with #{[first, *rest].inspect}"
      end
    end
  end

  let(:subclass_with_reassigned_rest_ctor) do
    Class.new(InheritanceBase) do
      def initialize(first, *rest)
        first = {arg: 'bar'}
        super
        trace.add "Ruby reassigned rest constructor called with #{[first, *rest].inspect}"
      end
    end
  end

  let(:subclass_with_pre_super_work_ctor) do
    Class.new(InheritanceBase) do
      def initialize(opts)
        computed = {arg: opts[:arg].upcase}
        super(computed)
        trace.add "Ruby computed constructor called with #{computed.inspect}"
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
                               "Ruby base constructor called with #{args.inspect}",
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

  it 'calls Ruby constructors first when a Ruby subclass directly forwards fixed args' do
    opts = {arg: 'foo'}
    obj = subclass_with_fixed_ctor.new(opts)

    expect(obj.trace).to eq(["Java base constructor called with foo",
                             "Ruby base constructor called with #{[opts].inspect}",
                             "Ruby fixed constructor called with #{opts.inspect}"])
  end

  it 'does not bypass fixed arg reassignments before super' do
    original_opts = {arg: 'foo'}
    reassigned_opts = {arg: 'bar'}
    obj = subclass_with_reassigned_fixed_ctor.new(original_opts)

    expect(obj.trace).to eq(["Java base constructor called with bar",
                             "Ruby base constructor called with #{[reassigned_opts].inspect}",
                             "Ruby reassigned fixed constructor called with #{reassigned_opts.inspect}"])
  end

  it 'calls Ruby constructors first when a Ruby subclass directly forwards rest args' do
    opts = {arg: 'foo'}
    obj = subclass_with_rest_ctor.new(opts)

    expect(obj.trace).to eq(["Java base constructor called with foo",
                             "Ruby base constructor called with #{[opts].inspect}",
                             "Ruby rest constructor called with #{[opts].inspect}"])
  end

  it 'does not bypass rest arg reassignments before super' do
    original_opts = {arg: 'foo'}
    reassigned_opts = {arg: 'bar'}
    obj = subclass_with_reassigned_rest_ctor.new(original_opts)

    expect(obj.trace).to eq(["Java base constructor called with bar",
                             "Ruby base constructor called with #{[reassigned_opts].inspect}",
                             "Ruby reassigned rest constructor called with #{[reassigned_opts].inspect}"])
  end

  it 'preserves arbitrary pre-super work before Java constructor selection' do
    original_opts = {arg: 'foo'}
    computed_opts = {arg: 'FOO'}
    obj = subclass_with_pre_super_work_ctor.new(original_opts)

    expect(obj.trace).to eq(["Java base constructor called with FOO",
                             "Ruby base constructor called with #{[computed_opts].inspect}",
                             "Ruby computed constructor called with #{computed_opts.inspect}"])
  end

  context('with no args') { include_examples 'tests', [] }

end
