# Output package.html for org.jruby.util.cli
require 'erb'

template = <<TEMPLATE
<body>
The CLI package provides utilities for JRuby's command-line interface.

<div>A complete listing of Options-borne configuration properties:

<dl>
<%
require 'java'
org.jruby.util.cli.Options::PROPERTIES.each do |property|
%>
<dt><%= property.name %></dt>
<dd>
<div><%= property.description %></div>
<div>Options:
<% property.options.each do |option| %>
<%=
  if property.defval == option
    "<b>jruby." + option.to_s + "</b>"
  else
    "jruby." + option.to_s
  end
%>
<% end %></div>
</dd>
<% end %>
</dl>
<div>
</body>
TEMPLATE

ERB.new(template).result