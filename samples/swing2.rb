
# Import Java packages
require 'java'
module JavaSwing
  include_package "javax.swing"
  include_package "java.awt.event"
end

# Create a frame with the title "HelloWorldSwing"
frame = JavaSwing::JFrame.new()

# Create a button with the text "Klick Me!"
button = JavaSwing::JButton.new("Klick Me!", nil)

# Add an action to the button
action = JavaSwing::ActionListener.new
class << action
  def actionPerformed(evt)
    JavaSwing::JOptionPane.showMessageDialog (nil, "<html>Hello from <b><u>JRuby</u></b>.<br> Button '#{evt.getActionCommand()}' clicked.")
  end
end
button.addActionListener(action)

# Add the button to the frame
frame.getContentPane().add(button)

# Show frame
frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack()
frame.setVisible(true)
