require File.expand_path('../../../spec_helper', __FILE__)

describe "ARGF.set_encoding" do
  before :each do
    @file = fixture __FILE__, "file1.txt"
  end

  after :each do
    ARGF.close unless ARGF.closed?
  end

  it "sets the external encoding when passed an encoding instance" do
    argv [@file] do
      ARGF.set_encoding Encoding::UTF_8
      ARGF.gets.encoding.should == Encoding::UTF_8
    end
  end

  it "sets the external encoding when passed an encoding name" do
    argv [@file] do
      ARGF.set_encoding "utf-8"
      ARGF.gets.encoding.should == Encoding::UTF_8
    end
  end

  it "sets the external, internal encoding when passed two encoding instances" do
    argv [@file] do
      ARGF.set_encoding Encoding::UTF_8, Encoding::EUC_JP
      ARGF.gets.encoding.should == Encoding::EUC_JP
    end
  end

  it "sets the external, internal encoding when passed 'ext:int' String" do
    argv [@file] do
      ARGF.set_encoding "utf-8:euc-jp"
      ARGF.gets.encoding.should == Encoding::EUC_JP
    end
  end
end
