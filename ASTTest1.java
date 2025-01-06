import java.io.*;
import minipython.lexer.Lexer;
import minipython.parser.Parser;
import minipython.node.Start;

import java.util.ArrayList;
import java.util.Map.Entry;

public class ASTTest1
{
  public static void main(String[] args)
  {
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

      for(Entry<String, ArrayList<SymbolTableEntry>> e : firstPass.symbolTable.entrySet())
      {
        ArrayList<SymbolTableEntry> entries = e.getValue();
        
        for(int i = 0; i < entries.size(); ++i)
        {
          SymbolTableEntry entry = entries.get(i);

          if(entry.type != SymbolTableEntry.EntryType.FUNCTION) continue;

          System.out.printf("def %s(", e.getKey());
          for(int j = 0; j < entry.parameterNames.size(); ++j)
          {
            if(entry.defaultValues.get(j).equals("None"))
            {
              System.out.printf("%s,", entry.parameterNames.get(j));
            }
            else
            {
              System.out.printf("%s=%s,", entry.parameterNames.get(j), entry.defaultValues.get(j));
            }
          }

          System.out.printf(")\n");
        }
      }

      for(Entry<String, ArrayList<SymbolTableEntry>> e : firstPass.symbolTable.entrySet())
      {
        ArrayList<SymbolTableEntry> entries = e.getValue();
        
        for(int i = 0; i < entries.size(); ++i)
        {
          SymbolTableEntry entry = entries.get(i);
          
          if(entry.type != SymbolTableEntry.EntryType.VARIABLE) continue;
          
          System.out.printf("%s=%s\n", e.getKey(), entry.value);
        }
      }

	//ast.apply(new ASTPrinter());

//      System.out.println(ast);
    }
    catch (Exception e)
    {
      System.err.println(e);
    }
  }
}

