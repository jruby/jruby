# GH-3155
describe "Array#map with delete inside the loop" do
  it "must not produce invalid array contents" do
    array = [1, 2, 3]
    array2 = array.map { |v| array.delete(v); v + 1 }

    expect(array2.compact).to eq array2
    expect(array2.inspect).to eq "[2, 4]"

    # added to test specialized arrays
    array = [1, 2]
    array2 = array.map { |v| array.delete(v); v + 1 }

    expect(array2.compact).to eq array2
    expect(array2.inspect).to eq "[2]"
  end
end
