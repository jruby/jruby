module CGISpecs
  def self.cgi_new(html = "html4")
    CGI.new(:tag_maker => html)
  end

  def self.split(string)
    string.split("<").reject { |x| x.empty? }.map { |x| "<#{x}" }
  end
end
