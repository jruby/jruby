require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes', __FILE__)
require File.expand_path('../shared/accessor', __FILE__)

describe "Struct#members" do
  ruby_version_is ""..."1.9" do
    it "returns an array of attribute names" do
      StructClasses::Car.new.members.should == %w(make model year)
      StructClasses::Car.new('Cadillac').members.should == %w(make model year)
      StructClasses::Ruby.members.should == %w(version platform)
    end
  end

  ruby_version_is "1.9" do
    it "returns an array of attribute names" do
      StructClasses::Car.new.members.should == [:make, :model, :year]
      StructClasses::Car.new('Cadillac').members.should == [:make, :model, :year]
      StructClasses::Ruby.members.should == [:version, :platform]
    end
  end

  it_behaves_like :struct_accessor, :length
end
