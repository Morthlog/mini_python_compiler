public class SymbolTableEntryVariable {
    public String value;
    public DataType type;

    public SymbolTableEntryVariable() {
        value = "";
        type = DataType.UNKNOWN;
    }

    public SymbolTableEntryVariable(String value, DataType type) {
        this.value = value;
        this.type = type;
    }
}
