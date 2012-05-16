require 'rspec'

describe "JRUBY-6661: %N formatter" do
  it "does not affect the output of the next (narrow) formatter" do
    Time.now.strftime("%9N %Y").should =~ /\A\d{9} \d{4}\z/
  end
end
