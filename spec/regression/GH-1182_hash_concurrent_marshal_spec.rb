class GH1182_Value
  attr_reader :j

  def initialize i, j
    @i=i
    @j=j
  end

  def to_s
    "#{@i} #{@j}"
  end
end

class GH1182_Test
  attr_reader :hash, :obj

  def initialize
    @hash={}
    @obj= GH1182_Value.new(0, -2)
  end

  def marshal_dump
    [@hash, @obj]
  end

  def marshal_load arr
    @hash = arr[0]
    @obj = arr[1]
  end
end

describe "A Hash being marshaled while modified" do
  it "produces valid marshal data or raises error if it cannot" do

    hash_size=1000
    dump_count=30
    sample_count = dump_count/20

    test = GH1182_Test.new

    def test_puts msg
      puts msg
    end

    Thread.new do
      hash_size.times do |i|
        test.hash[i] = GH1182_Value.new(i, 1)
        sleep 0.001 if i.divmod(sample_count)[1]==0
      end
      hash_size.times do |i|
        test.hash.delete(rand(hash_size - i))
        sleep 0.001 if i.divmod(sample_count)[1]==0
      end
    end

    dump_count.times do |i|
      begin
        d = Marshal.dump test
        t = Marshal.load(d);

        t.obj.should be_instance_of GH1182_Value
        #ok
        sleep 0.05
      rescue ConcurrencyError => ex
        # ok
      end

    end

  end
end