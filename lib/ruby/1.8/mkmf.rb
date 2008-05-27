# JRuby does not support mkmf yet, so we fail hard here with a useful message
raise NotImplementedError.new(
  "JRuby does not support native extensions. Check wiki.jruby.org for alternatives.")