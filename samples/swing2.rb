# Import Java packages
JavaObject.import "java.awt.event"
JavaObject.import "javax.swing"

# Create a frame with the title "HelloWorldSwing"
frame = JFrame.new("HelloWorldSwing")

# Create a button with the text "Klick Me!"
button = JButton.new("Klick Me!")

# Add an action to the button
button.addActionListener JavaInterface.listener ("ActionListener", "actionPerformed") {|evt| JOptionPane.showMessageDialog NIL, "<html>Hello from JRuby.<br>The button was: <i>" + evt.getActionCommand + "</i>"}

# Add the button to the frame
frame.getContentPane().add(button)

# Show frame
frame.setDefaultCloseOperation(JFrame::EXIT_ON_CLOSE)
frame.pack();
frame.setVisible(true);
