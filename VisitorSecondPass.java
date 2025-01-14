import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;

public class VisitorSecondPass extends DepthFirstAdapter {
    public Hashtable<String, SymbolTableEntryVariable> variablesTable;
    public Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> functionsTable;


    private final String STRATEGY_VALUE = "VALUE";
    private final String STRATEGY_TYPE = "TYPE";

    /**
     * has TODO // value and type should be set from first visitor
     */
    public VisitorSecondPass(Hashtable<String, SymbolTableEntryVariable> varTable, Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> funcTable){
        variablesTable = varTable;
        functionsTable = funcTable;
        for (var obj: variablesTable.entrySet())
        {
            // value and type should be set from first visitor.
            System.out.println(obj.getKey() + " " + obj.getValue().value + " " + obj.getValue().type);
            obj.getValue().value = null;
            obj.getValue().type = null;
        }
        System.out.println();
    }

    /**
     * Rule 2, 3
     * Checks whether there is a function that can be called with the given count of arguments
     */
    @Override
    public void outAPFunctionCall(APFunctionCall node){
        System.out.println("======== New func ========");

        String id = node.getId().toString().trim();
        System.out.println("ID: " + id);
        if (!functionsTable.containsKey(id))
        {
            System.out.printf("ERROR: Function named %s is not defined in file\n", id);
            return;
        }
        System.out.println("The function name exists");
        int argumentCount;
        System.out.println(node.getPArgList());

        List<SymbolTableEntryVariable> arguments = new ArrayList<>();
        if (node.getPArgList().isEmpty())
            argumentCount = 0;
        else // get argument count
        {
            APArgList parameters = (APArgList) node.getPArgList().get(0);
            PPArithmetics param = parameters.getPArithmetics();

            arguments.add(new SymbolTableEntryVariable (param.toString(), (DataType) getOut(param)));
            // TODO remove debug prints when done
            System.out.println(param);
            System.out.println(getOut(param));
//            if (parameterTypes.get(parameterTypes.size() - 1) == null)
//            {
//
//            }
            for (var obj: parameters.getPCommaValue())
            {
                param = ((APCommaValue) obj).getPArithmetics();
                arguments.add(new SymbolTableEntryVariable (param.toString(), (DataType) getOut(param)));
                // TODO remove debug prints when done
                System.out.println(obj);
                System.out.println(getOut(param));
            }
            argumentCount = arguments.size();
        }

        ArrayList<Tuples<Node, SymbolTableEntryFunction>> functionList = functionsTable.get(id);
        Tuples<Node, SymbolTableEntryFunction> function = new Tuples<>(null, null);
        boolean foundFunc = false;
        for (Tuples<Node, SymbolTableEntryFunction> func: functionList)
        {   // find function with the correct amount of arguments

            System.out.printf("table has %s vs %s\n", func.getSecond().numNonDefaultParameters, argumentCount);
            if (func.getSecond().numNonDefaultParameters > argumentCount
                || func.getSecond().parameters.size() < argumentCount)
                continue;

            foundFunc = true;
            function = func;
            System.out.println(node);
            setIn(func.getFirst(), func.getSecond().parameters);

            // TODO comments set value/type to variableTable
            for (int i = 0; i < func.getSecond().parameters.size(); i++) // add to the function that will be called the call's arguments
            {
                String varId = func.getSecond().parameters.get(i).name;
                if (i < argumentCount)
                {
                 //   variablesTable.get(varId).value = arguments.get(i).value;
                 //   variablesTable.get(varId).type = arguments.get(i).type;
                }
                else
                {
                 //   variablesTable.get(varId).value = func.getSecond().parameters.get(i).defaultValue;
                    // same with type, need default type
                }


                //function.getSecond().parameters.get(i++).type = parameter.type;
            }
            func.getFirst().apply(this);
            break;
        }

        if (!foundFunc)
        {
            System.out.printf("ERROR: There is no valid function call for %s with arguments %s\n", id, node.getPArgList());
        }
        else // check if we can make use of it, part of rules 4, 5, 6 coverage
        {


        }
    }

    @Override
    public void inAPFunction(APFunction node) {
        System.out.println(getIn(node));
        ArrayList<FunctionParameter> parameters = ((ArrayList<FunctionParameter>)getIn(node));
        if (parameters == null) // came from def, not function call
            return;
        // TODO commented till added on variablesTable
        for (var obj: parameters)
        {
            System.out.println(obj.name);
            //variablesTable.get(obj.name).type = obj.type;
        }
        System.out.println("From table");
        ArrayList<Tuples<Node, SymbolTableEntryFunction>> options = functionsTable.get(node.getId().toString().trim());
        for (Tuples<Node, SymbolTableEntryFunction> obj: options)
        {
            if (obj.getFirst().equals(node))
            {
                for (var item: obj.getSecond().parameters)
                {
                    System.out.println(item.type);
                }
            }
        }
        System.out.print("");
        setIn(node, null); // reset info on exit, in case of def
    }

    @Override
    public void inAPrintPStatement(APrintPStatement node) {
        System.out.println(node);
        System.out.print("");
    }

    @Override
    public void inAIdPArithmetics(AIdPArithmetics node) {
        System.out.println(node);
        System.out.print("");
    }

    /**
     * Below are for testing
     * @param node
     */
    @Override
    public void inAPArgList(APArgList node) {
        System.out.println(node);
        SymbolTableEntryFunction info = (SymbolTableEntryFunction) getIn(node.parent());
        setIn(node, info);
        System.out.print("");
    }

    @Override
    public void inAFunctionCallPArithmetics(AFunctionCallPArithmetics node) {
        System.out.println(node);
        SymbolTableEntryFunction info = (SymbolTableEntryFunction) getIn(node.parent());
        setIn(node, info);
        System.out.print("");
    }

    @Override
    public void inANumberPArithmetics(ANumberPArithmetics node) {
        System.out.println(node.getNumber());
        System.out.println(getIn(node.parent()));
        System.out.print("");
    }

    @Override
    public void inAStringPArithmetics(AStringPArithmetics node) {
        System.out.println(node.getString());
        System.out.print("");
    }

    /**
     * Automatically set the argument type
     *
     */
    @Override
    public void outAPlusPArithmetics(APlusPArithmetics node) {
        DataType typeL = (DataType) getOut(node.getL());
        DataType typeR = (DataType) getOut(node.getR());


        if (typeL == DataType.NONE || typeR == DataType.NONE)
        {
            System.out.println("ERROR: Can't add with None\n");
        }
        else if (typeL != typeR)
        {
            System.out.printf("ERROR: Can't add %s with %s\n", typeL, typeR);
        }
    }

    // Give out info that DataType is Number
    @Override
    public void outANumberPArithmetics(ANumberPArithmetics node)
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
        else
            setOut(node, DataType.NUMBER);
    }

    // Give out info that DataType is String
    @Override
    public void outAStringPArithmetics(AStringPArithmetics node)
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
        else
            setOut(node, DataType.STRING);
    }

    // Give out info that DataType is None
    @Override
    public void outANonePArithmetics(ANonePArithmetics node)
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
        else
            setOut(node, DataType.NONE);
    }
}
