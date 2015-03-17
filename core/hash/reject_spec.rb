require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/iteration', __FILE__)

describe "Hash#reject" do
  it "returns a new hash removing keys for which the block yields true" do
    h = new_hash(1=>false, 2=>true, 3=>false, 4=>true)
    h.reject { |k,v| v }.keys.sort.should == [1,3]
  end

  it "is equivalent to hsh.dup.delete_if" do
    h = new_hash(:a => 'a', :b => 'b', :c => 'd')
    h.reject { |k,v| k == 'd' }.should == (h.dup.delete_if { |k, v| k == 'd' })

    all_args_reject = []
    all_args_delete_if = []
    h = new_hash(1 => 2, 3 => 4)
    h.reject { |*args| all_args_reject << args }
    h.delete_if { |*args| all_args_delete_if << args }
    all_args_reject.should == all_args_delete_if

    h = new_hash(1 => 2)
    # dup doesn't copy singleton methods
    def h.to_a() end
    h.reject { false }.to_a.should == [[1, 2]]
  end

  ruby_bug "extra states should not be copied", "2.1" do
    it "returns Hash instance for subclasses" do
      HashSpecs::MyHash[1 => 2, 3 => 4].reject { false }.should be_kind_of(Hash)
      HashSpecs::MyHash[1 => 2, 3 => 4].reject { true }.should be_kind_of(Hash)
    end

    it "taints the resulting hash" do
      h = new_hash(:a => 1).taint
      h.reject {false}.tainted?.should == false
    end
  end

  it "processes entries with the same order as reject!" do
    h = new_hash(:a => 1, :b => 2, :c => 3, :d => 4)

    reject_pairs = []
    reject_bang_pairs = []
    h.dup.reject { |*pair| reject_pairs << pair }
    h.reject! { |*pair| reject_bang_pairs << pair }

    reject_pairs.should == reject_bang_pairs
  end

  it_behaves_like(:hash_iteration_no_block, :reject)
end

describe "Hash#reject!" do
  it "removes keys from self for which the block yields true" do
    hsh = new_hash
    (1 .. 10).each { |k| hsh[k] = (k % 2 == 0) }
    hsh.reject! { |k,v| v }
    hsh.keys.sort.should == [1,3,5,7,9]
  end

  it "is equivalent to delete_if if changes are made" do
    hsh = new_hash(:a => 1)
    hsh.reject! { |k,v| v < 2 }.should == hsh.dup.delete_if { |k, v| v < 2 }
  end

  it "returns nil if no changes were made" do
    new_hash(:a => 1).reject! { |k,v| v > 1 }.should == nil
  end

  it "processes entries with the same order as delete_if" do
    h = new_hash(:a => 1, :b => 2, :c => 3, :d => 4)

    reject_bang_pairs = []
    delete_if_pairs = []
    h.dup.reject! { |*pair| reject_bang_pairs << pair }
    h.dup.delete_if { |*pair| delete_if_pairs << pair }

    reject_bang_pairs.should == delete_if_pairs
  end

  it "raises a RuntimeError if called on a frozen instance that is modified" do
    lambda { HashSpecs.empty_frozen_hash.reject! { true } }.should raise_error(RuntimeError)
  end

  it "raises a RuntimeError if called on a frozen instance that would not be modified" do
    lambda { HashSpecs.frozen_hash.reject! { false } }.should raise_error(RuntimeError)
  end

  it_behaves_like(:hash_iteration_no_block, :reject!)
end
