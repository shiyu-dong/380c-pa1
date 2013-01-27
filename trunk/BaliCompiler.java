import java.io.*;
import java.util.*;
import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;

public class BaliCompiler
{
  static Hashtable<String, Integer> method_list;
  static int label_count;

  public static void main(String[] args) {
    label_count = 0;
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
    String pgm = "";
    int offset;

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

      // open parenthesis
      if (!f.check ('(')) {
        throw new Exception("Expect '(' in method decleartion");
      }
      // formals
      int input_num = getFormals(f, symt);
      method_list.put(methodName, input_num);
      //System.out.println("Method Name: "+methodName);
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

      pgm += getStatements(f, symt, methodName, -1);

      offset = symt.get("rv") - symt.get("FBR");
      pgm += methodName + "End:\n"
              + "STOREOFF " + offset + "\n"
              + "JUMPIND";

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

  static int  getFormals(SamTokenizer f, Hashtable<String, Integer> symt) throws Exception {
    String ID;
    String code="";
    int count = 0;
    while(!f.test(')')) {
      if (!f.check("int")) {
        throw new Exception("Expect 'int' in formal");
      }
      ID = f.getWord();
      symt.put(ID, symt.size());
      count ++;
      //System.out.print("ID: "+ ID+"\n");
      if (f.test(')')) {
        System.out.print("Done with Formal");
        return count;
      }
      if (!f.check(',')) {
        throw new Exception("Expect 'int' in formal");
      }
    }
    System.out.print("Done with Formal");

    return 0;
  }

  static String getDeclarations(SamTokenizer f, Hashtable<String, Integer> symt) throws Exception {
    String pgm = "";

    while(f.test("int")) {
      f.check("int");
      pgm += getDeclaration(f, symt);
      if (!f.check(';')) {
        throw new Exception("Expect ';' at end of line");
      }
    }

    return pgm;
  }

  static String getDeclaration(SamTokenizer f, Hashtable<String, Integer> symt) throws Exception{
    String pgm = "";
    String ID;
    Character c;

    System.out.println(f.peekAtKind());
    ID = f.getWord();
    System.out.println(ID);
    symt.put(ID, symt.size());
    if (f.test('=')) {
      f.check('=');
      pgm += getExp(f, symt);
      pgm += "PUSHOFF "+ symt.get(ID) + "\n";
    }

    if (f.test(',')) {
      f.check(',');
      pgm += getDeclaration(f, symt);
    };

    return pgm;
  }

  static String getStatements(SamTokenizer f, Hashtable<String, Integer> symt, String methodName, int current_while_label) throws Exception{
    String pgm = "";
    String tmp;

    while (!f.test('}')) {
      // empty statement ';'
      if (f.test(';')) {
        f.check(';');
        continue;
      }

      // block
      if (f.test('{')) {
        f.check('{');
        pgm += getStatements(f, symt, methodName, current_while_label);
        if (!f.check('}')) {
          throw new Exception("Expect '}' at the end of the statement block");
        }
      }

      tmp = f.getWord();

      // return
      if (tmp.equals("return")) {
        pgm += getExp(f, symt);
        return pgm + "JUMP " + methodName + "End\n";
      }

      // if e then B1 else B2
      if (tmp.equals("if")) {
        int current_label_count = label_count;
        label_count++;

        if (!f.check('('))
          throw new Exception("Expect '(' after 'if'");
        pgm += getExp(f, symt); // code for if condition e
        if (!f.check(')'))
          throw new Exception("Expect ')' after 'if'");
        pgm += "JUMPC " +"Taken" + current_label_count + "\n"; // branch taken

        String B1 = getStatements(f, symt, methodName, current_while_label);

        if (!f.check("else"))
          throw new Exception("Expect 'else after 'if'");

        String B2 = getStatements(f, symt, methodName, current_while_label);

        pgm += B2;
        pgm += "JUMP BranchEnd" + current_label_count + "\n";
        pgm += "Taken" + current_label_count + ":\n";
        pgm += B1;
        pgm += "BranchEnd" + current_label_count + ":\n";
      }

      // while ( E ) B
      if (tmp.equals("while")) {
        int current_label_count = label_count;
        label_count++;

        pgm += "JUMP " + "WhileLabel1_" + current_label_count + "\n";
        pgm += "WhileLabel2_" + current_label_count + ":\n";

        if (!f.check('('))
          throw new Exception("Expect '(' after 'while'");
        String E = getExp(f, symt); // code for while condition E
        if (!f.check(')'))
          throw new Exception("Expect ')' after 'while'");

        String B = getStatements(f, symt, methodName, current_label_count);

        pgm += B;
        pgm += "WhileLabel1_" + current_label_count + ":\n";
        pgm += E;
        pgm += "JUMPC WhileLabel2_"+current_label_count + "\n";
        pgm += "WhileLabel3_" + current_label_count + ":\n";
      }

      // break
      if (tmp.equals("break")) {
        if (!f.check(";")) {
          throw new Exception("Expecting ';' after break");
        }
        if (current_while_label == -1) {
          throw new Exception("'break' not used inside a loop");
        }
        pgm += "JUMP WhileLabel3_" + current_while_label + "\n";
      }


    }
    return pgm;
  }

  static String getExp(SamTokenizer f, Hashtable<String, Integer> symt) throws Exception{
    switch (f.peekAtKind()) {
      case INTEGER: // E -> LITERAL -> INT
      {
        return "PUSHIMM " + f.getInt() + "\n";
      }

      case WORD:
      {
        String tmp = f.getWord();
        if (tmp.equals("true")) { // E -> LITERAL -> true
          return "PUSHIMM 1\n";
        }
        if (tmp.equals("false")) { // E -> LITERAL -> false
          return "PUSHIMM 0\n";
        }
        if (method_list.contains(tmp)) { // E -> METHOD ( ACTUALS )
          String pgm = "PUSHIMM 0\n";
          if (!f.check('(')) {
            throw new Exception("expecting '(' in functions actuals");
          }
          while (!f.test(')')) {
            pgm += getExp(f, symt);
          }
          if (!f.check(')')) {
            throw new Exception("expecting ')' in functions actuals");
          }
          return (pgm +
                  "LINK\n" +
                  "JSR " + tmp + "\n" +
                  "POPFBR\n" +
                  "ADDSP " + method_list.get(tmp) + "\n");
        }
        if (symt.contains(tmp)) { // E -> LOCATION -> ID
          int offset = symt.get(tmp) - symt.get("FBR");
          return "PUSHIMM " + offset;
        }
        break;
      }

      case OPERATOR:
      {
        String pgm = "";
        char op;
        if (!f.check('('))
          throw new Exception("Expecting '('");
        if (f.test('-')) {
          f.check('-');
          pgm += getExp(f, symt);
          pgm += "PUSHIMM 0\n" + "SUB\n";
          if (!f.check(')'))
            throw new Exception("Expecting ')'");
          return pgm;
        }
        if (f.test('!')) {
          f.check('!');
          pgm += getExp(f, symt);
          pgm += "NOT\n";
          if (!f.check(')'))
            throw new Exception("Expecting ')'");
          return pgm;
        }

        // E -> ( E ...
        pgm += getExp(f, symt);

        // E -> ( E )
        if (f.test(')')) {
          return pgm;
        }

        // E -> ( E ? E )
        op = f.getOp();
        pgm += getExp(f, symt);

        switch (op) {
          case '+':
            return pgm + "ADD\n";
          case '-':
            return pgm + "SUB\n";
          case '*':
            return pgm + "TIMES\n";
          case '/':
            return pgm + "DIV\n";
          case '&':
            return pgm + "AND\n";
          case '|':
            return pgm + "OR\n";
          case '<':
            return pgm + "LESS\n";
          case '>':
            return pgm + "GREATER\n";
          case '=':
            return pgm + "EQUAL\n";
        }
        break;
      }

      default:
        throw new Exception("Cannot recognize the expression");
    }
    return null;
  }
}
