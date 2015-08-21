require File.dirname(__FILE__) + "/../spec_helper"

describe "java.util.regex.Matcher.end" do
  it "does not blow stack" do
    # See https://jira.codehaus.org/browse/JRUBY-6571
    import 'java.util.regex.Pattern'
    import 'java.util.regex.Matcher'

    s = 'hello world'
    r = Pattern.compile("[eo]")
    m = r.matcher(s)

    while m.find()
      expect{ m.end(0) }.not_to raise_error
    end
  end
end