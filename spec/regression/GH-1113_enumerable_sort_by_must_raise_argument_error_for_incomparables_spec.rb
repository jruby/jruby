# GH-1113: Enumerable#sort_by raises wrong error for incomparable items
# https://github.com/jruby/jruby/issues/1113

describe "Enumerable#sort_by with a block producing incomparable items" do
  it "raises ArgumentError" do
    inc1 = [1,2,3,4,5]
    inc2 = 1..5

    lambda do
      inc1.sort_by {|i| next nil if i == 3; i}
    end.should raise_error(ArgumentError)

    lambda do
      inc2.sort_by {|i| next nil if i == 3; i}
    end.should raise_error(ArgumentError)
  end
end
