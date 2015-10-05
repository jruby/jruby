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
      expect(args).to eq(nil)
      expect(index).to eq(0)
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
      expect(args).to eq("one")
      expect(index).to eq(0)
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
      expect(args).to eq([0, 1, 2, 3])
      expect(index).to eq(0)
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
      expect(args).to eq(nil)
      expect(index).to eq(0)
    end

    expect(no_args_method.new.enum_for(:my_method).next).to eq(nil)
  end

  it "passes the arg directly to the block if the method passes one arg" do
    one_arg_each = Class.new do
      include Enumerable
      def my_method(&block)
        block.call("one")
      end
    end

    one_arg_each.new.enum_for(:my_method) do |args, index|
      expect(args).to eq("one")
      expect(index).to eq(0)
    end

    expect(one_arg_each.new.enum_for(:my_method).next).to eq("one")
  end

  it "passes an array of arguments to the block if the method passes multiple values" do
    many_args_method = Class.new do
      include Enumerable
      def my_method(&block)
        block.call(0, 1, 2, 3)
      end
    end

    many_args_method.new.enum_for(:my_method).each_with_index do |args, index|
      expect(args).to eq([0, 1, 2, 3])
      expect(index).to eq(0)
    end

    expect(many_args_method.new.enum_for(:my_method).next).to eq([0, 1, 2, 3])
  end
end

describe "Enumerables whose #each method passes multiple values to a block.call (rather than a yield)," do
  before do
    @test_enum = Class.new do
      include Enumerable
      def each(&block)
        block.call 1,2,3
      end
    end.new
  end

  shared_examples "an Enumerable method which takes a block" do |arity_one_behavior|
    it "passes all #each args to its block" do
      @test_enum.send(subject) do |a, b, c|
        expect(a).to eq(1)
        expect(b).to eq(2)
        expect(c).to eq(3)
      end
    end

    it "passes the appropriate args to blocks of arity one" do
      case arity_one_behavior
        when :array
          @test_enum.send(subject) do |obj|
            expect(obj).to eq([1, 2, 3])
          end
        when :first_arg
          @test_enum.send(subject) do |obj|
            expect(obj).to eq(1)
          end
        else
          raise 'Unknown arity_one_behavior'
      end

    end
  end

  shared_examples "an Enumerable method which returns an enum element" do
    it "puts an array of all each args in the returned value" do
      expect(@test_enum.send(subject) { true }).to eq([1, 2, 3])
    end
  end

  shared_examples "an Enumerable method which returns an array" do |block_ret|
    it "puts an array of all each args in the returned array" do
      expect(@test_enum.send(subject) { block_ret.nil? ? true : block_ret}).to eq([[1, 2, 3]])
    end
  end

  describe "Enumerable#to_a" do
    subject { :to_a }
    it_behaves_like "an Enumerable method which returns an array"
  end

  describe "Enumerable#sort" do
    subject { :sort }
    it_behaves_like "an Enumerable method which returns an array"
  end

  describe "Enumerable#sort_by" do
    subject { :sort_by }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an array"
  end

  describe "Enumerable#select" do
    subject { :select }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an array"
  end

  describe "Enumerable#parition" do
    subject { :partition }
    it_behaves_like "an Enumerable method which takes a block", :array
    it "returns all #each args" do
      expect(@test_enum.partition { true }).to eq([[[1, 2, 3]], []])
    end
  end

  describe "Enumerable#reject" do
    subject { :reject }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an array", false
  end

  describe "Enumerable#min" do
    subject { :min }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an enum element"
  end

  describe "Enumerable#max" do
    subject { :max }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an enum element"
  end

  describe "Enumerable#minmax" do
    subject { :minmax }
    it_behaves_like "an Enumerable method which takes a block", :array
    it "returns all #each args" do
      expect(@test_enum.minmax).to eq([[1, 2, 3], [1, 2, 3]])
    end
  end

  describe "Enumerable#min_by" do
    subject { :min_by }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an enum element"
  end

  describe "Enumerable#max_by" do
    subject { :max_by }
    it_behaves_like "an Enumerable method which takes a block", :array
    it_behaves_like "an Enumerable method which returns an enum element"
  end

  describe "Enumerable#minmax_by" do
    subject { :minmax_by }
    it_behaves_like "an Enumerable method which takes a block", :array
    it "returns all #each args" do
      expect(@test_enum.minmax_by {|o| o}).to eq([[1, 2, 3], [1, 2, 3]])
    end
  end

  describe "Enumerable#include?" do
    it "tests against all #each args" do
      expect(@test_enum.include?([1, 2, 3])).to be true
    end
  end

  describe "Enumerable#any?" do
    subject { :any? }
    it_behaves_like "an Enumerable method which takes a block", :first_arg
  end

  describe "Enumerable#none?" do
    subject { :none? }
    it_behaves_like "an Enumerable method which takes a block", :first_arg
  end

  describe "Enumerable#one?" do
    subject { :one? }
    it_behaves_like "an Enumerable method which takes a block", :first_arg
  end

  describe "Enumerable#all?" do
    subject { :all? }
    it_behaves_like "an Enumerable method which takes a block", :first_arg
  end

  describe "Enumerable#inject" do
    it "passes all each args to its block" do
      @test_enum.inject(0) { |memo, obj| expect(obj).to eq([1, 2, 3]) }
    end
  end

  describe "Enumerable#group_by" do
    subject { :group_by }
    it_behaves_like "an Enumerable method which takes a block", :array
    it "returns groups containing all #each arguments in an array" do
      expect(@test_enum.group_by { :x }).to eq({ :x => [[1, 2, 3]]})
    end
  end

  describe "Enumerable#cycle" do
    it "passes all #each args to its block" do
      @test_enum.cycle(1) do |obj|
        expect(obj).to eq([1, 2, 3])
      end
    end
  end

  describe "Enumerable#each_slice" do
    it "passes all #each args to its block" do
      @test_enum.each_slice(1) do |obj|
        expect(obj).to eq([[1, 2, 3]])
      end
    end
  end

  describe "Enumerable#drop_while" do
    subject { :drop_while }
    it_behaves_like "an Enumerable method which takes a block", :array
    it "returns all #each args even if its block does not use them" do
      expect(@test_enum.drop_while do |a|
        false
      end).to eq([[1, 2, 3]])
    end
  end

  describe "Enumerable#take_while" do
    subject { :take_while }
    it_behaves_like "an Enumerable method which takes a block", RUBY_VERSION >= '1.9' ? :first_arg : :array
    it "returns all #each args even if its block does not use them" do
      expect(@test_enum.take_while do |a|
        true
      end).to eq([[1, 2, 3]])
    end
  end

  describe "Enumerable#find_index" do
    subject { :find_index }
    it_behaves_like "an Enumerable method which takes a block", :first_arg
  end

  describe "Enumerable#detect" do
    subject { :detect }
    it_behaves_like "an Enumerable method which takes a block", :array
  end

  describe "Enumerable#grep" do
    it "finds array of all #each args" do
      expect(@test_enum.grep([1, 2, 3])).to eq([[1, 2, 3]])
    end
  end

  describe "Enumerable#zip" do
    it "includes all #each args in the zipped array" do
      expect(@test_enum.zip).to eq([[[1, 2, 3]]])
    end
  end

  describe "Enumerable#each_cons" do
    it "puts all #each args in its block array" do
      @test_enum.each_cons(1) { |obj| expect(obj).to eq([[1, 2, 3]]) }
    end
  end

  describe "Enumerable#each_with_object" do
    it "passes all each args to its block" do
      @test_enum.each_with_object([]) { |obj, memo| expect(obj).to eq([1, 2, 3]) }
    end
  end

  describe "Enumerable#each_entry" do
    subject { :each_entry }
    it_behaves_like "an Enumerable method which takes a block", :array
  end

  describe "Enumerable#slice_before" do
    it "passes all #each args to its block" do
      @test_enum.slice_before do |obj|
	expect(obj).to eq([1, 2, 3])
      end.each{}
    end
  end

  describe "Enumerable#flat_map" do
    subject { :flat_map }
    it_behaves_like "an Enumerable method which takes a block", :first_arg
  end

  describe "Enumerable#chunk" do
    it "passes all #each args to its block" do
      @test_enum.chunk do |a, b, c|
	expect(a).to eq(1)
	expect(b).to eq(2)
	expect(c).to eq(3)
      end.each {}
    end
      
    it "passes all #each args to its block" do
      @test_enum.chunk do |obj|
	expect(obj).to eq([1, 2, 3])
      end.each{}
    end
  end
end
