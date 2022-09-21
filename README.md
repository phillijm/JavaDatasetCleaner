# JavaDatasetCleaner

This Maven Project prepares the Funcom Filtered dataset of Java method-comment pairs for neural source code summarisation using the NeuralCodeSum model.

NeuralCodeSum: [Paper](https://aclanthology.org/2020.acl-main.449.pdf).

Funcom: [Paper](https://aclanthology.org/N19-1394.pdf).

## Work so Far

This project currently compiles the data in a JSON form for further processing if desired, and in the form needed for the official implementation of NeuralCodeSum.

## Usage

Contained within this project is a graphical implementation of the preprocessing code (JavaDatasetPreprocessorGUI.java).  However, you can also import the project into your own Java class.

### Java Example

```Java
import uk.ac.lancs.scc.phd.jesse.*;
public class Clean
{
  public static void main(String[] args)
  {
    JavaDatasetPreprocessor jDP = new JavaDatasetPreprocessor(args[0]);
  }
}
```
