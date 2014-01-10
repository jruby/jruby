# -*- encoding: utf-8 -*-

describe :string_match_escaped_literal, :shared => true do
  it "matches a literal Regexp that uses ASCII-only UTF-8 escape sequences" do
    "a b".match(/([\u{20}-\u{7e}])/)[0].should == "a"
  end
end
