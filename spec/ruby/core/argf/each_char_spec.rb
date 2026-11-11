require_relative '../../spec_helper'
require_relative 'shared/each_char'

describe "ARGF.each_char" do
  it_behaves_like :argf_each_char, :each_char
end

describe "ARGF.each_char when reading $stdin" do
  before :each do
    @r, @w = IO.pipe("UTF-8")
    @w.write "hé"
    @w.close
    @stdin = $stdin
    $stdin = @r
  end

  after :each do
    $stdin = @stdin
    @r.close unless @r.closed?
  end

  it "yields a multibyte character as one String" do
    argf ['-'] do
      @argf.each_char.to_a.should == ["h", "é"]
    end
  end
end
