module CGISpecs
  def self.cgi_new(html = "html4")
    cgi = nil
    ruby_version_is "" ... "1.9" do
      cgi = CGI.new(html)
    end
    ruby_version_is "1.9" do
      cgi = CGI.new(:tag_maker => html)
    end
    cgi
  end

  def self.split(string)
    string.split("<").reject { |x| x.empty? }.map { |x| "<#{x}" }
  end
end
