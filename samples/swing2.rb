# Import Java packages
require 'java'

JFrame = javax.swing.JFrame

frame = JFrame.new("Hello Swing")
button = javax.swing.JButton.new("Klick Me!")

class ClickAction < java.awt.event.ActionListener
  def actionPerformed(evt)
    javax.swing.JOptionPane.showMessageDialog(nil, "<html>Hello from <b><u>JRuby</u></b>.<br> Button '#{evt.getActionCommand()}' clicked.")
  end
end
button.addActionListener(ClickAction.new)

# Add the button to the frame
frame.getContentPane().add(button)

# Show frame
frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack()
frame.setVisible(true)
