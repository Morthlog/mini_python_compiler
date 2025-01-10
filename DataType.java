public enum DataType {
    NONE,
    NUMBER,
    STRING,
    INVALID, // used when an expression PArithmetics has illegal operation eg addition of string with number
    // only operands of the same dataType are allowed this is a mark for the second pass to display error
    UNKNOWN,
};