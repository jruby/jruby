# Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved. This
# code is released under a tri EPL/GPL/LGPL license. You can use it,
# redistribute it and/or modify it under the terms of the:
#
# Eclipse Public License version 1.0
# GNU General Public License version 2
# GNU Lesser General Public License version 2.1

# -Xtruffle.callgraph=true -Xtruffle.callgraph.write=test.callgraph
# ruby tool/truffle/callgraph2html.rb < test.callgraph > callgraph.html && open callgraph.html

require 'erb'
require 'set'

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
        ['(native)']
      elsif file.start_with?('truffle:')
        ['(core)']
      else
        File.readlines(file)[line_start - 1, line_end - line_start + 1].map(&:rstrip)
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
      source.file.include?('/core/') || source.file.include?('core.rb') || source.file == '(unknown)'
    end

    def hidden?
      source.file == 'run_jruby_root' || source.file == 'context' || name == 'Truffle#run_jruby_root' || name == 'Truffle#context'
    end

    def reachable
      callsites # versions aren't reachable - find them through calls
    end
  end

  class MethodVersion
    attr_reader :id, :method, :callsite_versions, :called_from

    def initialize(id, method)
      @id = Integer(id)
      @method = method
      @callsite_versions = []
      @called_from = []
    end

    def reachable
      [method] + callsite_versions # called_from isn't reachable
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

    def reachable
      [method] + versions
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

    def reachable
      [callsite] + calls # method_version isn't reachable - find it through calls
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
      # We just store the method id here for now as we may not have seen all methods yet
      callsite_version.calls.push Integer(line[2])
    end
  else
    raise line.inspect
  end
end

# Resolve method ids to point to the actual object

objects.values.each do |object|
  if object.is_a? CG::CallSiteVersion
    callsite_version = object
    callsite_version.calls.map! do |call|
      if call == :mega
        :mega
      else
        called = objects[call]
        called.called_from.push callsite_version
        called
      end
    end
  end
end

# Find which objects were actually used

reachable_objects = Set.new

reachable_worklist = objects.values.select { |o|
  (o.is_a?(CG::Method) && !o.core?) || (o.is_a?(CG::MethodVersion) && o.method.name == '<main>' && !o.method.core?)
}

until reachable_worklist.empty?
  object = reachable_worklist.pop
  next if object == :mega
  next if reachable_objects.include? object
  reachable_objects.add object
  reachable_worklist.push *object.reachable
end

def annotate(method_version, offset)
  line_number = method_version.method.source.line_start + offset

  comments = []

  method_version.callsite_versions.each do |callsite_version|
    if callsite_version.callsite.line == line_number
      callsite_version.calls.each do |called|
        if called == :mega
          comments.push 'calls mega'
        elsif !called.method.hidden?
          comments.push "calls <a href='#method-version-#{called.id}'>#{called.method.name}</a>"
        end
      end
    end
  end

  if comments.empty?
    ''
  else
    '# ' + comments.join(', ')
  end
end

puts ERB.new(%{<html>
<header>
  <title>Call Graph Visualisation</title>
  <style>
    .method-version {
      background: AntiqueWhite;
      margin: 1em;
      padding: 1em;
    }
    .method-version:target {
      background: BurlyWood;
    }
    p.code {
      margin: 0;
    }
  </style>
</header>
<body>
<% reachable_objects.select { |o| o.is_a?(CG::Method) && !o.hidden? }.each do |method| %>
  <h2><%= h(method.name) %></h2>
  <p><%= h(method.source) %></p>
  <% method.versions.each do |method_version| %>
    <% if reachable_objects.include?(method_version) %>
      <div id='method-version-<%= method_version.id %>' class='method-version'>
        <% unless method_version.called_from.empty? %>
        <p>Called from:</p> 
        <ul>
          <% method_version.called_from.each do |caller| %>
            <li><a href='#method-version-<%= caller.method_version.id %>'><%= h(caller.method_version.method.name) %></a></li>
          <% end %>
        </ul>
        <% end %>
        <% method.source.lines.each_with_index do |code, offset| %>
          <p class='code'>
            <code><%= h(code + ' ').gsub(' ', '&nbsp;') %></code>
            <%= annotate(method_version, offset) %>
          </p>
        <% end %>
      </div>
    <% end %>
  <% end %>
<% end %>
</body>
</html>}).result
