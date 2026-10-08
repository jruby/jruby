require_relative '../../spec_helper'
require_relative 'shared/readlines'

describe "ARGF.readlines" do
  it_behaves_like :argf_readlines, :readlines
end

describe "ARGF.readlines when reading $stdin and $stdin is not an IO" do
  before :each do
    @stdin = $stdin
  end

  after :each do
    $stdin = @stdin
  end

  it "raises NoMethodError when $stdin has no public readlines" do
    $stdin = Object.new
    -> { ARGF.class.new.readlines }.should raise_error(NoMethodError)
  end
end
