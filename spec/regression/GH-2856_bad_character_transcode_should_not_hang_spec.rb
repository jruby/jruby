require 'base64'

describe "A badly-encoded UTF-8 String reencoded with replacements as UTF-16 " do
  it "completes for all inputs" do
    random = Random.new
    # We obviously can't test all valid inputs, but we use the script from #2856 to try
    10_000.times do
      data = random.bytes(1000)
      data.force_encoding("UTF-8")
      data = data.encode("UTF-16", :undef => :replace, :invalid => :replace, :replace => '')

      expect(data).to_not eq(nil)
    end
  end
end unless RUBY_VERSION.index('1.8') == 0
