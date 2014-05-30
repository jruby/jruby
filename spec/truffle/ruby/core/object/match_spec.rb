require File.expand_path('../../../spec_helper', __FILE__)

describe Object, '=~' do
  ruby_version_is ""..."1.9" do
    it "returns false matching any object" do
      o = Object.new

      (o =~ /Object/).should == false
      (o =~ 'Object').should == false
      (o =~ Object).should == false
      (o =~ Object.new).should == false
      (o =~ nil).should == false
      (o =~ true).should == false
    end
  end

  ruby_version_is "1.9" do
    it "returns nil matching any object" do
      o = Object.new

      (o =~ /Object/).should   be_nil
      (o =~ 'Object').should   be_nil
      (o =~ Object).should     be_nil
      (o =~ Object.new).should be_nil
      (o =~ nil).should        be_nil
      (o =~ true).should       be_nil
    end
  end
end
