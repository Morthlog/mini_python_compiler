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

			System.out.println("==========================================");
			VisitorSecondPass secondPass = new VisitorSecondPass(firstPass.variablesTable, firstPass.functionsTable);

			ast.apply(secondPass);

		}
		catch (Exception e)
		{
			System.err.println(e);
		}
	}
}

