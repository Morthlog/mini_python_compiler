import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;

import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

public class VisitorFirstPass extends DepthFirstAdapter{
    public Hashtable<String, SymbolTableEntryVariable> variablesTable;
    public Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> functionsTable;
    public Hashtable<Node, SymbolTableEntryFunction> nodeToSymbol;
    private final String STRATEGY_VALUE = "VALUE";
    private final String STRATEGY_TYPE = "TYPE";

    public VisitorFirstPass()
    {
        variablesTable = new Hashtable<String, SymbolTableEntryVariable>();
        functionsTable = new Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>>();
        nodeToSymbol = new Hashtable<>();
    }

    
    @Override
    public void inAPFunction(APFunction node)
    {
        String functionID = node.getId().toString().trim();
        
        int numTotalParameters = 0;
        int numDefaultParameters = 0;
        
        SymbolTableEntryFunction newEntry = new SymbolTableEntryFunction();

        if(node.getPArgument().size() != 0)
        {
            // non zero parameter count
            
            APArgument parameters = (APArgument)node.getPArgument().get(0);
    
            // first parameter
            String parameterID = parameters.getId().toString().trim();
            newEntry.parameters.add(new FunctionParameter());
            newEntry.parameters.get(newEntry.parameters.size() - 1).name = parameterID;
            
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
                
                newEntry.parameters.get(newEntry.parameters.size() - 1).defaultValue = (String)getOut(value);

                setIn(value, STRATEGY_TYPE);
                value.apply(this);

                newEntry.parameters.get(newEntry.parameters.size() - 1).type = (DataType)getOut(value);
            }
            else
            {
                newEntry.parameters.get(newEntry.parameters.size() - 1).defaultValue = "None";
                newEntry.parameters.get(newEntry.parameters.size() - 1).type = DataType.NONE;
            }

            numTotalParameters++;
    
            for(int i = 0; i < parameters.getPCommaAssignValue().size(); ++i)
            {
                APCommaAssignValue parameter = (APCommaAssignValue)parameters.getPCommaAssignValue().get(i);

                parameterID = parameter.getId().toString().trim();

                newEntry.parameters.add(new FunctionParameter());
                newEntry.parameters.get(newEntry.parameters.size() - 1).name = parameterID;


                /*
                  Make sure the default values are on the right side of the function
                  func (x, y=2, z) is illegal
                  func (x, y=2, z=2) is legal
                  func (x, y, z=2) is legal
                 */

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

                    newEntry.parameters.get(newEntry.parameters.size() - 1).defaultValue = (String)getOut(value);

                    setIn(value, STRATEGY_TYPE);
                    value.apply(this);

                    newEntry.parameters.get(newEntry.parameters.size() - 1).type = (DataType)getOut(value);
                }
                else
                {
                    newEntry.parameters.get(newEntry.parameters.size() - 1).defaultValue = "None";
                    newEntry.parameters.get(newEntry.parameters.size() - 1).type = DataType.NONE;
                }

                numTotalParameters++;
            }
        }

        int numNonDefaultParameters = numTotalParameters - numDefaultParameters;

        if(!functionsTable.containsKey(functionID))
        {
            functionsTable.put(functionID, new ArrayList<Tuples<Node, SymbolTableEntryFunction>>());
        }

        // we do not know what the return type is until we reach a return statement on the second pass, we assume None
        newEntry.returnType = DataType.UNKNOWN;
        newEntry.numNonDefaultParameters = numNonDefaultParameters;

        ArrayList<Tuples<Node, SymbolTableEntryFunction>> entries = functionsTable.get(functionID);

        boolean isEntryValid = true;

        for(int i = 0; i < entries.size(); ++i)
        {
            SymbolTableEntryFunction entry = entries.get(i).getSecond();

            if(numTotalParameters == entry.parameters.size()
            || (numNonDefaultParameters <= entry.numNonDefaultParameters && numTotalParameters > entry.parameters.size()) ||
            (numNonDefaultParameters >= entry.numNonDefaultParameters && numTotalParameters < entry.parameters.size()))
            {
                // function entry is not valid
                isEntryValid = false;
                break;
            }
        }

        if(isEntryValid)
        {
            functionsTable.get(functionID).add(new Tuples<>(node, newEntry));
            nodeToSymbol.put(node, newEntry);
        }
        else
        {
            System.out.println("[" + node.getId().getLine() + "," + node.getId().getPos() +"]:  Function " + functionID + " cannot be defined as it would cause ambiguity when called with other same name functions");
        }
    }

    @Override
    public void outAPlusPArithmetics(APlusPArithmetics node)
    {
        setIn(node.getL(), STRATEGY_TYPE);
        node.getL().apply(this);
        setIn(node.getR(), STRATEGY_TYPE);
        node.getR().apply(this);

        if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
        {
            setOut(node, DataType.INVALID);
        }
        else
        {
            setOut(node, (DataType)getOut(node.getL()));
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

                if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
                {
                    setOut(node, DataType.INVALID);
                }
                else
                {
                    setOut(node, (DataType)getOut(node.getL()));
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

                if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
                {
                    setOut(node, DataType.INVALID);
                }
                else
                {
                    setOut(node, (DataType)getOut(node.getL()));
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

                if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
                {
                    setOut(node, DataType.INVALID);
                }
                else
                {
                    setOut(node, (DataType)getOut(node.getL()));
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

                if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
                {
                    setOut(node, DataType.INVALID);
                }
                else
                {
                    setOut(node, (DataType)getOut(node.getL()));
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

                if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
                {
                    setOut(node, DataType.INVALID);
                }
                else
                {
                    setOut(node, (DataType)getOut(node.getL()));
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

                if((DataType)getOut(node.getL()) != (DataType)getOut(node.getR()))
                {
                    setOut(node, DataType.INVALID);
                }
                else
                {
                    setOut(node, (DataType)getOut(node.getL()));
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
                DataType type = DataType.INVALID; 

                if(variablesTable.containsKey(variableId))
                {
                    type = variablesTable.get(variableId).type;
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
                setOut(node, DataType.NUMBER);
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
                setOut(node, DataType.STRING);
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

                setOut(node, (DataType)getOut(arithmetics));
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

                setOut(node, (DataType)getOut(arithmetics));
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

                setOut(node, (DataType)getOut(arithmetics));
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
                setOut(node, DataType.STRING);
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
                DataType returnType = DataType.INVALID;
                
                if(functionsTable.containsKey(functionCall.getId().toString().trim()))
                {
                    ArrayList<Tuples<Node, SymbolTableEntryFunction>> entries = functionsTable.get(functionCall.getId().toString().trim());

                    int numFunctionCallParameters = 0;
                    if(functionCall.getPArgList().size() != 0)
                    {
                        numFunctionCallParameters++;
                        // there is at least 1 function call parameter

                        APArgList parameters = (APArgList)functionCall.getPArgList().get(0);

                        numFunctionCallParameters += parameters.getPCommaValue().size();
                    }

                    // search all entries of type function and look for a function with total number of parameters >= call function num parameters
                    for(Tuples<Node, SymbolTableEntryFunction> e : entries)
                    {
                        if( 
                        (numFunctionCallParameters <= e.getSecond().parameters.size() && numFunctionCallParameters >= e.getSecond().numNonDefaultParameters))
                        {
                            // found function return its type
                            returnType = e.getSecond().returnType;
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

                setOut(node, (DataType)getOut(arithmetics));
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
                DataType returnType = DataType.INVALID;
                
                if(functionsTable.containsKey(functionCall.getId().toString().trim()))
                {
                    ArrayList<Tuples<Node, SymbolTableEntryFunction>> entries = functionsTable.get(functionCall.getId().toString().trim());

                    int numFunctionCallParameters = 0;
                    if(functionCall.getPArgList().size() != 0)
                    {
                        numFunctionCallParameters++;
                        // there is at least 1 function call parameter

                        APArgList parameters = (APArgList)functionCall.getPArgList().get(0);

                        numFunctionCallParameters += parameters.getPCommaValue().size();
                    }

                    // search all entries of type function and look for a function with total number of parameters >= call function num parameters
                    for(Tuples<Node, SymbolTableEntryFunction> e : entries)
                    {
                        if(
                        (numFunctionCallParameters <= e.getSecond().parameters.size() && numFunctionCallParameters >= e.getSecond().numNonDefaultParameters))
                        {
                            // found function return its type
                            returnType = e.getSecond().returnType;
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
                setOut(node, DataType.NUMBER);
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
                setOut(node, DataType.STRING);
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
                setOut(node, DataType.NONE);
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

        if(!variablesTable.containsKey(variableID))
        {
            SymbolTableEntryVariable e = new SymbolTableEntryVariable();
            variablesTable.put(variableID, e);
        }
    }

    @Override
    public void inAAssignOpPStatement(AAssignOpPStatement node)
    {
        String variableID = node.getId().toString().trim();

        if(!variablesTable.containsKey(variableID))
        {
            SymbolTableEntryVariable e = new SymbolTableEntryVariable();
            variablesTable.put(variableID, e);
        }
    }
}