import java.io.*;
import java.util.*;
import edu.cornell.cs.sam.io.SamTokenizer;
import edu.cornell.cs.sam.io.Tokenizer;
import edu.cornell.cs.sam.io.Tokenizer.TokenType;

public class BaliCompiler
{
  static Hashtable<String, Integer> method_list;
  static int label_count;
  static int local_count;

  public static void main(String[] args) {
    label_count = 0;
    method_list = new Hashtable<String, Integer>();
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
    String methodName= "";
    String pgm = "";
    int offset;
    String tmp;
    local_count = 0;

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
      Integer input_num = new Integer(1);
      input_num = getFormals(f, symt);
      System.out.println("methond name " + methodName + " " + input_num);
      method_list.put(methodName, input_num);
      // open parenthesis
      if (!f.check (')')) {
        throw new Exception("Expect ')' in method decleartion");
      }
      symt.put("FBR", symt.size());
      symt.put("PC", symt.size());

      if (!f.check ('{')) {
        throw new Exception("Expect '{' in method decleartion");
      }

      tmp = getDeclarations(f, symt);
      pgm += "ADDSP " + local_count + "\n";
      pgm += tmp;

      while(!f.test('}'))
        pgm += getStatements(f, symt, methodName, -1);
      f.check('}');

      offset = symt.get("rv") - symt.get("FBR");
      pgm += methodName + "End:\n"
              + "STOREOFF " + offset + "\n"
              + "ADDSP -" + local_count + "\n"
              + "JUMPIND" + "\n";

//      if (!f.check ('}')) {
//        throw new Exception("Expect '}' in method decleartion");
//      }
      return pgm;
    }
    catch(NullPointerException e) {
      System.out.println("Null Pointer");
      System.exit(-1);
    }
    catch(Exception e){
      if (methodName.equals("")) {
        System.out.println("Expecting method name");
        System.exit(-1);
      }
      else {
        System.out.println("In method " + methodName +", line " + f.lineNo() +": ");
        System.out.println(e.getMessage());
        System.exit(-1);
      }
    }
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
      if (f.test(')')) {
        return count;
      }
      if (!f.check(',')) {
        throw new Exception("Expect 'int' in formal");
      }
    }

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
    int offset;

    ID = f.getWord();
    local_count++;
    symt.put(ID, symt.size());

    if (f.test('=')) {
      f.check('=');
      offset = symt.get(ID) - symt.get("FBR");
      pgm += getExp(f, symt);
      pgm += "STOREOFF "+ offset + "\n";
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
    System.out.println("statement start");

    // empty statement ';'
    if (f.test(';')) {
      f.check(';');
      return pgm;
    }

    // block
    if (f.test('{')) {
      f.check('{');
      while(!f.test('}'))
        pgm += getStatements(f, symt, methodName, current_while_label);
      if (!f.check('}')) {
        throw new Exception("Expect '}' at the end of the statement block");
      }
      return pgm;
    }

    tmp = f.getWord();

    // return
    if (tmp.equals("return")) {
      System.out.println("return");
      pgm += getExp(f, symt);
      if (!f.check(';'))
        throw new Exception("Expecting ';' at the end of the return statement");
      return pgm + "JUMP " + methodName + "End\n";
    }

    // if e then B1 else B2
    else if (tmp.equals("if")) {
      int current_label_count = label_count;
      label_count++;

      if (!f.check('('))
        throw new Exception("Expect '(' after 'if'");
      pgm += getExp(f, symt); // code for if condition e
      if (!f.check(')'))
        throw new Exception("Expect ')' after 'if'");
      pgm += "JUMPC " +"Taken" + current_label_count + "\n"; // branch taken

      System.out.println("IIIIIIFFFFFFFF");
      String B1 = getStatements(f, symt, methodName, current_while_label);
      System.out.println("IIIIIIFFFFFFFF");

      if (!f.check("else"))
        throw new Exception("Expect 'else after 'if'");

      String B2 = getStatements(f, symt, methodName, current_while_label);

      pgm += B2;
      pgm += "JUMP BranchEnd" + current_label_count + "\n";
      pgm += "Taken" + current_label_count + ":\n";
      pgm += B1;
      pgm += "BranchEnd" + current_label_count + ":\n";

      return pgm;
    }

    // while ( E ) B
    else if (tmp.equals("while")) {
      System.out.println("while start");
      int current_label_count = label_count;
      label_count++;

      if (!f.check('('))
        throw new Exception("Expect '(' after 'while'");
      String E = getExp(f, symt); // code for while condition E
      if (!f.check(')'))
        throw new Exception("Expect ')' after 'while'");

      String B = getStatements(f, symt, methodName, current_label_count);

      pgm += "WL1_" + current_label_count + ":\n";
      pgm += E;
      pgm += "ISNIL\n"
             + "JUMPC WL2_" + current_label_count + "\n"
             + B
             + "JUMP WL1_" + current_label_count + "\n"
             + "WL2_" + current_label_count + ":\n";

      System.out.println("while end");

      return pgm;
    }

    // break
    else if (tmp.equals("break")) {
      if (!f.check(';')) {
        throw new Exception("Expecting ';' after break");
      }
      if (current_while_label == -1) {
        throw new Exception("'break' not used inside a loop");
      }
      System.out.println("break");
      pgm += "JUMP WL2_" + current_while_label + "\n";

      return pgm;
    }

    // assign
    else if (symt.containsKey(tmp)) {
      if (f.getOp() != '=') {
        throw new Exception("Expecting '=' after symbol" + tmp);
      }
      pgm += getExp(f, symt);
      int offset = symt.get(tmp) - symt.get("FBR");
      pgm += "STOREOFF " + offset + "\n";
      if (!f.check(';'))
        throw new Exception("Expecting ';' at the end of the assign statement");

      return pgm;
    }

    // undefined
    else {
      throw new Exception("Symbol " + tmp + " undefined");
    }
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
       if (method_list.containsKey(tmp)) { // E -> METHOD ( ACTUALS )
          String pgm = "PUSHIMM 0\n";
          if (!f.check('(')) {
            throw new Exception("expecting '(' in functions actuals");
          }
          while (!f.test(')')) {
            pgm += getExp(f, symt);
            if (f.test(','))
              f.check(',');
          }
          if (!f.check(')')) {
            throw new Exception("expecting ')' in functions actuals");
          }
          return (pgm +
                  "LINK\n" +
                  "JSR " + tmp + "\n" +
                  "POPFBR\n" +
                  "ADDSP -" + method_list.get(tmp) + "\n");
        }
        if (symt.containsKey(tmp)) { // E -> LOCATION -> ID
          int offset = symt.get(tmp) - symt.get("FBR");
          return "PUSHOFF " + offset + "\n";
        }
        else {
          throw new Exception("symbol not defined");
        }
      }

      case OPERATOR:
      {
        String pgm = "";
        char op;
        if (!f.check('('))
          throw new Exception("Expecting '('");
        if (f.test('-')) {
          f.check('-');
          pgm += "PUSHIMM 0\n";
          pgm += getExp(f, symt) + "SUB\n";
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

        if (!f.check(')')) {
          throw new Exception("Expecting ')'");
        }

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
