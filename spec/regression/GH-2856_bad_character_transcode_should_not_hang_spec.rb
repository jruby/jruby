require 'base64'
 
NUM_BYTES = 1000
 
rand = Random.new

describe "A badly-encoded UTF-8 String reencoded with replacements as UTF-16 " do
  it "completes for all inputs" do
    # We obviously can't test all valid inputs, but we use the script from #2856 to try
    10_000.times do
      data = rand.bytes(NUM_BYTES)
      data.force_encoding("UTF-8")
      data = data.encode("UTF-16", :undef => :replace, :invalid => :replace, :replace => '')

      expect(data).to_not eq(nil)
    end
  end
end
