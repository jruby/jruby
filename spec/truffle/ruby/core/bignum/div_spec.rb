require File.expand_path('../../../spec_helper', __FILE__)
require File.expand_path('../shared/divide', __FILE__)

describe "Bignum#div" do
  it_behaves_like(:bignum_divide, :div)

  ruby_bug "#", "1.8.6" do
    it "returns a result of integer division of self by a float argument" do
      bignum_value(88).div(0xffff_ffff.to_f).should eql(2147483648)
      bignum_value(88).div(bignum_value(88).to_f).should eql(1)
      bignum_value(88).div(-bignum_value(88).to_f).should eql(-1)
    end
  end

  ruby_version_is ""..."1.9" do
    it "raises FloatDomainError if the argument is a Float zero" do
      lambda { bignum_value(88).div(0.0) }.should raise_error(FloatDomainError)
      lambda { bignum_value(88).div(-0.0) }.should raise_error(FloatDomainError)
    end
  end

  ruby_version_is "1.9" do
    ruby_bug "#5490", "2.0.0" do
      it "raises ZeroDivisionError if the argument is Float zero" do
        lambda { bignum_value(88).div(0.0) }.should raise_error(ZeroDivisionError)
        lambda { bignum_value(88).div(-0.0) }.should raise_error(ZeroDivisionError)
      end
    end
  end
end
