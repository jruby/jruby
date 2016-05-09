require File.dirname(__FILE__) + "/../spec_helper"

describe "uri:classloader path strings" do
  let(:many_slashes_path) { "uri:classloader:////foo/b.gemspec" }
  let(:unc_like_path) { "uri:classloader://foo/b.gemspec" }
  let(:normal_path) { "uri:classloader:/foo/b.gemspec" }

  it "sent to expand_path will normalize slashes" do
    expect(File.expand_path(unc_like_path)).to eq(normal_path)
    # On windows this converts to many backslashes on front and backslashes
    # Everywhere. JRuby #3771 is tracking this.  We should add a check
    # to not only make sure :/ vs :\\\\ but also that not \ is present at
    # all as part of specing this.
    #    expect(File.expand_path(many_slashes_path)).to eq(normal_path)
  end
end
