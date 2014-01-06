require File.expand_path('../../../spec_helper', __FILE__)

ruby_version_is ""..."1.9.1" do
  describe "Fixnum#id2name" do

    not_compliant_on :rubinius do
      it "returns the string name of the object whose symbol ID is self" do
        a = :@sym
        b = :@ruby
        c = :@rubinius
        a.to_i.id2name.should == '@sym'
        b.to_i.id2name.should == '@ruby'
        c.to_i.id2name.should == '@rubinius'
      end

      it "returns nil if there is no symbol in the symbol table with this value" do
        100000000.id2name.should == nil
      end
    end
  end
end
