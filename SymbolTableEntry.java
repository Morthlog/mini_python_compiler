import java.util.ArrayList;

public class SymbolTableEntry {

    public static enum EntryType {
      VARIABLE,
      FUNCTION
    };

    public static enum DataType {
      NONE,
      NUMBER,
      STRING,
      INVALID // used when an expression PArithmetics has illegal operation eg addition of string with number
      // only operands of the same dataType are allowed this is a mark for the second pass to display error
    };

    public EntryType type;
    public String value; // only used for variables holds initial value
    // we assume scope is global
    
    // variable type for variable
    // return type for function
    public DataType dataType;

    // empty for variables
    // contains names of parameters for function
    public ArrayList<String> parameterNames;
    public ArrayList<String> defaultValues;
    public ArrayList<DataType> parameterTypes;

    public int numTotalParameters;
    public int numNonDefaultParameters;

    public SymbolTableEntry()
    {
      type = EntryType.VARIABLE;
      value = "None";
      dataType = DataType.NONE;
      parameterNames = new ArrayList<String>();
      defaultValues = new ArrayList<String>();
      parameterTypes = new ArrayList<DataType>();
      numTotalParameters = 0;
      numNonDefaultParameters = 0;
    }
}
