describe "The for expression" do
  it "repeats the loop from the beginning with 'retry'" do
    j = 0
    for i in 1..5
      j += i

      retry if i == 3 && j < 7
    end

    j.should == 21
  end
end
