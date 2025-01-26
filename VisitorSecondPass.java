import java.util.ArrayList;
import java.util.Hashtable;
import java.util.List;

import minipython.analysis.DepthFirstAdapter;
import minipython.node.*;

public class VisitorSecondPass extends DepthFirstAdapter {
    public Hashtable<String, SymbolTableEntryVariable> variablesTable;
    public Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> functionsTable;

    public VisitorSecondPass(Hashtable<String, SymbolTableEntryVariable> varTable, Hashtable<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> funcTable){
        variablesTable = varTable;
        functionsTable = funcTable;
    }

    /**
     * Rule 2, 3
     * Checks whether there is a function that can be called with the given count of arguments
     */
    @Override
    public void outAPFunctionCall(APFunctionCall node){

        String id = node.getId().toString().trim();
        if (!functionsTable.containsKey(id))
        {
            System.out.printf("ERROR at [%s,%s]: Function named %s is not defined in file\n", node.getId().getLine(), node.getId().getPos(), id);
            return;
        }
        int argumentCount;

        List<SymbolTableEntryVariable> arguments = new ArrayList<>();
        if (node.getPArgList().isEmpty())
            argumentCount = 0;
        else // get argument count
        {
            APArgList parameters = (APArgList) node.getPArgList().get(0);
            PPArithmetics param = parameters.getPArithmetics();

            arguments.add(new SymbolTableEntryVariable (param.toString(), (DataType) getOut(param)));

            for (var obj: parameters.getPCommaValue())
            {
                param = ((APCommaValue) obj).getPArithmetics();
                arguments.add(new SymbolTableEntryVariable (param.toString(), (DataType) getOut(param)));
            }
            argumentCount = arguments.size();
        }

        ArrayList<Tuples<Node, SymbolTableEntryFunction>> functionList = functionsTable.get(id);
        Tuples<Node, SymbolTableEntryFunction> function = new Tuples<>(null, null);
        boolean foundFunc = false;
        for (Tuples<Node, SymbolTableEntryFunction> func: functionList)
        {   // find function with the correct amount of arguments
            if (func.getSecond().numNonDefaultParameters > argumentCount
                || func.getSecond().parameters.size() < argumentCount)
                continue;

            foundFunc = true;
            function = func;
            setIn(func.getFirst(), func.getSecond().parameters);

            for (int i = 0; i < argumentCount; i++) // add to the function that will be called the call's arguments
                func.getSecond().parameters.get(i).type = arguments.get(i).type;
            func.getFirst().apply(this);
            break;
        }

        if (!foundFunc)
        {
            System.out.printf("ERROR at [%s,%s]: There is no valid function call for %s with arguments %s\n", node.getId().getLine(), node.getId().getPos(), id, node.getPArgList());
        }
        else // part of 6 coverage
        {
            setOut(node, function.getSecond().returnType);
        }
    }

    @Override
    public void inAPFunction(APFunction node) {
        ArrayList<FunctionParameter> parameters = ((ArrayList<FunctionParameter>)getIn(node));
        if (parameters == null) // came from def, not function call
            return;

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
        setIn(node.getPArithmetics(), getIn(node));
    }

    @Override
    public void inAFunctionPArithmetics(AFunctionPArithmetics node) {
        ArrayList<FunctionParameter> info = (ArrayList<FunctionParameter>) getIn(node.parent());
        setIn(node.getPFunctionCall(), info);
    }

    @Override
    public void inAMinusPArithmetics(AMinusPArithmetics node) {
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAPlusPArithmetics(APlusPArithmetics node) {
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAMultPArithmetics(AMultPArithmetics node) {
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inADivPArithmetics(ADivPArithmetics node) {
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAModPArithmetics(AModPArithmetics node) {
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAExpPArithmetics(AExpPArithmetics node) {
        setIn(node.getL(), getIn(node));
        setIn(node.getR(), getIn(node));
    }

    @Override
    public void inAMaxPArithmetics(AMaxPArithmetics node) {
        setIn(node.getPArithmetics(), getIn(node));
        for (var obj: node.getPCommaValue())
            setIn(((APCommaValue)obj).getPArithmetics(), getIn(node));

    }

    @Override
    public void inAMinPArithmetics(AMinPArithmetics node) {
        setIn(node.getPArithmetics(), getIn(node));
        for (var obj: node.getPCommaValue())
            setIn(((APCommaValue)obj).getPArithmetics(), getIn(node));
    }

    @Override
    public void inAPrintPStatement(APrintPStatement node) {
        setIn(node.getPArithmetics(), getIn(node));
        for (var obj: node.getPCommaValue())
            setIn(((APCommaValue)obj).getPArithmetics(), getIn(node));
    }

    @Override
    public void inAIdPArithmetics(AIdPArithmetics node) {

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
    }

    @Override
    public void inAPArgList(APArgList node) {
        ArrayList<FunctionParameter> info = (ArrayList<FunctionParameter>) getIn(node.parent());
        if (node.getPArithmetics() != null)
            setIn(node.getPArithmetics(), info);

        for (var obj: node.getPCommaValue())
            setIn(((APCommaValue)obj).getPArithmetics(), info);
    }

    @Override
    public void inAFunctionCallPArithmetics(AFunctionCallPArithmetics node) {
        SymbolTableEntryFunction info = (SymbolTableEntryFunction) getIn(node.parent());
        setIn(node, info);
    }

    @Override
    public void inALenPArithmetics(ALenPArithmetics node) {
        setIn(node.getPArithmetics(), getIn(node));
    }

    @Override
    public void inAAssignOpPStatement(AAssignOpPStatement node) {
        String variableID = node.getId().toString().trim();
        if (!variablesTable.containsKey(variableID)) {
            System.out.printf("ERROR: Variable %s is not defined.\n", variableID);
            return;
        }

        setIn(node.getPArithmetics(), getIn(node));
    }

    /**
     * Set the return type to the appropriate function
     */
    @Override
    public void outAReturnPStatement(AReturnPStatement node) {
        ArrayList<Tuples<Node, SymbolTableEntryFunction>> name = functionsTable.get(((APFunction)node.parent()).getId().toString().trim());
        for (Tuples<Node, SymbolTableEntryFunction> obj: name)
        {
            if (obj.getFirst().equals(node.parent()))
            {
                DataType returnType = (DataType) getOut(node.getPArithmetics());
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
        setOut(node, getOut(node.getPFunctionCall()));
    }

    @Override
    public void outAPlusPArithmetics(APlusPArithmetics node) {

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
            setOut(node, typeL);
        }
    }

    @Override
    public void outAMinusPArithmetics(AMinusPArithmetics node) {
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
            setOut(node, typeL);
        }
    }



    @Override
    public void outADivPArithmetics(ADivPArithmetics node) {
        DataType typeL = (DataType) getOut(node.getL());
        setOut(node.getL(), null);

        DataType typeR = (DataType) getOut(node.getR());
        setOut(node.getR(), null);

        setOut(node, DataType.INVALID);

        if (typeL == DataType.NUMBER && typeR == DataType.NUMBER) {
            setOut(node, DataType.NUMBER);
        } else {
            System.out.printf("ERROR: Invalid division between %s and %s\n", typeL, typeR);
        }
    }

    @Override
    public void outAMultPArithmetics(AMultPArithmetics node) {
        DataType typeL = (DataType)getOut(node.getL());
        setOut(node.getL(), null);

        DataType typeR = (DataType)getOut(node.getR());
        setOut(node.getR(), null);

        setOut(node, DataType.INVALID);

        if (typeL == DataType.NUMBER && typeR == DataType.NUMBER) {
            setOut(node, DataType.NUMBER);
        } else {
            System.out.printf("ERROR: Invalid multiplication between %s and %s\n", typeL, typeR);
        }
    }

    @Override
    public void outAModPArithmetics(AModPArithmetics node) {
        DataType typeL = (DataType)getOut(node.getL());
        setOut(node.getL(), null);

        DataType typeR = (DataType)getOut(node.getR());
        setOut(node.getR(), null);

        setOut(node, DataType.INVALID);

        if (typeL == DataType.NUMBER && typeR == DataType.NUMBER) {
            setOut(node, DataType.NUMBER);
        } else {
            System.out.printf("ERROR: Invalid mod between %s and %s\n", typeL, typeR);
        }
    }

    @Override
    public void outAExpPArithmetics(AExpPArithmetics node) {
        DataType typeL = (DataType)getOut(node.getL());
        setOut(node.getL(), null);

        DataType typeR = (DataType)getOut(node.getR());
        setOut(node.getR(), null);

        setOut(node, DataType.INVALID);

        if (typeL == DataType.NUMBER && typeR == DataType.NUMBER) {
            setOut(node, DataType.NUMBER);
        } else {
            System.out.printf("ERROR: Invalid exponent between %s and %s\n", typeL, typeR);
        }
    }

    @Override
    public void outAMaxPArithmetics(AMaxPArithmetics node) {
        DataType type = (DataType) getOut(node.getPArithmetics());
        setOut(node, type);
        for (var other: node.getPCommaValue())
        {
            DataType oType = (DataType) getOut(((APCommaValue)other).getPArithmetics());
            if (oType != type)
            {
                System.out.printf("ERROR: Invalid max between %s and %s\n", type, oType);
                setOut(node, DataType.INVALID);
                break;
            }
        }
    }

    @Override
    public void outAMinPArithmetics(AMinPArithmetics node) {
        DataType type = (DataType) getOut(node.getPArithmetics());
        setOut(node, type);
        for (var other: node.getPCommaValue())
        {
            DataType oType = (DataType) getOut(((APCommaValue)other).getPArithmetics());
            if (oType != type)
            {
                System.out.printf("ERROR: Invalid min between %s and %s\n", type, oType);
                setOut(node, DataType.INVALID);
                break;
            }
        }
    }

    @Override
    public void outALenPArithmetics(ALenPArithmetics node){

        DataType type = (DataType) getOut(node.getPArithmetics());

        // set default as invalid
        setOut(node, DataType.INVALID);

        // STRING are accepted
        if (type == DataType.STRING) {
            setOut(node, DataType.NUMBER);
        }
        else {
            System.out.printf("ERROR:len(...) is only allowed on STRING. Type %s is invalid\n", type);
        }
    }

    @Override
    public void outAAssignOpPStatement(AAssignOpPStatement node) {
        String variableName = node.getId().toString().trim();

        DataType expressionType = (DataType) getOut(node.getPArithmetics());

        SymbolTableEntryVariable newVariable = variablesTable.get(variableName);

        if (newVariable == null) {
            newVariable = new SymbolTableEntryVariable();
            variablesTable.put(variableName, newVariable);
        }
        // add expressionType in symboltable
        newVariable.type = expressionType;
        newVariable.value = node.getPArithmetics().toString().trim();

    }

    @Override
    public void outAAssignDivPStatement(AAssignDivPStatement node) {
        String variableName = node.getId().toString().trim();

        DataType expressionType = (DataType) getOut(node.getPArithmetics());

        SymbolTableEntryVariable variable = variablesTable.get(variableName);

        if (variable == null) {
            System.out.printf("ERROR at [%s,%s]: Variable %s has not been declared, /= in invalid\n", node.getId().getLine(), node.getId().getPos(), variableName);
        }
        else if (variable.type != DataType.NUMBER) {
            System.out.printf("ERROR at [%s,%s]: /= accepts only numbers, /= with type %s in invalid\n", node.getId().getLine(), node.getId().getPos(), variable.type);
            variable.type = DataType.INVALID;
        }
        else {
            variable.type = expressionType;
            variable.value = node.getPArithmetics().toString().trim();
        }

    }

    @Override
    public void outAAssignMinusPStatement(AAssignMinusPStatement node) {
        String variableName = node.getId().toString().trim();

        DataType expressionType = (DataType) getOut(node.getPArithmetics());

        SymbolTableEntryVariable variable = variablesTable.get(variableName);

        if (variable == null) {
            System.out.printf("ERROR at [%s,%s]: Variable %s has not been declared, -= in invalid\n", node.getId().getLine(), node.getId().getPos(), variableName);
        }
        else if (variable.type != DataType.NUMBER) {
            System.out.printf("ERROR at [%s,%s]: -= accepts only numbers, -= with type %s in invalid\n", node.getId().getLine(), node.getId().getPos(), variable.type);
            variable.type = DataType.INVALID;
        }
        else {
            variable.type = expressionType;
            variable.value = node.getPArithmetics().toString().trim();
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