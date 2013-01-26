import java.io.*;
import java.util.*;
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

  static String compiler(String fileName) {
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

  static String getProgram(SamTokenizer f) {
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
        pgm += getMethod(f);
      }
      return pgm;
    }
    catch(Exception e)
    {
      System.out.println("Fatal error: could not compile program");
      return "STOP\n";
    }
  }

  static String getMethod(SamTokenizer f) {
    //TODO: add code to convert a method declaration to SaM code.
    //Since the only data type is an int, you can safely check for int
    //in the tokenizer.
    //TODO: add appropriate exception handlers to generate useful error msgs.
    String methodName= "";
    String formals = "";
    String pgm = "";
    Hashtable<String, Integer>  symt = new Hashtable<String, Integer>();
    symt.put("rv", symt.size());

    try {
      // return type
      if (!f.check("int")) {
        throw new Exception("Cannot find return type of method");
      }

      // method name
      methodName = f.getWord();
      pgm += methodName + ":\n";
      System.out.println("Method Name: "+methodName);

      // open parenthesis
      if (!f.check ('(')) {
        throw new Exception("Expect '(' in method decleartion");
      }
      // formals
      formals = getFormals(f, symt);
      // open parenthesis
      if (!f.check (')')) {
        throw new Exception("Expect ')' in method decleartion");
      }
      symt.put("FBR", symt.size());
      symt.put("PC", symt.size());

      if (!f.check ('{')) {
        throw new Exception("Expect '{' in method decleartion");
      }

      pgm += getDeclarations(f, symt);

      pgm += getStatements(f, symt);

      if (!f.check ('}')) {
        throw new Exception("Expect '}' in method decleartion");
      }
    }
    catch(Exception e){
      if (methodName.equals("")) {
        System.out.println("Expecting method name");
        System.exit(-1);
      }
      else {
        System.out.println("In method " + methodName +":");
        System.out.println(e.getMessage());
        System.exit(-1);
      }
    }
    //You would need to read in formals if any
    //And then have calls to getDeclarations and getStatements.
    return null;
  }

  static String getFormals(SamTokenizer f, Hashtable<String, Integer> symt) throws Exception {
    String ID;
    String code="";
    while(!f.test(')')) {
      if (!f.check("int")) {
        throw new Exception("Expect 'int' in formal");
      }
      ID = f.getWord();
      symt.put(ID, symt.size());
      System.out.print("ID: "+ ID+"\n");
      if (f.test(')')) {
        System.out.print("Done with Formal");
        return ID; // need to fix..
      }
      if (!f.check(',')) {
        throw new Exception("Expect 'int' in formal");
      }
    }
    System.out.print("Done with Formal");

    return null;
  }

  static String getDeclarations(SamTokenizer f, Hashtable<String, Integer> symt) {
    String pgm = "";
    String ID;

    while(f.test("int")) {
      f.check("int");
      pgm += getDeclaration(f, symt);
      if (!f.check(';')) {
        throw new Exception("Expect ';' at end of line");
    }

    return pgm;
  }

  static String getDeclaration(SamToeknizer f, Hashtable<String, Integer> symt) {
    String pgm = "";

    ID = f.getString();
    symt.put(ID, symt.size());
    if (f.test('=')) {
      f.check('=');
      pgm += getExp(f, symt);
      pgm += "PUSHOFF "+ symt.getValue(ID) + "\n";
    }

    if f.test(',') {
      f.check(',');
      pgm += getDeclaration(fm, symt);
    };

    return pgm;
  }


  static String getStatements(SamTokenizer f, Hashtable<String, Integer> symt) {
    return null;
  }

  static String getExp(SamTokenizer f, Hashtable<String, Integer> symt) {
//    switch (f.peekAtKind()) {
//      case INTEGER: //E -> integer
//        return "PUSHIMM " + f.getInt() + "\n";
//      case OPERATOR:  
//        {
//        }
//      default:   return "ERROR\n";
//    }
    return null;
  }

}
