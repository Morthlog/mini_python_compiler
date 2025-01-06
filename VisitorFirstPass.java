import minipython.analysis.DepthFirstAdapter;
import minipython.node.APFunction;
import minipython.node.APFunctionCall;
import minipython.node.APIdDot;
import minipython.node.AParsPArithmetics;
import minipython.node.APlusPArithmetics;
import minipython.node.AStringPArithmetics;
import minipython.node.PPArgList;
import minipython.node.PPArithmetics;
import minipython.node.PPAssignValue;
import minipython.node.PPCommaValue;
import minipython.node.AAsciiPArithmetics;
import minipython.node.AAssignArrayPStatement;
import minipython.node.AAssignOpPStatement;
import minipython.node.ADivPArithmetics;
import minipython.node.AExpPArithmetics;
import minipython.node.AFunctionCallPArithmetics;
import minipython.node.AFunctionPArithmetics;
import minipython.node.AIdPArithmetics;
import minipython.node.ALenPArithmetics;
import minipython.node.AListPArithmetics;
import minipython.node.AListcallPArithmetics;
import minipython.node.AMaxPArithmetics;
import minipython.node.AMinPArithmetics;
import minipython.node.AMinusPArithmetics;
import minipython.node.AModPArithmetics;
import minipython.node.AMultPArithmetics;
import minipython.node.ANonePArithmetics;
import minipython.node.ANumberPArithmetics;
import minipython.node.APArgList;
import minipython.node.APArgument;
import minipython.node.APAssignValue;
import minipython.node.APCommaAssignValue;
import minipython.node.APCommaValue;

import java.util.ArrayList;
import java.util.Hashtable;

public class VisitorFirstPass extends DepthFirstAdapter{
    public Hashtable<String, ArrayList<SymbolTableEntry>> symbolTable;

    private final String STRATEGY_VALUE = "VALUE";
    private final String STRATEGY_TYPE = "TYPE";

    public VisitorFirstPass()
    {
        symbolTable = new Hashtable<String, ArrayList<SymbolTableEntry>>();
    }

    
    @Override
    public void inAPFunction(APFunction node)
    {
        String functionID = node.getId().toString().trim();
        
        int numTotalParameters = 0;
        int numDefaultParameters = 0;
        
        SymbolTableEntry newEntry = new SymbolTableEntry();
        newEntry.type = SymbolTableEntry.EntryType.FUNCTION;

        if(node.getPArgument().size() != 0)
        {
            // non zero parameter count
            
            APArgument parameters = (APArgument)node.getPArgument().get(0);
    
            // first parameter
            String parameterID = parameters.getId().toString().trim();
            newEntry.parameterNames.add(parameterID);
            if(parameters.getPAssignValue().size() != 0)
            {
                // first parameter has default value
                numDefaultParameters++;

                /*
                 * STORE DEFAULT VALUE?
                 */

                APAssignValue assignValue = (APAssignValue)parameters.getPAssignValue().get(0);

                PPArithmetics value = assignValue.getPArithmetics();

                setIn(value, STRATEGY_VALUE);
                value.apply(this);

                newEntry.defaultValues.add((String)getOut(value));

                setIn(value, STRATEGY_TYPE);
                value.apply(this);

                newEntry.parameterTypes.add((SymbolTableEntry.DataType)getOut(value));
            }
            else
            {
                newEntry.defaultValues.add("None");
                newEntry.parameterTypes.add(SymbolTableEntry.DataType.NONE);
            }

            numTotalParameters++;
    
            for(int i = 0; i < parameters.getPCommaAssignValue().size(); ++i)
            {
                APCommaAssignValue parameter = (APCommaAssignValue)parameters.getPCommaAssignValue().get(i);

                parameterID = parameter.getId().toString().trim();
                newEntry.parameterNames.add(parameterID);

                if(parameter.getPAssignValue().size() != 0)
                {
                    // has default value
                    numDefaultParameters++;

                    /*
                     * STORE DEFAULT VALUE?
                     */

                    APAssignValue assignValue = (APAssignValue)parameter.getPAssignValue().get(0);

                    PPArithmetics value = assignValue.getPArithmetics();

                    setIn(value, STRATEGY_VALUE);
                    value.apply(this);

                    newEntry.defaultValues.add((String)getOut(value));

                    setIn(value, STRATEGY_TYPE);
                    value.apply(this);

                    newEntry.parameterTypes.add((SymbolTableEntry.DataType)getOut(value));
                }
                else
                {
                    newEntry.defaultValues.add("None");
                    newEntry.parameterTypes.add(SymbolTableEntry.DataType.NONE);
                }

                numTotalParameters++;
            }
        }

        int numNonDefaultParameters = numTotalParameters - numDefaultParameters;

        if(!symbolTable.containsKey(functionID))
        {
            symbolTable.put(functionID, new ArrayList<SymbolTableEntry>());
        }

        // we do not know what the return type is until we reach a return statement on the second pass, we assume None
        newEntry.dataType = SymbolTableEntry.DataType.NONE;
        newEntry.numTotalParameters = numTotalParameters;
        newEntry.numNonDefaultParameters = numNonDefaultParameters;

        ArrayList<SymbolTableEntry> entries = symbolTable.get(functionID);

        boolean isEntryValid = true;

        for(int i = 0; i < entries.size(); ++i)
        {
            SymbolTableEntry entry = entries.get(i);

            if(entry.type == SymbolTableEntry.EntryType.FUNCTION)
            {
                if(numTotalParameters == entry.numTotalParameters
                || (numNonDefaultParameters <= entry.numNonDefaultParameters && numTotalParameters > entry.numTotalParameters) ||
                (numNonDefaultParameters >= entry.numNonDefaultParameters && numTotalParameters < entry.numTotalParameters))
                {
                    // function entry is not valid
                    isEntryValid = false;
                    break;
                }
            }
        }

        if(isEntryValid)
        {
            symbolTable.get(functionID).add(newEntry);
        }
        else
        {
            System.out.println("[" + node.getId().getLine() + "," + node.getId().getPos() +"]:  Function " + functionID + " cannot be defined as it would cause ambiguity when called with other same name functions");
        }
    }

    @Override
    public void inAPlusPArithmetics(APlusPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setIn(node.getL(), STRATEGY_VALUE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_VALUE);
                node.getR().apply(this);

                setOut(node, (String)getOut(node.getL()) + "+" + (String)getOut(node.getR()));
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(node.getL(), STRATEGY_TYPE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_TYPE);
                node.getR().apply(this);

                if((SymbolTableEntry.DataType)getOut(node.getL()) != (SymbolTableEntry.DataType)getOut(node.getR()))
                {
                    setOut(node, SymbolTableEntry.DataType.INVALID);
                }
                else
                {
                    setOut(node, (SymbolTableEntry.DataType)getOut(node.getL()));
                }
            }
        }
    }

    @Override
    public void inAMinusPArithmetics(AMinusPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setIn(node.getL(), STRATEGY_VALUE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_VALUE);
                node.getR().apply(this);

                setOut(node, (String)getOut(node.getL()) + "-" + (String)getOut(node.getR()));
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(node.getL(), STRATEGY_TYPE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_TYPE);
                node.getR().apply(this);

                if((SymbolTableEntry.DataType)getOut(node.getL()) != (SymbolTableEntry.DataType)getOut(node.getR()))
                {
                    setOut(node, SymbolTableEntry.DataType.INVALID);
                }
                else
                {
                    setOut(node, (SymbolTableEntry.DataType)getOut(node.getL()));
                }
            }
        }
    }

    @Override
    public void inAMultPArithmetics(AMultPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setIn(node.getL(), STRATEGY_VALUE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_VALUE);
                node.getR().apply(this);

                setOut(node, (String)getOut(node.getL()) + "*" + (String)getOut(node.getR()));
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(node.getL(), STRATEGY_TYPE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_TYPE);
                node.getR().apply(this);

                if((SymbolTableEntry.DataType)getOut(node.getL()) != (SymbolTableEntry.DataType)getOut(node.getR()))
                {
                    setOut(node, SymbolTableEntry.DataType.INVALID);
                }
                else
                {
                    setOut(node, (SymbolTableEntry.DataType)getOut(node.getL()));
                }
            }
        }
    }

    @Override
    public void inADivPArithmetics(ADivPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setIn(node.getL(), STRATEGY_VALUE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_VALUE);
                node.getR().apply(this);

                setOut(node, (String)getOut(node.getL()) + "/" + (String)getOut(node.getR()));
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(node.getL(), STRATEGY_TYPE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_TYPE);
                node.getR().apply(this);

                if((SymbolTableEntry.DataType)getOut(node.getL()) != (SymbolTableEntry.DataType)getOut(node.getR()))
                {
                    setOut(node, SymbolTableEntry.DataType.INVALID);
                }
                else
                {
                    setOut(node, (SymbolTableEntry.DataType)getOut(node.getL()));
                }
            }
        }
    }

    @Override
    public void inAModPArithmetics(AModPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setIn(node.getL(), STRATEGY_VALUE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_VALUE);
                node.getR().apply(this);

                setOut(node, (String)getOut(node.getL()) + "%" + (String)getOut(node.getR()));
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(node.getL(), STRATEGY_TYPE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_TYPE);
                node.getR().apply(this);

                if((SymbolTableEntry.DataType)getOut(node.getL()) != (SymbolTableEntry.DataType)getOut(node.getR()))
                {
                    setOut(node, SymbolTableEntry.DataType.INVALID);
                }
                else
                {
                    setOut(node, (SymbolTableEntry.DataType)getOut(node.getL()));
                }
            }
        }
    }

    @Override
    public void inAExpPArithmetics(AExpPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setIn(node.getL(), STRATEGY_VALUE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_VALUE);
                node.getR().apply(this);

                setOut(node, (String)getOut(node.getL()) + "**" + (String)getOut(node.getR()));
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(node.getL(), STRATEGY_TYPE);
                node.getL().apply(this);
                setIn(node.getR(), STRATEGY_TYPE);
                node.getR().apply(this);

                if((SymbolTableEntry.DataType)getOut(node.getL()) != (SymbolTableEntry.DataType)getOut(node.getR()))
                {
                    setOut(node, SymbolTableEntry.DataType.INVALID);
                }
                else
                {
                    setOut(node, (SymbolTableEntry.DataType)getOut(node.getL()));
                }
            }
        }
    }

    @Override
    public void inAListcallPArithmetics(AListcallPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            PPArithmetics arithmetics = node.getPArithmetics();

            setIn(arithmetics, STRATEGY_VALUE);
            arithmetics.apply(this);

            String variableId = node.getId().toString().trim() + "[" + (String)getOut(arithmetics) + "]";

            if(strategy.equals(STRATEGY_VALUE))
            {
                setOut(node, variableId);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                SymbolTableEntry.DataType type = SymbolTableEntry.DataType.INVALID; 

                if(symbolTable.containsKey(variableId))
                {
                    for(SymbolTableEntry e : symbolTable.get(variableId))
                    {
                        if(e.type == SymbolTableEntry.EntryType.VARIABLE)
                        {
                            type = e.dataType;
                            break;
                        }
                    }
                }

                setOut(node, type);
            }
        }
    }

    @Override
    public void inALenPArithmetics(ALenPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {   
            if(strategy.equals(STRATEGY_VALUE))
            {
                PPArithmetics arithmetics = node.getPArithmetics();

                setIn(arithmetics, STRATEGY_VALUE);
                arithmetics.apply(this);

                String value = "len(" + (String)getOut(arithmetics) + ")";

                setOut(node, value);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setOut(node, SymbolTableEntry.DataType.NUMBER);
            }
        }
    }

    @Override
    public void inAAsciiPArithmetics(AAsciiPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setOut(node, "ascii(" + node.getId().toString().trim() + ")");
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setOut(node, SymbolTableEntry.DataType.STRING);
            }
        }
    }

    @Override
    public void inAMaxPArithmetics(AMaxPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                String value = "max(";

                PPArithmetics arithmetics = node.getPArithmetics();
                setIn(arithmetics, STRATEGY_VALUE);
                arithmetics.apply(this);

                value = value + (String)getOut(arithmetics);

                for(int i = 0; i < node.getPCommaValue().size(); ++i)
                {
                    APCommaValue parameter = (APCommaValue)node.getPCommaValue().get(i);

                    arithmetics = parameter.getPArithmetics();
                    setIn(arithmetics, STRATEGY_VALUE);
                    arithmetics.apply(this);

                    value = value + ", " + (String)getOut(arithmetics);
                }

                value = value + ")";

                setOut(node, value);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                PPArithmetics arithmetics = node.getPArithmetics();
                setIn(arithmetics, STRATEGY_TYPE);
                arithmetics.apply(this);

                setOut(node, (SymbolTableEntry.DataType)getOut(arithmetics));
            }
        }
    }

    @Override
    public void inAMinPArithmetics(AMinPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                String value = "min(";

                PPArithmetics arithmetics = node.getPArithmetics();
                setIn(arithmetics, STRATEGY_VALUE);
                arithmetics.apply(this);

                value = value + (String)getOut(arithmetics);

                for(int i = 0; i < node.getPCommaValue().size(); ++i)
                {
                    APCommaValue parameter = (APCommaValue)node.getPCommaValue().get(i);

                    arithmetics = parameter.getPArithmetics();
                    setIn(arithmetics, STRATEGY_VALUE);
                    arithmetics.apply(this);

                    value = value + ", " + (String)getOut(arithmetics);
                }

                value = value + ")";

                setOut(node, value);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                PPArithmetics arithmetics = node.getPArithmetics();
                setIn(arithmetics, STRATEGY_TYPE);
                arithmetics.apply(this);

                setOut(node, (SymbolTableEntry.DataType)getOut(arithmetics));
            }
        }
    }

    @Override
    public void inAListPArithmetics(AListPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            APArgList arguments = (APArgList)node.getPArgList();
            
            if(strategy.equals(STRATEGY_VALUE))
            {
                String value = "[";


                PPArithmetics arithmetics = arguments.getPArithmetics();
                setIn(arithmetics, STRATEGY_VALUE);
                arithmetics.apply(this);

                value = value + (String)getOut(arithmetics);

                for(int i = 0; i < arguments.getPCommaValue().size(); ++i)
                {
                    APCommaValue parameter = (APCommaValue)arguments.getPCommaValue().get(i);

                    arithmetics = parameter.getPArithmetics();
                    setIn(arithmetics, STRATEGY_VALUE);
                    arithmetics.apply(this);

                    value = value + ", " + (String)getOut(arithmetics);
                }

                value = value + "]";

                setOut(node, value);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                PPArithmetics arithmetics = arguments.getPArithmetics();
                setIn(arithmetics, STRATEGY_TYPE);
                arithmetics.apply(this);

                setOut(node, (SymbolTableEntry.DataType)getOut(arithmetics));
            }
        }
    }

    @Override
    public void inAIdPArithmetics(AIdPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setOut(node, node.getId().toString().trim());
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setOut(node, SymbolTableEntry.DataType.STRING);
            }
        }
    }

    @Override
    public void inAFunctionPArithmetics(AFunctionPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            APFunctionCall functionCall = (APFunctionCall)node.getPFunctionCall();

            if(strategy.equals(STRATEGY_VALUE))
            {
                String value = functionCall.getId().toString().trim() + "(";

                if(functionCall.getPArgList().size() != 0)
                {
                    // there are parameters
                    APArgList parameters = (APArgList)functionCall.getPArgList().get(0);

                    // first parameter
                    PPArithmetics arithmetics = parameters.getPArithmetics();
                    setIn(arithmetics, STRATEGY_VALUE);
                    arithmetics.apply(this);

                    value = value + (String)getOut(arithmetics);
                    // fun(var1

                    // rest
                    for(int i = 0; i < parameters.getPCommaValue().size(); ++i)
                    {
                        APCommaValue parameter = (APCommaValue)parameters.getPCommaValue().get(i);

                        arithmetics = parameter.getPArithmetics();
                        setIn(arithmetics, STRATEGY_VALUE);
                        arithmetics.apply(this);

                        value = value + ", " + (String)getOut(arithmetics);
                    }

                    
                }
                value = value + ")";
                
                setOut(node, value);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                SymbolTableEntry.DataType returnType = SymbolTableEntry.DataType.INVALID;
                
                if(symbolTable.containsKey(functionCall.getId().toString().trim()))
                {
                    ArrayList<SymbolTableEntry> entries = symbolTable.get(functionCall.getId().toString().trim());

                    int numFunctionCallParameters = 0;
                    if(functionCall.getPArgList().size() != 0)
                    {
                        numFunctionCallParameters++;
                        // there is at least 1 function call parameter

                        APArgList parameters = (APArgList)functionCall.getPArgList().get(0);

                        numFunctionCallParameters += parameters.getPCommaValue().size();
                    }

                    // search all entries of type function and look for a function with total number of parameters >= call function num parameters
                    for(SymbolTableEntry e : entries)
                    {
                        if(e.type == SymbolTableEntry.EntryType.FUNCTION && 
                        (numFunctionCallParameters <= e.numTotalParameters && numFunctionCallParameters >= e.numNonDefaultParameters))
                        {
                            // found function return its type
                            returnType = e.dataType;
                            break;
                        }
                    }

                }

                setOut(node, returnType);
            }
        }
    }

    @Override
    public void inAParsPArithmetics(AParsPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            PPArithmetics arithmetics = node.getPArithmetics();
            
            if(strategy.equals(STRATEGY_VALUE))
            {
                // (parithmetics)
                String value = "(";

                setIn(arithmetics, STRATEGY_VALUE);
                arithmetics.apply(this);

                value = value + (String)getOut(arithmetics);

                value = value + ")";

                setOut(node, value);
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setIn(arithmetics, STRATEGY_TYPE);
                arithmetics.apply(this);

                setOut(node, (SymbolTableEntry.DataType)getOut(arithmetics));
            }
        }
    }

    @Override
    public void inAFunctionCallPArithmetics(AFunctionCallPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            APFunctionCall functionCall = (APFunctionCall)node.getPFunctionCall();
            
            if(strategy.equals(STRATEGY_VALUE))
            {
                APIdDot varId = (APIdDot)node.getPIdDot();
                String value = varId.getId().toString().trim() + ".";

                value = value + functionCall.getId().toString().trim() + "(";

                if(functionCall.getPArgList().size() != 0)
                {
                    // there are parameters
                    APArgList parameters = (APArgList)functionCall.getPArgList().get(0);

                    // first parameter
                    PPArithmetics arithmetics = parameters.getPArithmetics();
                    setIn(arithmetics, STRATEGY_VALUE);
                    arithmetics.apply(this);

                    value = value + (String)getOut(arithmetics);
                    // fun(var1

                    // rest
                    for(int i = 0; i < parameters.getPCommaValue().size(); ++i)
                    {
                        APCommaValue parameter = (APCommaValue)parameters.getPCommaValue().get(i);

                        arithmetics = parameter.getPArithmetics();
                        setIn(arithmetics, STRATEGY_VALUE);
                        arithmetics.apply(this);

                        value = value + ", " + (String)getOut(arithmetics);
                    }

                    
                }
                value = value + ")";
                
                setOut(node, value);

            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                SymbolTableEntry.DataType returnType = SymbolTableEntry.DataType.INVALID;
                
                if(symbolTable.containsKey(functionCall.getId().toString().trim()))
                {
                    ArrayList<SymbolTableEntry> entries = symbolTable.get(functionCall.getId().toString().trim());

                    int numFunctionCallParameters = 0;
                    if(functionCall.getPArgList().size() != 0)
                    {
                        numFunctionCallParameters++;
                        // there is at least 1 function call parameter

                        APArgList parameters = (APArgList)functionCall.getPArgList().get(0);

                        numFunctionCallParameters += parameters.getPCommaValue().size();
                    }

                    // search all entries of type function and look for a function with total number of parameters >= call function num parameters
                    for(SymbolTableEntry e : entries)
                    {
                        if(e.type == SymbolTableEntry.EntryType.FUNCTION && 
                        (numFunctionCallParameters <= e.numTotalParameters && numFunctionCallParameters >= e.numNonDefaultParameters))
                        {
                            // found function return its type
                            returnType = e.dataType;
                            break;
                        }
                    }

                }

                setOut(node, returnType);
            }
        }
    }

    @Override
    public void inANumberPArithmetics(ANumberPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setOut(node, node.getNumber().toString().trim());
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setOut(node, SymbolTableEntry.DataType.NUMBER);
            }
        }
    }

    @Override
    public void inAStringPArithmetics(AStringPArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setOut(node, node.getString().toString().trim());
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setOut(node, SymbolTableEntry.DataType.STRING);
            }
        }
    }

    @Override
    public void inANonePArithmetics(ANonePArithmetics node)
    {
        String strategy = (String)getIn(node);

        if(strategy != null)
        {
            if(strategy.equals(STRATEGY_VALUE))
            {
                setOut(node, node.getNone().toString().trim());
            }
            else if(strategy.equals(STRATEGY_TYPE))
            {
                setOut(node, SymbolTableEntry.DataType.NONE);
            }
        }
    }

    @Override
    public void inAAssignArrayPStatement(AAssignArrayPStatement node)
    {
        // Name is id[...]
        String id = node.getId().toString().trim();

        PPArithmetics arithmetics = node.getBracketsExpression();

        setIn(arithmetics, STRATEGY_VALUE);
        arithmetics.apply(this);

        String variableID = id + "[" + (String)getOut(arithmetics) + "]";

        if(!symbolTable.containsKey(variableID))
        {
            symbolTable.put(variableID, new ArrayList<SymbolTableEntry>());
        }

        boolean isAlreadyDefined = false;
        int t = 0;

        ArrayList<SymbolTableEntry> entries = symbolTable.get(variableID);
        
        for(int i = 0; i < entries.size(); ++i)
        {
            if(entries.get(i).type == SymbolTableEntry.EntryType.VARIABLE)
            {
                isAlreadyDefined = true;
                t = i;
                break;
            }
        }

        arithmetics = node.getAssignExpression();

        setIn(arithmetics, STRATEGY_VALUE);
        arithmetics.apply(this);

        String variableInitialValue = (String)getOut(arithmetics);

        setIn(arithmetics, STRATEGY_TYPE);
        arithmetics.apply(this);
        
        SymbolTableEntry.DataType variableType = (SymbolTableEntry.DataType)getOut(arithmetics);

        if(isAlreadyDefined)
        {
            SymbolTableEntry e = entries.get(t);

            e.value = variableInitialValue;
            e.dataType = variableType;
        }
        else
        {
            SymbolTableEntry e = new SymbolTableEntry();
            e.type = SymbolTableEntry.EntryType.VARIABLE;
            e.value = variableInitialValue;
            e.dataType = variableType;

            symbolTable.get(variableID).add(e);
        }
    }

    @Override
    public void inAAssignOpPStatement(AAssignOpPStatement node)
    {
        String variableID = node.getId().toString().trim();
        
        if(!symbolTable.containsKey(variableID))
        {
            symbolTable.put(variableID, new ArrayList<SymbolTableEntry>());
        }

        boolean isAlreadyDefined = false;
        int t = 0;

        ArrayList<SymbolTableEntry> entries = symbolTable.get(variableID);

        for(int i = 0; i < entries.size(); ++i)
        {
            if(entries.get(i).type == SymbolTableEntry.EntryType.VARIABLE)
            {
                isAlreadyDefined = true;
                t = i;
                break;
            }
        }

        PPArithmetics arithmetics = node.getPArithmetics();

        setIn(arithmetics, STRATEGY_VALUE);
        arithmetics.apply(this);

        String variableInitialValue = (String)getOut(arithmetics);

        setIn(arithmetics, STRATEGY_TYPE);
        arithmetics.apply(this);

        SymbolTableEntry.DataType variableType = (SymbolTableEntry.DataType)getOut(arithmetics);

        if(isAlreadyDefined)
        {
            SymbolTableEntry e = entries.get(t);
            e.value = variableInitialValue;
            e.dataType = variableType;
        }
        else
        {
            SymbolTableEntry e = new SymbolTableEntry();
            e.type = SymbolTableEntry.EntryType.VARIABLE;
            e.value = variableInitialValue;
            e.dataType = variableType;

            symbolTable.get(variableID).add(e);
        }
    }
}