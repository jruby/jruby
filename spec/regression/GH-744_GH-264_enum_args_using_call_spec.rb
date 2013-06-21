require 'rspec'

describe "Enumerable#each_with_index with Enumerable#each implemented with a call rather than a yield" do
  it "passes nil to the block if each passes no args" do
    no_args_each = Class.new do
      include Enumerable
      def each(&block)
        block.call
      end
    end

    no_args_each.new.each_with_index do |args, index|
      args.should == nil
      index.should == 0
    end
  end

  it "passes the arg directly to the block if each passes one arg" do
    one_arg_each = Class.new do
      include Enumerable
      def each(&block)
        block.call("one")
      end
    end

    one_arg_each.new.each_with_index do |args, index|
      args.should == "one"
      index.should == 0
    end
  end

  it "passes an array of arguments to the block if Enumerable#each passes multiple values" do
    many_args_each = Class.new do
      include Enumerable
      def each(&block)
        block.call(0, 1, 2, 3)
      end
    end

    many_args_each.new.each_with_index do |args, index|
      args.should == [0, 1, 2, 3]
      index.should == 0
    end
  end
end

describe "Enumerator#each_with_index for a method implemented with a call rather than a yield" do
  it "passes nil to the block if the method passes no args" do
    no_args_method = Class.new do
      include Enumerable
      def my_method(&block)
        block.call
      end
    end

    no_args_method.new.enum_for(:my_method).each_with_index do |args, index|
      args.should == nil
      index.should == 0
    end

    no_args_method.new.enum_for(:my_method).next.should == nil
  end

  it "passes the arg directly to the block if the method passes one arg" do
    one_arg_each = Class.new do
      include Enumerable
      def my_method(&block)
        block.call("one")
      end
    end

    one_arg_each.new.enum_for(:my_method) do |args, index|
      args.should == "one"
      index.should == 0
    end

    one_arg_each.new.enum_for(:my_method).next.should == "one"
  end

  it "passes an array of arguments to the block if the method passes multiple values" do
    many_args_method = Class.new do
      include Enumerable
      def my_method(&block)
        block.call(0, 1, 2, 3)
      end
    end

    many_args_method.new.enum_for(:my_method).each_with_index do |args, index|
      args.should == [0, 1, 2, 3]
      index.should == 0
    end

    many_args_method.new.enum_for(:my_method).next.should == [0, 1, 2, 3]
  end
end
