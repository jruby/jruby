require File.dirname(__FILE__) + "/../spec_helper"

describe "uri:classloader path strings" do
  let(:many_slashes_path) { "uri:classloader:////foo/b.gemspec" }
  let(:unc_like_path) { "uri:classloader:/foo/b.gemspec" }
  let(:normal_path) { "uri:classloader:/foo/b.gemspec" }

  let(:sub_path) { "../../../vendor/jface/*.jar" }
  let(:base_path) { "uri:classloader:/gems/swt-4.6.1/lib/swt/full.rb" }
  let(:resolved_path) { "uri:classloader:/gems/swt-4.6.1/vendor/jface/*.jar" }
  let(:resolved_path_win) { "uri:classloader:/gems/swt-4.6.1/vendor/jface/*.jar" }
  let(:windows) { RbConfig::CONFIG['host_os'] =~ /Windows|mswin/ }

  it "sent to expand_patyh will normalize slashes" do
    expect(File.expand_path(unc_like_path)).to eq(normal_path)

    # FIXME: It is unclear why 1 arg expand path is '/' and not '//' so perhaps
    # we need to resolve this inconsistency.
    if windows
      expect(File.expand_path(sub_path, base_path)).to eq(resolved_path_win)
    else
      expect(File.expand_path(sub_path, base_path)).to eq(resolved_path)
    end
    
    # On windows this converts to many backslashes on front and backslashes
    # Everywhere. JRuby #3771 is tracking this.  We should add a check
    # to not only make sure :/ vs :\\\\ but also that not \ is present at
    # all as part of specing this.
    #    expect(File.expand_path(many_slashes_path)).to eq(normal_path)
  end
end
