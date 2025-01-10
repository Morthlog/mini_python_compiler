import java.util.ArrayList;

public class SymbolTableEntryFunction {
    public DataType returnType;
    public ArrayList<FunctionParameter> parameters;
    public int numNonDefaultParameters;

    public SymbolTableEntryFunction() {
        returnType = DataType.UNKNOWN;
        parameters = new ArrayList<FunctionParameter>();
        numNonDefaultParameters = 0;
    }
}