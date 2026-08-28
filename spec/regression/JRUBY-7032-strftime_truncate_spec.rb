require 'rspec'

describe "Time#strftime" do
  context "%3N is passed" do
    it "truncate output to 3 characters" do
      output = Time.now.strftime("%3N")
      expect(output.length).to eq(3)
    end
  end
  context "%2N is passed" do
    it "truncate output to 2 characters" do
      output = Time.now.strftime("%2N")
      expect(output.length).to eq(2)
    end
  end
end
