require_relative '../../spec_helper'
require_relative 'shared/each_byte'

describe "ARGF.each_byte" do
  it_behaves_like :argf_each_byte, :each_byte
end

describe "ARGF.each_byte when reading $stdin" do
  before :each do
    @r, @w = IO.pipe
    @w.write "hé"
    @w.close
    @stdin = $stdin
    $stdin = @r
  end

  after :each do
    $stdin = @stdin
    @r.close unless @r.closed?
  end

  it "yields each byte as an Integer" do
    argf ['-'] do
      @argf.each_byte.to_a.should == [104, 195, 169]
    end
  end
end
