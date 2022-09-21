/**
 * JavaDatasetPreprocessorGUI - a Frontend for the Preprocessor.
 *
 * @author Jesse Phillips <j.m.phillips@lancaster.ac.uk>
 * @version 0.0.1
 **/
package uk.ac.lancs.scc.phd.jesse;

import javax.swing.*;
import java.awt.*;
import java.io.*;
import java.nio.file.*;

public class JavaDatasetPreprocessorGUI
{
  private JTextField textField1;
  private JButton goButton;
  private JPanel mainPanel;
  private JLabel messageLabel;

  public static void main(String[] args)
  {
    JFrame frame = new JFrame("Java Dataset Preprocessor");
    frame.setContentPane(new JavaDatasetPreprocessorGUI().mainPanel);
    frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    //frame.pack();
    frame.setSize(512, 128);
    frame.setVisible(true);
  }

  /**
   * Checks if a given filepath exists.
   *
   * @param path the path to the file
   * @return true if it exists.
   **/
  private boolean checkIsFile(String path)
  {
      Path p = Paths.get(path);
    return Files.exists(p);
  }

  /**
   * Resets the GUI form to its default state.  For when there's an error.
   **/
  private void resetForm()
  {
    messageLabel.setText("");
    textField1.setText("");
    goButton.setEnabled(true);
  }

  /**
   * Shows a message dialogue to the user.
   *
   * @param message the message to use.
   * @param title the title for the titlebar.
   * @param type the type of message (influences the icon used).
   **/
  private void showMessage(String message, String title, int type)
  {
    JOptionPane.showMessageDialog(null,
      message,
      title,
      type);
  }

  /**
   * Shows an error message dialogue, then resets the form.
   * Builds on showMessage().
   *
   * @param message the error message.
   */
  private void showError(String message)
  {
    showMessage(message, "Error", JOptionPane.ERROR_MESSAGE);
    resetForm();
  }

  /**
   * Does the preprocessing the user has requested.
   *
   * @param path the path to the data.
   */
  private void doPreprocessing(String path)
  {
    goButton.setEnabled(false);
    JavaDatasetPreprocessor jDP = new JavaDatasetPreprocessor();
    jDP.setDataLocation(path);
    try
    {
      jDP.getData(jDP.getDataLocation());
    } catch (IOException e)
    {
      showError("Couldn't find data! " + e.getMessage());
      return;
    }
    messageLabel.setText("Trimming methods list to valid methods only.");
    jDP.trimToValidData();
    messageLabel.setText("Removing repeat entries.");
    jDP.removeRepeatEntries();
    messageLabel.setText("Trimming to 500k records for testing.");
    jDP.shrinkLists();
    messageLabel.setText("Stripping HTML tags from Javadoc method summaries.");
    jDP.stripHTMLFromSummaries();
    messageLabel.setText("Extracting attempted summaries from Javadoc.");
    jDP.extractAssumedSummaryFromJdoc();
    messageLabel.setText("Lowercasing summaries.");
    jDP.lowercaseSummaries();
    messageLabel.setText("Stripping special characters from summaries.");
    jDP.stripSpecialCharsFromSummaries();
    try
    {
      jDP.saveData(jDP.getDataLocation());
      jDP.prepareDataForNeuralCodeSum(jDP.getDataLocation());
      showMessage("Data saved at:" + jDP.getDataLocation(),
        "Saved!",
        JOptionPane.INFORMATION_MESSAGE);
      Desktop d = Desktop.getDesktop();
      File f = new File(jDP.getDataLocation());
      d.open(f);
    } catch (IOException e)
    {
      showError("Couldn't save data! " + e.getMessage());
    }
  }

  /**
   * Constructor for the GUI.  Contains an Event Handler for the button press.
   **/
  public JavaDatasetPreprocessorGUI()
  {
    goButton.addActionListener(e ->
    {
      String path = textField1.getText();
      if (path.length() < 1)
        return;
      if (checkIsFile(path))
        doPreprocessing(path);
      else
        showError("Could not find file: " + path);
    });
  }
}
