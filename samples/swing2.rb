# Import Java packages
Java.import "java.awt.event"
Java.import "javax.swing"

# Create a frame with the title "HelloWorldSwing"
frame = JFrame.new "HelloWorldSwing"

# Create a button with the text "Klick Me!"
button = JButton.new "Klick Me!"

# Add an action to the button
button.addActionListener ActionListener.actionPerformed { |evt| 
  JOptionPane.showMessageDialog NIL, 
    "<html>Hello from JRuby.<br>The button was: <i>#{evt.actionCommand}</i>"
}

# Add the button to the frame
frame.contentPane.add button

# Show frame
frame.defaultCloseOperation = JFrame::EXIT_ON_CLOSE
frame.pack
frame.visible = true
