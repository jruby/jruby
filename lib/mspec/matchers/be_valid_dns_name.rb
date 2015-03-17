class BeValidDNSName
  # http://stackoverflow.com/questions/106179/regular-expression-to-match-hostname-or-ip-address
  # ftp://ftp.rfc-editor.org/in-notes/rfc3696.txt
  # http://domainkeys.sourceforge.net/underscore.html
  VALID_DNS = /^(([a-zA-Z0-9_]|[a-zA-Z0-9_][a-zA-Z0-9\-_]*[a-zA-Z0-9_])\.)*([A-Za-z_]|[A-Za-z_][A-Za-z0-9\-_]*[A-Za-z0-9_])\.?$/

  def matches?(actual)
    @actual = actual
    (VALID_DNS =~ @actual) == 0
  end

  def failure_message
    ["Expected '#{@actual}'", "to be a valid DNS name"]
  end

  def negative_failure_message
    ["Expected '#{@actual}'", "not to be a valid DNS name"]
  end
end

class Object
  def be_valid_DNS_name
    BeValidDNSName.new
  end
end
