import java.io.*;
import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;

public class BaliCompiler
{
  public static void main(String[] args) {
    if (args.length < 2) {
      // check input arguments
      System.out.println("Please specify input and output file names");
      System.exit(-1);
    }

    try {
      // open output file
      FileOutputStream ofile = new FileOutputStream(args[1], true);

      // run the compiler which generates SaM code stream
      String code = compiler(args[0]);

      // check for compiler error
      if (code.equals("STOP\n")) {
        System.exit(-1);
      }

      // write and close output file
      ofile.write(code.getBytes());
      ofile.close();
    }

    catch(FileNotFoundException ex) {
      System.out.println("File not found");
      System.exit(-1);
    }
    catch(IOException ioe) {
      System.out.println("IOException");
      System.exit(-1);
    }

    return;
  }

  static String compiler(String fileName)
  {
    try
    {
      // Tokenize input file
      SamTokenizer f = new SamTokenizer(fileName);
      String pgm = getProgram(f);
      return pgm;
    }
    catch (Exception e)
    {
      System.out.println("Fatal error: could not compile program");
      return "STOP\n";
    }
  }

  static String getProgram(SamTokenizer f)
  {
    try
    {
      // OS code for SaM
      String pgm="PUSHIMM 0\n" +
                  "LINK\n" +
                  "JSR main\n" +
                  "POPFBR\n" +
                  "STOP\n";

      while(f.peekAtKind()!=TokenType.EOF)
      {
        pgm+= getMethod(f);
      }
      return pgm;
    }
    catch(Exception e)
    {
      System.out.println("Fatal error: could not compile program");
      return "STOP\n";
    }
  }

  static String getMethod(SamTokenizer f)
  {
    //TODO: add code to convert a method declaration to SaM code.
    //Since the only data type is an int, you can safely check for int
    //in the tokenizer.
    //TODO: add appropriate exception handlers to generate useful error msgs.
    f.check("int"); //must match at begining
    String methodName = f.getString(); 
    f.check ("("); // must be an opening parenthesis
    String formals = getFormals(f);
    f.check(")");  // must be an closing parenthesis
    //You would need to read in formals if any
    //And then have calls to getDeclarations and getStatements.
    return null;
  }
  static String getExp(SamTokenizer f)
  {
    switch (f.peekAtKind()) {
      case INTEGER: //E -> integer
        return "PUSHIMM " + f.getInt() + "\n";
      case OPERATOR:  
        {
        }
      default:   return "ERROR\n";
    }
  }
  static String getFormals(SamTokenizer f){
    return null;
  }
}
