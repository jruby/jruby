# Import Java packages
include Java

import javax.swing.JFrame

frame = JFrame.new("Hello Swing")
button = javax.swing.JButton.new("Klick Me!")

class ClickAction 
  include java.awt.event.ActionListener
  def actionPerformed(evt)
    javax.swing.JOptionPane.showMessageDialog(nil, <<EOS)
<html>Hello from <b><u>JRuby</u></b>.<br> 
Button '#{evt.getActionCommand()}' clicked.
EOS
  end
end
button.add_action_listener(ClickAction.new)

# Add the button to the frame
frame.get_content_pane.add(button)

# Show frame
frame.set_default_close_operation(JFrame::EXIT_ON_CLOSE)
frame.pack
frame.visible = true

# Sleep the main thread, so we don't exit
Thread.stop