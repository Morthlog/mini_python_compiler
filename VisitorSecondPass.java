import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;

public class VisitorSecondPass extends DepthFirstAdapter {
    public Hashtable<String, SymbolTableEntryVariable> variablesTable;
    public Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> functionsTable;


    /**
     * has TODO // value and type should NOT be set from first visitor
     *
     */
    public VisitorSecondPass(Hashtable<String, SymbolTableEntryVariable> varTable, Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> funcTable){
        variablesTable = varTable;
        functionsTable = funcTable;
        for (var obj: variablesTable.entrySet())
        {
            // value and type should NOT be set from first visitor.
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
        System.out.println("__________ outAPFunctionCall __________");

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
            for (int i = 0; i < argumentCount; i++) // add to the function that will be called the call's arguments
                func.getSecond().parameters.get(i).type = arguments.get(i).type;
            func.getFirst().apply(this);
            break;
        }

        if (!foundFunc)
        {
            System.out.printf("ERROR: There is no valid function call for %s with arguments %s\n", id, node.getPArgList());
        }
        else // part of 6 coverage
        {
            System.out.println(function.getSecond().returnType);
            System.out.println("Leaving outAPFunctionCall");
            setOut(node, function.getSecond().returnType);
        }
    }

    @Override
    public void inAPFunction(APFunction node) {
        System.out.println("__________ inAPFunction __________");
        System.out.println(getIn(node));
        ArrayList<FunctionParameter> parameters = ((ArrayList<FunctionParameter>)getIn(node));
        if (parameters == null) // came from def, not function call
        {
            // Branch produced from def should be ignored
            System.out.println("-------- FROM DEF --------");
            return;
        }
        else
            System.out.println("-------- FROM CALL --------");

        for (var obj: parameters)
        {
            System.out.println(obj.name);
            System.out.println(obj.type);
        }

        setIn(node.getPStatement(), parameters); // pass info to statement
    }

    @Override
    public void caseAPFunction(APFunction node)
    {
        inAPFunction(node);
        if(node.getId() != null)
        {
            node.getId().apply(this);
        }
        if (getIn(node) == null) // We are inside Def
                return;
        setIn(node, null); // reset own info on exit, to account for def
        {
            Object temp[] = node.getPArgument().toArray();
            for(int i = 0; i < temp.length; i++)
            {
                ((PPArgument) temp[i]).apply(this);
            }
        }
        if(node.getPStatement() != null)
        {
            node.getPStatement().apply(this);
        }
        outAPFunction(node);
    }

    @Override
    public void inAReturnPStatement(AReturnPStatement node) {
        System.out.println("__________ inAReturnPStatement __________");
        setIn(node.getPArithmetics(), getIn(node));
    }

    @Override
    public void inAFunctionPArithmetics(AFunctionPArithmetics node) {
        System.out.println("__________ inAFunctionPArithmetics __________");
        System.out.println(node);
        ArrayList<FunctionParameter> info = (ArrayList<FunctionParameter>) getIn(node.parent());
        setIn(node.getPFunctionCall(), info);
    }

    @Override
    public void inAMinusPArithmetics(AMinusPArithmetics node) {
        System.out.println("__________ inAMinusPArithmetics __________");
        System.out.println(node.getL());
        System.out.println(node.getL().getClass());
        System.out.println(node.getR());
        System.out.println(node.getR().getClass());
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAPlusPArithmetics(APlusPArithmetics node) {
        System.out.println("__________ inAPlusPArithmetics __________");
        System.out.println(node.getL());
        System.out.println(node.getL().getClass());
        System.out.println(node.getR());
        System.out.println(node.getR().getClass());
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAPrintPStatement(APrintPStatement node) {
        System.out.println("__________ inAPrintPStatement __________");
        System.out.println(node);
        System.out.print("");
    }

    @Override
    public void inAIdPArithmetics(AIdPArithmetics node) {
        System.out.println("__________ inAIdPArithmetics __________");
        System.out.println(node);

        DataType type = DataType.UNKNOWN;
        if (getIn(node) != null) // came from functions
        {
            for (var obj: ((ArrayList<FunctionParameter>)getIn(node)))
            {   // identifier is a function's argument
                if (obj.name.equals(node.getId().toString().trim())) {
                    type = obj.type;
                    break;
                }
            }
        }
        if (type == DataType.UNKNOWN)
        {
            SymbolTableEntryVariable entry = variablesTable.get(node.getId().toString().trim());
            if (entry != null)
                type = entry.type;
            else
                System.out.printf("ERROR at [%s,%s]: Variable %s is not defined in file\n", node.getId().getLine(), node.getId().getPos(), node.getId().toString().trim());
        }

        setOut(node, type);
        System.out.println(type);
    }

    @Override
    public void inAPArgList(APArgList node) {
        System.out.println("__________ inAPArgList __________");

        System.out.println(node);
        ArrayList<FunctionParameter> info = (ArrayList<FunctionParameter>) getIn(node.parent());
        if (node.getPArithmetics() != null)
            setIn(node.getPArithmetics(), info);

        for (var obj: node.getPCommaValue())
            setIn(((APCommaValue)obj).getPArithmetics(), info);
        System.out.print("");
    }

    @Override
    public void inAFunctionCallPArithmetics(AFunctionCallPArithmetics node) {
        System.out.println("__________ inAFunctionCallPArithmetics __________");
        System.out.println(node);
        SymbolTableEntryFunction info = (SymbolTableEntryFunction) getIn(node.parent());
        setIn(node, info);
    }

    /**
     * Set the return type to the appropriate function
     */
    @Override
    public void outAReturnPStatement(AReturnPStatement node) {
        System.out.println("__________ outAReturnPStatement __________");

        System.out.println(node.getPArithmetics());
        System.out.println(getOut(node.getPArithmetics()));
        ArrayList<Tuples<Node, SymbolTableEntryFunction>> name = functionsTable.get(((APFunction)node.parent()).getId().toString().trim());
        for (Tuples<Node, SymbolTableEntryFunction> obj: name)
        {
            if (obj.getFirst().equals(node.parent()))
            {
                DataType returnType = (DataType) getOut(node.getPArithmetics());
                System.out.println(returnType);
                obj.getSecond().returnType = returnType;
                break;
            }
        }
    }

    /**
     * Pass return type
     */
    @Override
    public void outAFunctionPArithmetics(AFunctionPArithmetics node) {
        System.out.println("__________ outAFunctionPArithmetics __________");
        setOut(node, getOut(node.getPFunctionCall()));
        System.out.println(node.getPFunctionCall());
    }

    @Override
    public void outAPlusPArithmetics(APlusPArithmetics node) {
        System.out.println("__________ outAPlusPArithmetics __________");

        DataType typeL = (DataType) getOut(node.getL());
        setOut(node.getL(), null);
        DataType typeR = (DataType) getOut(node.getR());
        setOut(node.getR(), null); // reset info

        setOut(node, DataType.INVALID);

        if (typeL == DataType.NONE || typeR == DataType.NONE)
        {
            System.out.println("ERROR: Can't add with None\n");
        }
        else if (typeL == DataType.UNKNOWN || typeR == DataType.UNKNOWN)
        {
            System.out.println("ERROR: Can't add with Unknown\n");
        }
        else if (typeL == DataType.INVALID|| typeR == DataType.INVALID)
        {
            System.out.println("ERROR: Can't add with Invalid\n");
        }
        else if (typeL != typeR)
        {
            System.out.printf("ERROR: Can't add %s of type %s with %s of type %s\n",
                    node.getL() ,typeL, node.getR(), typeR);
        }
        else
        {
            System.out.println("OK +"); //TODO remove Debug msg
            setOut(node, typeL);
        }
    }

    @Override
    public void outAMinusPArithmetics(AMinusPArithmetics node) {
        System.out.println("__________ outAMinusPArithmetics __________");

        DataType typeL = (DataType) getOut(node.getL());
        setOut(node.getL(), null);
        DataType typeR = (DataType) getOut(node.getR());
        setOut(node.getR(), null); // reset info

        setOut(node, DataType.INVALID);

        if (typeL != DataType.NUMBER || typeR != DataType.NUMBER)
        {
            System.out.printf("ERROR: Can't subtract %s of type %s with %s of type %s\n",
                    node.getL() ,typeL, node.getR(), typeR);
        }
        else
        {
            System.out.println("OK -"); //TODO remove Debug msg
            setOut(node, typeL);
        }
    }

    // Give out info that DataType is Number
    @Override
    public void outANumberPArithmetics(ANumberPArithmetics node)
    {
        setIn(node, null);
        setOut(node, DataType.NUMBER);
    }

    // Give out info that DataType is String
    @Override
    public void outAStringPArithmetics(AStringPArithmetics node)
    {
        setIn(node, null);
        setOut(node, DataType.STRING);
    }

    // Give out info that DataType is None
    @Override
    public void outANonePArithmetics(ANonePArithmetics node)
    {
        setIn(node, null);
        setOut(node, DataType.NONE);
    }
}
