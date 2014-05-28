require File.expand_path('../../../shared/rational/to_r', __FILE__)

describe "Rational#to_r" do
  it_behaves_like(:rational_to_r, :to_r)
end
