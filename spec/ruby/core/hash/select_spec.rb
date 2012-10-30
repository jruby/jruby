require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/iteration', __FILE__)

describe "Hash#select" do
  before(:each) do
    @hsh = new_hash(1 => 2, 3 => 4, 5 => 6)
    @empty = new_hash
  end

  it "yields two arguments: key and value" do
    all_args = []
    new_hash(1 => 2, 3 => 4).select { |*args| all_args << args }
    all_args.sort.should == [[1, 2], [3, 4]]
  end

  ruby_version_is ""..."1.9" do
    it "returns an Array of entries for which block is true" do
      a_pairs = new_hash('a' => 9, 'c' => 4, 'b' => 5, 'd' => 2).select { |k,v| v % 2 == 0 }
      a_pairs.should be_an_instance_of(Array)
      a_pairs.sort.should == [['c', 4], ['d', 2]]
    end
  end

  ruby_version_is "1.9" do
    it "returns a Hash of entries for which block is true" do
      a_pairs = new_hash('a' => 9, 'c' => 4, 'b' => 5, 'd' => 2).select { |k,v| v % 2 == 0 }
      a_pairs.should be_an_instance_of(Hash)
      a_pairs.sort.should == [['c', 4], ['d', 2]]
    end
  end

  it "processes entries with the same order as reject" do
    h = new_hash(:a => 9, :c => 4, :b => 5, :d => 2)

    select_pairs = []
    reject_pairs = []
    h.dup.select { |*pair| select_pairs << pair }
    h.reject { |*pair| reject_pairs << pair }

    select_pairs.should == reject_pairs
  end

  ruby_version_is "" ... "1.8.7" do
    it "raises a LocalJumpError when called on a non-empty hash without a block" do
      lambda { @hsh.select }.should raise_error(LocalJumpError)
    end

    it "does not raise a LocalJumpError when called on an empty hash without a block" do
      @empty.select.should == []
    end
  end

  ruby_version_is "1.8.7" do
    it "returns an Enumerator when called on a non-empty hash without a block" do
      @hsh.select.should be_an_instance_of(enumerator_class)
    end

    it "returns an Enumerator when called on an empty hash without a block" do
      @empty.select.should be_an_instance_of(enumerator_class)
    end
  end

end

ruby_version_is "1.9" do
  describe "Hash#select!" do
    before(:each) do
      @hsh = new_hash(1 => 2, 3 => 4, 5 => 6)
      @empty = new_hash
    end

    it "is equivalent to keep_if if changes are made" do
      new_hash(:a => 2).select! { |k,v| v <= 1 }.should ==
        new_hash(:a => 2).keep_if { |k, v| v <= 1 }

      h = new_hash(1 => 2, 3 => 4)
      all_args_select = []
      all_args_keep_if = []
      h.dup.select! { |*args| all_args_select << args }
      h.dup.keep_if { |*args| all_args_keep_if << args }
      all_args_select.should == all_args_keep_if
    end

    it "returns nil if no changes were made" do
      new_hash(:a => 1).select! { |k,v| v <= 1 }.should == nil
    end

    it "raises a RuntimeError if called on a frozen instance that is modified" do
      lambda { HashSpecs.empty_frozen_hash.select! { false } }.should raise_error(RuntimeError)
    end

    it "raises a RuntimeError if called on a frozen instance that would not be modified" do
      lambda { HashSpecs.frozen_hash.select! { true } }.should raise_error(RuntimeError)
    end
  end
end
