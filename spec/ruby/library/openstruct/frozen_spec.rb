require File.expand_path('../../../spec_helper', __FILE__)
require 'ostruct'

describe "OpenStruct.new when frozen" do
  before :each do
    @os = OpenStruct.new(:name => "John Smith", :age => 70, :pension => 300).freeze
  end
  #
  # method_missing case handled in method_missing_spec.rb
  #
  it "is still readable" do
    @os.age.should eql(70)
    @os.pension.should eql(300)
    @os.name.should == "John Smith"
  end

  ruby_bug "#1219", "1.9.1" do
    ruby_version_is ""..."2.1.0" do
      it "is not writeable" do
        lambda{ @os.age = 42 }.should raise_error( TypeError )
      end

      it "cannot create new fields" do
        lambda{ @os.state = :new }.should raise_error( TypeError )
      end

      it "creates a frozen clone" do
        f = @os.clone
        f.age.should == 70
        lambda{ f.age = 0 }.should raise_error( TypeError )
        lambda{ f.state = :newer }.should raise_error( TypeError )
      end
    end

    ruby_version_is "2.1.0" do
      it "is not writeable" do
        lambda{ @os.age = 42 }.should raise_error( RuntimeError )
      end

      it "cannot create new fields" do
        lambda{ @os.state = :new }.should raise_error( RuntimeError )
      end

      it "creates a frozen clone" do
        f = @os.clone
        f.age.should == 70
        lambda{ f.age = 0 }.should raise_error( RuntimeError )
        lambda{ f.state = :newer }.should raise_error( RuntimeError )
      end
    end
  end # ruby_bug

  it "creates an unfrozen dup" do
    d = @os.dup
    d.age.should == 70
    d.age = 42
    d.age.should == 42
  end
end
