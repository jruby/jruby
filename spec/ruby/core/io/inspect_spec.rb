require File.expand_path('../../../spec_helper', __FILE__)

describe "IO#inspect" do
  it "contains the file descriptor number" do
    begin
      i, o = IO.pipe
      i.inspect["fd #{i.fileno}"].should_not == nil
    ensure
      i.close
      o.close
    end
  end

  it "contains \"(closed)\" if the stream is closed" do
    begin
      i, o = IO.pipe
      i.close
      i.inspect["(closed)"].should_not == nil
    ensure
      o.close
    end
  end
end
