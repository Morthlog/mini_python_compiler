import java.io.*;
import minipython.lexer.Lexer;
import minipython.node.Node;
import minipython.parser.Parser;
import minipython.node.Start;

import java.util.ArrayList;
import java.util.Map.Entry;

public class ASTTest1
{
	public static void main(String[] args){

		try
		{
			Parser parser = 
			new Parser(
			new Lexer(
			new PushbackReader(
			new FileReader(args[0].toString()), 1024)));

			Start ast = parser.parse();

			VisitorFirstPass firstPass = new VisitorFirstPass();

			ast.apply(firstPass);

			for(Entry<String, ArrayList<Tuples<Node, SymbolTableEntryFunction>>> e : firstPass.functionsTable.entrySet())
			{
				ArrayList<Tuples<Node, SymbolTableEntryFunction>> entries = e.getValue();
				
				for(int i = 0; i < entries.size(); ++i)
				{
					SymbolTableEntryFunction entry = entries.get(i).getSecond();

					System.out.printf("def %s(", e.getKey());
					for(int j = 0; j < entry.parameters.size(); ++j)
					{
						if(entry.parameters.get(j).defaultValue.equals("None"))
						{
							System.out.printf("%s,", entry.parameters.get(j).name);
						}
						else
						{
							System.out.printf("%s=%s,", entry.parameters.get(j).name, entry.parameters.get(j).defaultValue);
						}
					}

					System.out.printf(")\n");
				}
			}

			for(Entry<String, SymbolTableEntryVariable> e : firstPass.variablesTable.entrySet())
			{
				SymbolTableEntryVariable entry = e.getValue();
					
				System.out.printf("%s=%s\n", e.getKey(), entry.value);
			}


			System.out.println("==========================================");
			VisitorSecondPass secondPass = new VisitorSecondPass(firstPass.variablesTable, firstPass.functionsTable);

			ast.apply(secondPass);
			//ast.apply(new ASTPrinter());

			//      System.out.println(ast);
		}
		catch (Exception e)
		{
			System.err.println(e);
		}
	}
}

