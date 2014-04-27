require 'open3'

describe "Open3#popen3" do
  it "uses a leading Hash as additional environment variables" do
    Open3.popen3({'foo' => 'bar'}, 'env') do |i, out, err, thr|
      out.read.should =~ /foo=bar/
    end
  end
end
