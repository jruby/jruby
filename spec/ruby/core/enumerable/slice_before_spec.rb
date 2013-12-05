require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)

ruby_version_is '1.9' do
  describe "Enumerable#slice_before" do
    before :each do
      @enum = EnumerableSpecs::Numerous.new(7,6,5,4,3,2,1)
    end

    describe "when given an argument and no block" do
      it "calls === on the argument to determine when to yield" do
        arg = mock "filter"
        arg.should_receive(:===).and_return(false, true, false, false, false, true, false)
        e = @enum.slice_before(arg)
        e.should be_an_instance_of(enumerator_class)
        e.to_a.should == [[7], [6, 5, 4, 3], [2, 1]]
      end

      it "doesn't yield an empty array if the filter matches the first entry or the last entry" do
        arg = mock "filter"
        arg.should_receive(:===).and_return(true).exactly(7)
        e = @enum.slice_before(arg)
        e.to_a.should == [[7], [6], [5], [4], [3], [2], [1]]
      end

      it "uses standard boolean as a test" do
        arg = mock "filter"
        arg.should_receive(:===).and_return(false, :foo, nil, false, false, 42, false)
        e = @enum.slice_before(arg)
        e.to_a.should == [[7], [6, 5, 4, 3], [2, 1]]
      end
    end

    describe "when given a block" do
      describe "and no argument" do
        it "calls the block to determine when to yield" do
          e = @enum.slice_before{|i| i == 6 || i == 2}
          e.should be_an_instance_of(enumerator_class)
          e.to_a.should == [[7], [6, 5, 4, 3], [2, 1]]
        end
      end

      describe "and an argument" do
        it "calls the block with a copy of that argument" do
          arg = [:foo]
          first = nil
          e = @enum.slice_before(arg) do |i, init|
            init.should == arg
            init.should_not equal(arg)
            first = init
            i == 6 || i == 2
          end
          e.should be_an_instance_of(enumerator_class)
          e.to_a.should == [[7], [6, 5, 4, 3], [2, 1]]
          e = @enum.slice_before(arg) do |i, init|
            init.should_not equal(first)
          end
          e.to_a
        end

        quarantine! do # need to double-check with ruby-core. Might be wrong or too specific
          it "duplicates the argument directly without calling dup" do
            arg = EnumerableSpecs::Undupable.new
            e = @enum.slice_before(arg) do |i, init|
              init.initialize_dup_called.should be_true
              false
            end
            e.to_a.should == [[7, 6, 5, 4, 3, 2, 1]]
          end
        end
      end
    end

    it "raises an Argument error when given an incorrect number of arguments" do
      lambda { @enum.slice_before("one", "two") }.should raise_error(ArgumentError)
      lambda { @enum.slice_before }.should raise_error(ArgumentError)
    end
  end
end
