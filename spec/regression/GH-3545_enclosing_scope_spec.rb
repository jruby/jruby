class GH3545Loop
  
  def loop(dim1, dim2)
    for i in 0..dim1
      for j in 0..dim2
        0.upto(1) do |x|
          Hash.new 
        end
      end
    end
  end

end

describe GH3545Loop do

  it "GH-3545: should not raise retrieving current scope" do
    pending 'FIXME: https://github.com/jruby/jruby/issues/3545'
    expect { GH3545Loop.new.loop(2,2) }.not_to raise_exception
  end

end
