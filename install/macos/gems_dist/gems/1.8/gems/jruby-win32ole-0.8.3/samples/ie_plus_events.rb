require 'win32ole'


$urls = []


def navigate(url)
  $urls << url
end


def stop_msg_loop
  puts "IE has exited..."
  $done = true
end


def default_handler(event, *args)
  case event
  when "BeforeNavigate"
    puts "Now Navigating to #{args[0]}..."
  end
end


ie = WIN32OLE.new('InternetExplorer.Application')
ie.visible = TRUE
ie.gohome
ev = WIN32OLE_EVENT.new(ie, 'DWebBrowserEvents')


ev.on_event {|*args| default_handler(*args)}
ev.on_event("NavigateComplete2") {|obj, url| navigate(url)}
ev.on_event("OnQuit") {|*args| stop_msg_loop}


while !$done do
  WIN32OLE_EVENT.message_loop
  sleep 0.5
end

puts "You Navigated to the following URLs: "
$urls.each_with_index do |url, i|
  puts "(#{i+1}) #{url}"
end
