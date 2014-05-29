require File.expand_path('../shared/http_header', __FILE__)

ruby_version_is ''...'2.0' do
  describe "CGI#header" do
    it_behaves_like(:cgi_http_header, :header)
  end
end
