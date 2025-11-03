# Note: this file is modified from CRuby's gem_prelude.rb and should be carefully updated
begin
  require 'rubygems'
rescue LoadError => e
  raise unless e.path == 'rubygems'

  warn "'RubyGems' were not loaded."
else
  require 'bundled_gems'

  begin
    require 'did_you_mean'
  rescue LoadError
    warn "'did_you_mean' was not loaded."
  end if defined?(DidYouMean)

  begin
    require 'syntax_suggest/core_ext'
  rescue LoadError
    warn "'syntax_suggest' was not loaded."
  end if defined?(SyntaxSuggest)

  # clear RubyGems paths so they can be reinitialized after boot
  Gem.clear_paths
end if defined?(Gem)
