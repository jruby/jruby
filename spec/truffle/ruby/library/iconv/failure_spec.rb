require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../fixtures/classes.rb', __FILE__)

ruby_version_is ''...'2.0' do 
  describe "Iconv::Failure" do
    it "is a module" do
      Iconv::Failure.should be_kind_of(Module)
      Iconv::Failure.should_not be_kind_of(Class)
    end

    it "is included by Iconv::InvalidEncoding" do
      Iconv::Failure.should be_ancestor_of(Iconv::InvalidEncoding)
    end

    it "is included by Iconv::IllegalSequence" do
      Iconv::Failure.should be_ancestor_of(Iconv::IllegalSequence)
    end

    it "is included by Iconv::InvalidCharacter" do
      Iconv::Failure.should be_ancestor_of(Iconv::InvalidCharacter)
    end

    it "is included by Iconv::OutOfRange" do
      Iconv::Failure.should be_ancestor_of(Iconv::OutOfRange)
    end

    it "is included by Iconv::BrokenLibrary" do
      Iconv::Failure.should be_ancestor_of(Iconv::BrokenLibrary)
    end
  end
end
