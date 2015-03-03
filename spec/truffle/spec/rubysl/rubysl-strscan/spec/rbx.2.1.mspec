# vim: filetype=ruby

class MSpecScript
  # Enable language features
  MSpec.enable_feature :encoding

  # These are encoding-aware methods backported to 1.8.7+ (eg String#bytes)
  MSpec.enable_feature :encoding_transition
end
