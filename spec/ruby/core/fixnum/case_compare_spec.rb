require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/equal', __FILE__)

ruby_version_is "1.9" do
  describe "Fixnum#===" do
    it_behaves_like :fixnum_equal, :===

    it "should pickuo overriden stuff" do
      class Fixnum
        def ===(other)
          true
        end
      end

      ret = case 4
        when 3
          "YES"
        else
          "NO"
        end

      ret.should == "YES"
      Fixnum.send(:remove_method, :===)
    end
  end
end
