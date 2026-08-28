require 'rspec'

describe "A parse which uses enough parser states" do
  it "will properly allocate and complete without NPEing" do
    def test(num)
      case num
      when 55
      when 64
      when 65
      when 66
      when 67
      when 68
      when 69
      when 80
      when 81
      when 82
      when 83
      when 84
      when 85
      when 86
      when 87
      when 88
      when 96
      when 97
      when 98
      when 99
      when 100
      when 101
      when 112
      when 120
      when 121
      when 128
      when 130
      when 144
      when 145
      when 146
      when 147
      when 148
      when 149
      when 150
      when 160
      when 176
      when 177
      when 178
      when 179
      when 180
      when 181
      when 182
      when 183
      when 184
      when 185
      when 190
      when 200
      when 201
      when 202
      when 250
      when 251
      when 260
      when 261
      when 280
      when 281
      when 300
      when 301
      when 302
      when 320
      when 350
      when 500
        return "Video"
      end
    end

    expect(test(500)).to eq("Video")
  end
end
