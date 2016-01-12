require 'erb'

include ERB::Util

module CG
  class Source
    attr_reader :file, :line_start, :line_end

    def initialize(file, line_start, line_end)
      @file = file
      @line_start = Integer(line_start)
      @line_end = Integer(line_end)
    end

    def to_s
      "#{file}:#{line_start}:#{line_end}"
    end

    def lines
      if file == '(unknown)'
        ['(unknown)']
      elsif !File.exist?(file)
        ['(doesn\'t exist)']
      else
        lines = File.readlines(file)[line_start - 1, line_end - line_start + 1]
        if lines.nil?
          ['(error)']
        else
          lines.map(&:rstrip)
        end
      end
    end
  end

  class Method
    attr_reader :id, :name, :source, :versions, :callsites

    def initialize(id, name, source)
      @id = Integer(id)
      @name = name
      @source = source
      @versions = []
      @callsites = []
    end

    def core?
      source.file.start_with?('truffle:') || source.file == '(unknown)'
    end
  end

  class MethodVersion
    attr_reader :id, :method, :callsite_versions

    def initialize(id, method)
      @id = Integer(id)
      @method = method
      @callsite_versions = []
    end
  end

  class CallSite
    attr_reader :id, :method, :line, :versions

    def initialize(id, method, line)
      @id = Integer(id)
      @method = method
      @line = Integer(line)
      @versions = []
    end
  end

  class CallSiteVersion
    attr_reader :id, :callsite, :method_version, :calls

    def initialize(id, callsite, method_version)
      @id = Integer(id)
      @callsite = callsite
      @method_version = method_version
      @calls = []
    end
  end
end

objects = {}

ARGF.each_line do |line|
  line = line.split
  case line[0]
  when 'method'
    method = CG::Method.new(line[1], line[2], CG::Source.new(*line[3, 6]))
    objects[method.id] = method
  when 'method-version'
    method = objects[Integer(line[1])]
    method_version = CG::MethodVersion.new(line[2], method)
    objects[method_version.id] = method_version
    method.versions.push method_version
  when 'local'
  when 'callsite'
    method = objects[Integer(line[1])]
    callsite = CG::CallSite.new(line[2], method, line[3])
    objects[callsite.id] = callsite
    method.callsites.push callsite
  when 'callsite-version'
    callsite = objects[Integer(line[1])]
    method_version = objects[Integer(line[2])]
    callsite_version = CG::CallSiteVersion.new(line[3], callsite, method_version)
    objects[callsite_version.id] = callsite_version
    callsite.versions.push callsite_version
    method_version.callsite_versions.push callsite_version
  when 'calls'
    callsite_version = objects[Integer(line[1])]

    if line[2] == 'mega'
      callsite_version.calls.push :mega
    else
      callsite_version.calls.push Integer(line[2])
    end
  else
    raise line.inspect
  end
end

objects.values.each do |object|
  if object.is_a? CG::CallSiteVersion
    callsite_version = object
    callsite_version.calls.map! do |call|
      if call == :mega
        :mega
      else
        objects[call]
      end
    end
  end
end

def annotate(method_version, offset)
  line_number = method_version.method.source.line_start + offset

  comments = []

  method_version.callsite_versions.each do |callsite_version|
    if callsite_version.callsite.line == line_number
      callsite_version.calls.each do |called|
        if called == :mega
          comments.push 'calls mega'
        else
          comment = "calls #{called.method.name}"
          comment += " #{called.method.source}" unless called.method.core?
          comments.push comment
        end
      end
    end
  end

  if comments.empty?
    ''
  else
    " # #{comments.join(', ')}"
  end
end

puts ERB.new(%{
  <html>
  <header>
    <title>Call Graph Visualisation</title>
    <style>
      .method-version {
        background: grey;
        margin: 1em;
        padding: 1em;
      }
    </style>
  </header>
  <body>
  <% objects.values.select { |o| o.is_a?(CG::Method) && !o.core? }.each do |method| %>
    <h2><%= h(method.name) %></h2>
    <p><%= h(method.source) %></p>
    <% method.versions.each do |method_version| %>
      <a name='method-version-<%= method_version.id %>'></a><div class='method-version'>
        <pre><% method.source.lines.each_with_index do |code, offset| %><%= h(code + annotate(method_version, offset) + '\n') %><% end %></pre>
      </div>
    <% end %>
  <% end %>
  </body>
  </html>
}).result
