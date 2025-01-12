import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;

public class VisitorSecondPass extends DepthFirstAdapter {
    public Hashtable<String, SymbolTableEntryVariable> variablesTable;
    public Hashtable<String, ArrayList<SymbolTableEntryFunction>> functionsTable;


    private final String STRATEGY_VALUE = "VALUE";
    private final String STRATEGY_TYPE = "TYPE";

    public VisitorSecondPass(Hashtable<String, SymbolTableEntryVariable> varTable, Hashtable<String, ArrayList<SymbolTableEntryFunction>> funcTable){
        variablesTable = varTable;
        functionsTable = funcTable;
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

        List<DataType> parameterTypes = new ArrayList<>();
        if (node.getPArgList().isEmpty())
            argumentCount = 0;
        else // get argument count
        {
            APArgList parameters = (APArgList) node.getPArgList().get(0);
            PPArithmetics param = parameters.getPArithmetics();
            parameterTypes.add((DataType) getOut(param));

            System.out.println(parameters.getPArithmetics());
            System.out.println(parameterTypes.get(parameterTypes.size() - 1));
            if (parameterTypes.get(parameterTypes.size() - 1) == null)
            {

            }
            for (var obj: parameters.getPCommaValue())
            {
                param = ((APCommaValue) obj).getPArithmetics();
                parameterTypes.add((DataType) getOut(param));

                System.out.println(obj);
                System.out.println(parameterTypes.get(parameterTypes.size() - 1));
            }
            argumentCount = parameterTypes.size();
        }

        ArrayList<SymbolTableEntryFunction> functionList = functionsTable.get(id);
        boolean foundFunc = false;
        for (SymbolTableEntryFunction func: functionList)
        {   // find function with the correct amount of arguments

            System.out.printf("table has %s vs %s\n", func.numNonDefaultParameters, argumentCount);
            if (func.numNonDefaultParameters > argumentCount
                || func.parameters.size() < argumentCount)
                continue;

            foundFunc = true;
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

    /**
     *
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
