
# Import Java packages
require 'java'

include_class "java.awt.event.ActionListener"
include_class ["JButton", "JFrame", "JLabel", "JOptionPane"].map {|e| "javax.swing." + e}

frame = JFrame.new("Hello Swing")
button = JButton.new("Klick Me!")

class ClickAction < ActionListener
  def actionPerformed(evt)
    JOptionPane.showMessageDialog(nil, "<html>Hello from <b><u>JRuby</u></b>.<br> Button '#{evt.getActionCommand()}' clicked.")
  end
end
button.addActionListener(ClickAction.new)

# Add the button to the frame
frame.getContentPane().add(button)

# Show frame
frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack()
frame.setVisible(true)
