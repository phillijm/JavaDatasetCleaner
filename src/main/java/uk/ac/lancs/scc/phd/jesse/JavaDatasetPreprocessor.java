/**
 * JavaDatasetPreprocessor Preprocesses the Funcom dataset for our use.
 *
 * @author Jesse Phillips <j.m.phillips@lancaster.ac.uk>
 * @version 0.0.1
 **/
package uk.ac.lancs.scc.phd.jesse;

import com.github.javaparser.*;
import com.github.javaparser.ast.body.BodyDeclaration;
import org.json.*;
import java.io.*;
import java.nio.file.*;
import java.util.*;

public class JavaDatasetPreprocessor
{
  private ArrayList<String> methods = new ArrayList<>();
  private ArrayList<String> summaries = new ArrayList<>();
  private long goodMethods = 0;
  private long badMethods = 0;
  private String dataLocation ="";

  public JavaDatasetPreprocessor(String path)
  {
    setDataLocation(path);
    System.out.println("Data will be searched for and saved in: " + dataLocation);
    try
    {
      System.out.println("Searching for data:");
      getData(dataLocation);
    } catch (IOException e)
    {
      System.out.println("Couldn't find data!");
      e.printStackTrace();
    }
    System.out.println("Trimming methods list to valid methods only.");
    trimToValidData();
    System.out.println("Trimming to 500k records for testing.");
    shrinkLists();
    System.out.println("Stripping HTML tags from Javadoc method summaries.");
    stripHTMLFromSummaries();
    System.out.println("Extracting attempted summaries from Javadoc.");
    extractAssumedSummaryFromJdoc();
    System.out.println("Lowercasing summaries.");
    lowercaseSummaries();
    System.out.println("Stripping special characters from summaries.");
    stripSpecialCharsFromSummaries();
    try
    {
      System.out.println("Saving data");
      saveData(dataLocation);
      prepareDataForNeuralCodeSum(dataLocation);
      System.out.println("Data saved at " + dataLocation + "!");
    } catch (IOException e)
    {
      System.out.println("Could not save data!");
      e.printStackTrace();
    }
  }

  /**
   * Empty constructor.
   * To allow the GUI to construct a Preprocessor without running automatically.
   **/
  public JavaDatasetPreprocessor()
  {
    //silence is golden.
  }

  /**
   * Sets the (path) location the Preprocessor uses for data.
   *
   * @param path the (relative or absolute) filepath.
   */
  public void setDataLocation(String path)
  {
    this.dataLocation = path;
    if (dataLocation.charAt(dataLocation.length() - 1) != '/')
      dataLocation += '/';
  }

  /**
   * Gets the (path) location the Preprocessor uses for data.
   *
   * @return the filepath.
   */
  public String getDataLocation()
  {
    return this.dataLocation;
  }

  /**
   * Gets the number of good methods.
   *
   * @return the number of good methods.
   */
  public long getNumberOfGoodMethods()
  {
    return goodMethods;
  }

  /**
   * Gets the number of bad methods.
   *
   * @return the number of bad methods.
   */
  public long getNumberOfBadMethods()
  {
    return badMethods;
  }

  /**
   * Gets data from the Funcom dataset, and stores it in ArrayLists.
   *
   * @param dataLocation where to look for the data.
   * @throws IOException
   **/
  public void getData(String dataLocation) throws IOException
  {
    FileSystem fS = FileSystems.getDefault();
    String functions = Files.readString(fS.getPath(dataLocation + "functions.json"));
    JSONObject functionsJSON = new JSONObject(functions);
    for (String id: functionsJSON.keySet())
      methods.add(functionsJSON.getString(id));

    String comments = Files.readString(fS.getPath(dataLocation + "comments.json"));
    JSONObject commentsJSON = new JSONObject(comments);
    for (String id: commentsJSON.keySet())
      summaries.add(commentsJSON.getString(id));
  }

  /**
   * Trims the method data to valid records only.
   * Removes any comments in the process.
   **/
  public void trimToValidData()
  {
    for (int cnt = 0; cnt < methods.size(); cnt++)
    {
      JavaParser jp = JP.createJavaParser(false);
      ParseResult<BodyDeclaration<?>> result = jp.parseBodyDeclaration(methods.get(cnt));
      try
      {
        BodyDeclaration<?> thing = result.getResult().get();
        JP.removeComments(thing);
        methods.set(cnt, "" + result.getResult().get() + "");
        goodMethods++;
      } catch (NoSuchElementException e)
      {
        badMethods++;
        methods.remove(cnt);
        summaries.remove(cnt);
        System.out.println("could not parse:\n" + methods.get(cnt));
        System.out.println("\n++++++++++++++++++++++++++++++++++++++++++++++++++++++");
      }
    }
  }

  /**
   * Shrinks both ArrayLists to the first 500k records.
   * NB: We actually shrink to 800k, because there's a lot of un-parsable
   * methods, but we'll only use 500k of these.  The only reason we shrink now
   * as well is to save on memory.
   **/
  public void shrinkLists()
  {
    //Actually using 800k, of which only the first 500k is used.
    while(methods.size() > 800000)
      methods.remove(methods.size() - 1);
    while(summaries.size() > 800000)
      summaries.remove(summaries.size() - 1);
  }

  /**
   * Strips HTML tags from the summaries, using a RegEx.
   **/
  public void stripHTMLFromSummaries()
  {
    summaries.replaceAll(s1 -> s1.replaceAll("<[^<]+?>", " "));
  }

  /**
   * Extracts the data we hope is a summary from the Javadoc comments.
   * It just uses the first line of text with more than 8 non-space characters.
   **/
  public void extractAssumedSummaryFromJdoc()
  {
    for (int cnt = 0; cnt < summaries.size(); cnt++)
    {
      String s = summaries.get(cnt);
      String[] parts = s.split("\n");
      for (String line: parts)
      {
        String trimmedLine = line.replaceAll("\t", " ");
        trimmedLine = trimmedLine.trim();
        if (trimmedLine.length() > 8)
        {
          s = line.trim();
          break;
        }
      }
      summaries.set(cnt, s);
    }
  }

  /**
   * Lowercases the method summaries.
   **/
  public void lowercaseSummaries()
  {
    summaries.replaceAll(String::toLowerCase);
  }

  /**
   * Strips special chars from the method summaries using a RegEx.
   **/
  public void stripSpecialCharsFromSummaries()
  {
    summaries.replaceAll(s1 -> s1.replaceAll("[^a-z0-9 .']", " "));
  }

  /**
   * Saves the data we've processed into two big files, mainly for debugging.
   *
   * @param dir where to save them.
   * @throws IOException
   **/
  public void saveData(String dir) throws IOException
  {
    FileSystem fS = FileSystems.getDefault();
    Path methodPath = fS.getPath(dir + "methodsProcessed.json");
    Path summaryPath = fS.getPath(dir + "summariesProcessed.json");
    FileWriter fp = new FileWriter(methodPath.toFile());
    JSONArray data = new JSONArray(methods);
    JSONObject jSON = new JSONObject();
    jSON.append("methods", data);
    fp.write(jSON.toString(4));
    fp.close();
    fp = new FileWriter(summaryPath.toFile());
    data = new JSONArray(summaries);
    jSON = new JSONObject();
    jSON.append("summaries", data);
    fp.write(jSON.toString(4));
    fp.close();
  }

  /**
   * Removes repeat entries from the datasets.
   **/
  public void removeRepeatEntries()
  {
    ArrayList<String> newMethods = new ArrayList<>();
    ArrayList<String> newSummaries = new ArrayList<>();
    for (int cnt = 0; cnt < summaries.size(); cnt++)
    {
      if (!newSummaries.contains(summaries.get(cnt)))
      {
        newMethods.add(methods.get(cnt));
        newSummaries.add(summaries.get(cnt));
      }
    }
    methods = new ArrayList<>(newMethods);
    summaries = new ArrayList<>(newSummaries);
  }

  /**
   * Strips newline characters from the methods and summaries ArrayLists.
   **/
  public void stripNewlines()
  {
    methods.replaceAll(s1 -> s1.replaceAll("\n", " "));
    summaries.replaceAll(s1 -> s1.replaceAll("\n", " "));
  }

  /**
   * Tokenises all methods in the methods ArrayList.
   *
   * @return ArrayList of tokenised methods.
   **/
  public ArrayList <String> tokeniseMethods()
  {
    ArrayList<String> tokenisedMethods = new ArrayList<>();
    for (String s : methods)
    {
      // Split the string on camel case.
      String[] camelSplitData = s.split("(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
      StringBuilder tmp = new StringBuilder();
      for (String c : camelSplitData)
        tmp.append(" ").append(c);
      tmp = new StringBuilder(tmp.toString().trim());
      // Also space out punctuation and lowercase it.
      String tokenisedMethod = tmp.toString().replaceAll("\\p{Punct}", " $0 ");
      tokenisedMethod = tokenisedMethod.replaceAll("\\s+", " ");
      tokenisedMethods.add(tokenisedMethod.toLowerCase());
    }
    return tokenisedMethods;
  }

  /**
   * Removes repeat data from the datasets - including a tokenised one if
   * passed as a parameter.
   *
   * @param tokenisedMethods the dataset of tokenised methods.
   * @return the dataset of tokenised methods with repeat data removed.
   **/
  public ArrayList<String> removeRepeatData(ArrayList<String> tokenisedMethods)
  {
    ArrayList<String> newMethods = new ArrayList<>();
    ArrayList<String> newTokMethods = new ArrayList<>();
    ArrayList<String> newSummaries = new ArrayList<>();
    for (int cnt = 0; cnt < summaries.size(); cnt++)
    {
      if (!newSummaries.contains(summaries.get(cnt)))
      {
        newMethods.add(methods.get(cnt));
        newTokMethods.add(tokenisedMethods.get(cnt));
        newSummaries.add(summaries.get(cnt));
      }
    }
    methods = new ArrayList<>(newMethods);
    summaries = new ArrayList<>(newSummaries);
    tokenisedMethods = new ArrayList<>(newTokMethods);
    return tokenisedMethods;
  }

  /**
   * Generates the random order in which to split the dataset.
   *
   * @param length The number of records for which to generate an order
   * @return the random order.
   **/
  public ArrayList<Integer> generateSeed(Integer length)
  {
    ArrayList<Integer> randomOrder = new ArrayList<>();
    for (int cnt = 0; cnt < length; cnt++)
      randomOrder.add(cnt);
    Collections.shuffle(randomOrder);
    return randomOrder;
  }

  /**
   * Prepares and saves the data in the format needed by NeuralCodeSum.
   * Big ugly monstrosity, should probably be refactored.
   *
   * @param dir where to save them.
   * @throws IOException
   */
  public void prepareDataForNeuralCodeSum(String dir) throws IOException
  {
    // make test/train/dev dirs
    FileSystem fS = FileSystems.getDefault();
    Path testPath = fS.getPath(dir + "test");
    Files.createDirectories(testPath);
    Path trainPath = fS.getPath(dir + "train");
    Files.createDirectories(trainPath);
    Path devPath = fS.getPath(dir + "dev");
    Files.createDirectories(devPath);

    // strip newlines
    stripNewlines();

    // make tokenised version of the code
    ArrayList<String> tokenisedMethods = tokeniseMethods();

    // remove repeat data if it's still present.
    tokenisedMethods = removeRepeatData(tokenisedMethods);

    // generate random order
    ArrayList<Integer> randomOrder = generateSeed(500000);

    // save the files in dirs 10/80/10
    Path testMethodPath = fS.getPath(testPath.toString() + "/code.original");
    Path testTokenisedPath = fS.getPath(testPath.toString() + "/code.original_subtoken");
    Path testSummaryPath = fS.getPath(testPath.toString() + "/javadoc.original");
    FileWriter fp = new FileWriter(testMethodPath.toFile());
    FileWriter fp1 = new FileWriter(testTokenisedPath.toFile());
    FileWriter fp2 = new FileWriter(testSummaryPath.toFile());
    for (int cnt = 0; cnt < 50000; cnt++)
    {
      fp.write(methods.get(randomOrder.get(cnt)) + "\n");
      fp1.write(tokenisedMethods.get(randomOrder.get(cnt)) + "\n");
      fp2.write(summaries.get(randomOrder.get(cnt)).trim() + "\n");
    }
    fp.close();fp1.close();fp2.close();

    Path trainMethodPath = fS.getPath(trainPath.toString() + "/code.original");
    Path trainTokenisedPath = fS.getPath(trainPath.toString() + "/code.original_subtoken");
    Path trainSummaryPath = fS.getPath(trainPath.toString() + "/javadoc.original");
    fp = new FileWriter(trainMethodPath.toFile());
    fp1 = new FileWriter(trainTokenisedPath.toFile());
    fp2 = new FileWriter(trainSummaryPath.toFile());
    for (int cnt = 50000; cnt < 450000; cnt++)
    {
      fp.write(methods.get(randomOrder.get(cnt)) + "\n");
      fp1.write(tokenisedMethods.get(randomOrder.get(cnt)) + "\n");
      fp2.write(summaries.get(randomOrder.get(cnt)).trim() + "\n");
    }
    fp.close();fp1.close();fp2.close();

    Path devMethodPath = fS.getPath(devPath.toString() + "/code.original");
    Path devTokenisedPath = fS.getPath(devPath.toString() + "/code.original_subtoken");
    Path devSummaryPath = fS.getPath(devPath.toString() + "/javadoc.original");
    fp = new FileWriter(devMethodPath.toFile());
    fp1 = new FileWriter(devTokenisedPath.toFile());
    fp2 = new FileWriter(devSummaryPath.toFile());
    for (int cnt = 450000; cnt < 500000; cnt++)
    {
      fp.write(methods.get(randomOrder.get(cnt)) + "\n");
      fp1.write(tokenisedMethods.get(randomOrder.get(cnt)) + "\n");
      fp2.write(summaries.get(randomOrder.get(cnt)).trim() + "\n");
    }
    fp.close();fp1.close();fp2.close();
  }
}
