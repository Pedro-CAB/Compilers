package pt.up.fe.comp2023.analysis;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;
import java.util.*;
import pt.up.fe.comp2023.symbol.table.Table;
import pt.up.fe.comp2023.symbol.table.TableVisitor;


public class Analysis implements JmmAnalysis {

    public List<Symbol> getRelevantVars(String methodName, Table table) {
        List<Symbol> vars = table.getLocalVariables(methodName);
        vars.addAll(table.getParameters(methodName));
        vars.addAll(table.getFields());
        return vars;
    }

    public Report createReport(JmmNode node, String message) {
        int startLine = Integer.parseInt(node.get("lineStart")), startColumn = Integer.parseInt(node.get("colStart"));
        System.out.println("NEW ERROR IN LINE " + startLine + "  ! -> " + message);
        return new Report(ReportType.ERROR, Stage.SEMANTIC, startLine, startColumn, message);
    }

    public List<Report> eraseDuplicateReports(List<Report> reports) {
        Set<Report> set = new HashSet<>(reports);
        return new ArrayList<>(set);
    }

    public Boolean isContainedInImports(String className, List<String> imports) {
        return imports.contains("import " + className + ";\n");
    }

    public Boolean isVarArray(String varName, List<Symbol> vars) {
        for (Symbol var : vars) {
            if (Objects.equals(var.getName(), varName)) {
                if (var.getType().isArray())
                    return true;
            }
        }
        return false;
    }

    public String getVarType(String varName, List<Symbol> vars) {
        for (Symbol var : vars) {
            if (Objects.equals(var.getName(), varName)) {
                if (var.getType().isArray() && !Objects.equals(var.getType().getName(), "String"))
                    return var.getType().getName() + "[]";
                else
                    return var.getType().getName();
            }
        }
        return null;
    }

    public String getTypeOfBinaryOp(JmmNode node, List<Symbol> vars) {
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType, secondType;
        System.out.println(vars);
        firstType = switch (first.getKind()) {
            case "Identifier" -> getVarType(first.get("value"), vars);
            case "Boolean" -> "boolean";
            case "Integer" -> "int";
            case "BinaryOp" -> getTypeOfBinaryOp(first, vars);
            default -> "invalid_type";
        };
        secondType = switch (second.getKind()) {
            case "Identifier" -> getVarType(second.get("value"), vars);
            case "Boolean" -> "boolean";
            case "Integer" -> "int";
            case "BinaryOp" -> getTypeOfBinaryOp(first, vars);
            default -> "invalid_type";
        };
        System.out.println("getTypeOfBynaryOp::Evaluating " + firstType + " " + node.get("op") + " " + secondType);
        switch (node.get("op")) {
            case ">":
            case "<":
            case "==":
            case "!=":
            case ">=":
            case "<=":
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                if (!Objects.equals(firstType, "integer") || !Objects.equals(secondType, "integer")) {
                    return "invalid";
                } else return "integer";
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
                if (!Objects.equals(first.getKind(), "Identifier") || (!Objects.equals(second.getKind(), "Integer") && !Objects.equals(second.getKind(), "Identifier")))
                    return "invalid";
            default:
                return "invalid";
        }
    }

    public List<Report> visitBinaryOp(List<Report> reports, JmmNode node, Table table, List<Symbol> vars, String methodName) {
        System.out.println("Called visitBinaryOp");
        String operator = node.get("op");
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType, secondType;
        switch (first.getKind()) {
            case "Identifier" -> {
                firstType = getVarType(first.get("value"), vars);
                reports.addAll(visitIdentifier(reports, first, table, vars, firstType));
            }
            case "Boolean" -> firstType = "boolean";
            case "Integer" -> firstType = "int";
            case "BinaryOp" -> {
                firstType = getTypeOfBinaryOp(first, vars);
                reports.addAll(visitBinaryOp(reports, first, table, vars, methodName));
            }
            case "ArrayAcess" ->{
                firstType = getArrayAccessReturn(first, vars);
                reports.addAll(visitArrayAcess(reports,first,table,methodName));
            }
            default -> {
                firstType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
        switch (second.getKind()) {
            case "Identifier" -> {
                secondType = getVarType(second.get("value"), vars);
                reports.addAll(visitIdentifier(reports, second, table, vars, secondType));
            }
            case "Boolean" -> secondType = "boolean";
            case "Integer" -> secondType = "int";
            case "BinaryOp" -> {
                secondType = getTypeOfBinaryOp(first, vars);
                reports.addAll(visitBinaryOp(reports, first, table, vars, methodName));
            }
            case "ArrayAcess" ->{
                secondType = getArrayAccessReturn(second, vars);
                reports.addAll(visitArrayAcess(reports,second,table,methodName));
            }
            default -> {
                secondType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
        System.out.println("Evaluating " + first.getKind() + " and " + second.getKind());
        System.out.println("Evaluating " + firstType + " " + operator + " " + secondType);
        switch (operator) {
            //Comparators
            case ">":
            case "<":
            case "==":
            case "!=":
            case ">=":
            case "<=":
                //Arithmetic Operators
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                //If Type of one of the operands is invalid, the problem is already reported
                if (!Objects.equals(firstType, "invalid_type") && !Objects.equals(secondType, "invalid_type")) {
                    if (!Objects.equals(firstType, "int") || !Objects.equals(secondType, "int")) {
                        reports.add(createReport(node, "Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'."));
                    }
                }
                break;
            //Incrementation
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
                if (!Objects.equals(firstType, "invalid")) {
                    if (!Objects.equals(first.getKind(), "Identifier")) {
                        reports.add(createReport(first, "Expected a variable, found '" + first.get("value")));
                    } else if (!Objects.equals(firstType, secondType)) {
                        reports.add(createReport(first, "Incompatible types. Cannot use '" + operator + "' with '" + firstType + "' and '" + secondType + "'."));
                    }
                    if (!Objects.equals(secondType, "invalid")) {
                        if (!Objects.equals(secondType, "integer")) {
                            reports.add(createReport(second, "Trying to increment '" + firstType + "' to '" + secondType + "'."));
                        }
                    }
                }
                break;
            //Logic Operators (Can only be used between booleans)
            case "&&":
            case "||":
                if (!Objects.equals(firstType, "boolean") || !Objects.equals(secondType, "boolean")) {
                    reports.add(createReport(node, "Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'boolean'."));
                }
                //Bitwise Operators (Can only be used between ints)
            case "&":
            case "|":
            case "^":
                if (!Objects.equals(firstType, "int") || !Objects.equals(secondType, "int")) {
                    reports.add(createReport(node, "Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'int'."));
                }
            default:
                System.out.println("OP TYPE NAO CONTABILIZADO EM visitBinaryOP : " + node.get("op"));
        }
        return reports;
    }

    public List<Report> visitReturn(List<Report> reports, JmmNode root, Table table, String methodName) {
        JmmNode child = root.getChildren().get(0);
        List<Symbol> vars = getRelevantVars(methodName, table);
        String returnType = table.getReturnType(methodName).getName();
        switch (child.getKind()) {
            case "Integer":
                if (!Objects.equals(returnType, "int")) {
                    reports.add(createReport(child, "Method " + methodName + " should return " + returnType + " but is returning 'int'."));
                }
                break;
            case "Boolean":
                if (!Objects.equals(returnType, "boolean")) {
                    reports.add(createReport(child, "Method " + methodName + " should return " + returnType + " but is returning 'boolean'."));
                }
                break;
            case "Identifier":
                String varName = child.get("value");
                String varType = getVarType(varName, vars);
                reports.addAll(visitIdentifier(reports, child, table, vars, varType));
                break;
            case "BinaryOp":
                String operationType = getTypeOfBinaryOp(child, vars);
                System.out.println("OPERATION TYPE -> " + operationType);
                if (Objects.equals(operationType, "invalid")) {
                    System.out.println("CALLING VISITBINARYOP OVER -> " + child.getKind());
                    reports.addAll(visitBinaryOp(reports, child, table, vars, methodName));
                } else if (!Objects.equals(operationType, returnType)) {
                    reports.add(createReport(child, "Method " + methodName + " should return '" + returnType + "' but is returning '" + operationType + "'."));
                }
                break;
            case "MethodCalls":
                String methodReturnType = getMethodCallType(child, table, vars);
                String methodCallType = getMethodCallType(child, table, vars);
                if (!Objects.equals(methodReturnType, returnType) && methodCallType != null) {
                    String methodCallName = getMethodCallName(child, vars);
                    reports.add(createReport(child, "Return type of " + methodName + " is '" + returnType + "' but " + methodCallName + " returns '" + methodReturnType + "'."));
                }
                reports.addAll(visitMethodCalls(reports, child, table, vars));
                break;
            case "ArrayAcess":
                reports.addAll(visitArrayAcess(reports, child, table, methodName));
        }
        return reports;
    }

    public String getMethodCallType(JmmNode node, Table table, List<Symbol> vars) {
        String methodName = node.getChildren().get(1).get("methodName");
        String methodClassName = getVarType(node.getChildren().get(0).get("value"), vars);
        if (Objects.equals(table.getClassName(), methodClassName)) { //In case it is the class where the method is called
            return table.getReturnType(methodName).toString();
        } else return null;
    }

    public String getMethodCallName(JmmNode node, List<Symbol> vars) {
        return getVarType(node.getChildren().get(0).get("value"), vars);
    }

    public List<Report> visitMethodCalls(List<Report> reports, JmmNode root, Table table, List<Symbol> vars) {
        System.out.println("called visitMethodCalls");
        String childKind = root.getChildren().get(0).getKind();
        System.out.println(childKind);
        if ("Identifier".equals(childKind)) {
            String methodClassName = getVarType(root.getChildren().get(0).get("value"), vars);
            String className = table.getClassName();
            List<String> imports = table.getImports();
            if (!isContainedInImports(methodClassName, imports) && !Objects.equals(methodClassName, className)) {
                reports.add(createReport(root, "Class " + methodClassName + " doesn't exist. Maybe you should have imported it?"));
            }
            String calledMethodName = root.getJmmChild(1).get("methodName");
            System.out.println(methodClassName);
            System.out.println(className);
            System.out.println();
            if (Objects.equals(methodClassName, className) && !table.getMethods().contains(calledMethodName) && Objects.equals(table.getSuper(), "")){
                reports.add(createReport(root, "Method " + calledMethodName + " is not declared."));
            }
        }
        return reports;
    }

    public String getArrayAccessReturn(JmmNode root, List<Symbol> vars) {
        JmmNode arrayChild = root.getJmmChild(0);
        for (Symbol var : vars) {
            if (Objects.equals(var.getName(), arrayChild.get("value"))) {
                return var.getType().getName();
            }
        }
        return null;
    }

    public List<Report> visitAssignment(List<Report> reports, JmmNode root, Table table, String methodName) {
        System.out.println("Called visitAssignment");
        JmmNode child = root.getChildren().get(0);
        List<Symbol> vars = getRelevantVars(methodName, table);
        String varType = getVarType(root.get("var"), vars);
        String kind = child.getKind();
        System.out.println("        visitAssignment :: Descendants of" + root.getKind() + " are " + root.getChildren());
        switch (child.getKind()) {
            case "Modifier":
                System.out.println("???? -> " + child);
                break;
            case "NewObject":
                String assignType = child.getChildren().get(0).get("type");
                if (Objects.equals(assignType, "int")) assignType += "[]";
                if (varType == null) {
                    String message = "Variable " + root.get("var") + " does not exist.";
                    reports.add(createReport(root, message));
                } else if (!Objects.equals(varType, assignType)) {
                    String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                    reports.add(createReport(root, message));
                }
                break;
            case "ArrayAcess":
                String arrayReturnType = getArrayAccessReturn(child, vars);
                String arrayType = getVarType(child.getJmmChild(0).get("value"), vars);
                if (!Objects.equals(arrayReturnType, varType)) {
                    reports.add(createReport(child, "Assigning variable of type '" + varType + "' to element of array of type '" + arrayType + "'."));
                }
                reports.addAll(visitArrayAcess(reports, child, table, methodName));
            case "Integer":
                assignType = "int";
                if (!Objects.equals(varType, assignType)) {
                    String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                    reports.add(createReport(root, message));
                }
                break;
            case "Boolean":
                assignType = "boolean";
                if (!Objects.equals(varType, assignType)) {
                    String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                    reports.add(createReport(root, message));
                }
                break;
            case "Identifier":
                String varName = root.get("var");
                String assignedVarType = getVarType(varName, vars);
                reports.addAll(visitIdentifier(reports, child, table, vars, assignedVarType));
                break;
            case "BinaryOp":
                String operationType = getTypeOfBinaryOp(child, vars);
                if (Objects.equals(operationType, "invalid")) {
                    reports.addAll(visitBinaryOp(reports, child, table, vars, methodName));
                } else if (!Objects.equals(operationType, varType)) {
                    reports.add(createReport(child, "Assignment between a '" + varType + "' and a '" + operationType + "'."));
                }
                break;
            case "MethodCalls":
                reports.addAll(visitMethodCalls(reports, child, table, vars));
                break;
            default:
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitAssignment : " + child.getKind());
                break;
        }
        return reports;
    }

    public List<Report> visitIdentifier(List<Report> reports, JmmNode root, Table table, List<Symbol> vars, String varType) {
        String idType = getVarType(root.get("value"), vars);
        if (idType == null) { //Checks if variable was previously declared
            reports.add(createReport(root, "Variable " + root.get("value") + " is not declared."));
        } else {
            switch (root.getJmmParent().getKind()) {
                case "Assignment":
                    if (!Objects.equals(varType, idType)) {
                        if (!(Objects.equals(varType, table.getClassName()) && idType.equals(table.getSuper())) &&
                                !(Objects.equals(idType, table.getClassName()) && varType.equals(table.getSuper())) &&
                                (!isContainedInImports(idType,table.getImports()) || !isContainedInImports(varType,table.getImports())))
                            reports.add(createReport(root, "Assignment between a '" + varType + "' and a '" + idType + "'."));
                    }
                    break;
                default:
                    break;
            }
        }
        return reports;
    }


    public List<Report> visitDeclaration(List<Report> reports, JmmNode root, Table table, String methodName){
        String varName = root.get("var");
        List<Symbol> vars = getRelevantVars(methodName,table);
        String className = table.getClassName(), methodClassName = root.getChildren().get(0).get("type");
        if (getVarType(varName,vars) == null){
            reports.add(createReport(root,"Variable " + varName + " is already declared."));
        }
        return reports;
    }

    public List<Report> visitMethodBody(List<Report> reports, JmmNode node, Table table, String methodName){
        List<Symbol> vars = getRelevantVars(methodName,table);
        for(JmmNode child : node.getChildren()){
            switch(child.getKind()) {
                case "Assignment":
                    reports.addAll(visitAssignment(reports, child, table, methodName));
                    break;
                case "Declaration":
                    reports.addAll(visitDeclaration(reports,child,table,methodName));
                    break;
                case "MethodCalls":
                    visitMethodCalls(reports,child,table,vars);
                    break;
                case "Return":
                    reports.addAll(visitReturn(reports, child, table, methodName));
                    break;
                case "IfElse":
                    reports.addAll(visitIfElse(reports,child,table,methodName));
                    break;
                case "ExprStmt":
                    reports.addAll(visitExprStmt(reports,child,table, methodName));
                    break;
                case "While":
                    reports.addAll(visitWhile(reports,child,table,methodName));
                    break;
                default:
                    System.out.println("    visitMethodBody :: NODE TYPE NAO CONTABILIZADO " + child.getKind());
                    break;
            }
        }
        return reports;
    }

    private Collection<? extends Report> visitWhile(List<Report> reports, JmmNode root, Table table, String methodName) {
        JmmNode condition = root.getJmmChild(0);
        List<Symbol> vars = getRelevantVars(methodName,table);
        switch(condition.getKind()){
            case "Identifier":
                String varName = condition.get("value");
                String varType = getVarType(varName,vars);
                if(!Objects.equals(varType, "boolean"))
                    reports.add(createReport(condition,"Condition should be 'boolean'."));

        }
        reports.addAll(visitMethodBody(reports,root.getJmmChild(1),table,methodName));
        return reports;
    }

    public List<Report> visitArrayAcess(List<Report> reports, JmmNode root, Table table, String methodName){
        List<Symbol> vars = getRelevantVars(methodName,table);
        JmmNode varChild = root.getChildren().get(0);
        System.out.println(varChild);
        JmmNode indexChild = root.getChildren().get(1);
        String varName = varChild.get("value");
        if (getVarType(varName,vars) == null){
            reports.add(createReport(varChild,"Variable " + varName + " was not declared."));
        }
        else if (!isVarArray(varName,vars)){
            reports.add(createReport(varChild,"Array Access over variable " + varName + " which is not an array."));
        }
        switch(indexChild.getKind()){
            case "Integer":
                break;
            case "Identifier":
                String accessVarName = indexChild.get("value");
                String accessVarType = getVarType(accessVarName,vars);
                if (!Objects.equals(accessVarType, "int")){
                    reports.add(createReport(indexChild,"Array Access Index should be of type 'int'."));
                }
                else {
                    reports.addAll(visitIdentifier(reports, indexChild, table, vars, getVarType(accessVarName, vars)));
                }
        }
        return reports;
    }

    public List<Report> visitExprStmt(List<Report> reports, JmmNode root, Table table, String methodName){
        System.out.println("called visitExprSmt");
        System.out.println(root.getChildren().get(0).getKind());
        List<Symbol> vars = getRelevantVars(methodName,table);
        JmmNode child = root.getChildren().get(0);
        switch(child.getKind()){
            case "BinaryOp":
                reports.addAll(visitBinaryOp(reports,child,table,vars, methodName));
                break;
            case "ArrayAcess":
                break;
            case "MethodCalls":
                reports.addAll(visitMethodCalls(reports,child,table,vars));
        }
        return reports;
    }

    public List<Report> visitMethod(List<Report> reports, JmmNode root, Table table){
        System.out.println("    Called visitMethod \n");
        Queue<JmmNode> queue = new LinkedList<>();
        queue.addAll(root.getChildren());
        String name = root.get("name"), returnType;
        Boolean isPrivate = Boolean.FALSE, isStatic = Boolean.FALSE;
        while (queue.size() > 0){
                JmmNode node = queue.remove();
                System.out.println("    visitMethod :: Visiting Node " + node.getKind());
                String kind = node.getKind();
                switch(kind){
                    case "Argument":
                        //Por agora não é necessário explorar os argumentos
                        break;
                    case "MethodBody":
                        reports.addAll(visitMethodBody(reports,node,table,name));
                        break;
                    case "Modifier":
                        switch(node.get("value")){
                            case "private":
                                isPrivate = Boolean.TRUE;
                                break;
                            case "static":
                                isStatic = Boolean.TRUE;
                                break;
                            default:
                                break;
                        }
                        break;
                    case "Type":
                        returnType = node.get("type");
                        break;
                    case "ExprStmt":
                        reports.addAll(visitExprStmt(reports,node,table, name));
                        break;
                    case "IfElse":
                        reports.addAll(visitIfElse(reports,node,table,name));
                    default:
                        System.out.println("    visitMethod :: NODE TYPE NAO CONTABILIZADO " + node.getKind());
                        break;
                    }
        }
        return reports;
    }

    public List<Report> visitIfElse(List<Report> reports, JmmNode node, Table table, String methodName) {
        JmmNode condition = node.getJmmChild(0), ifNode = node.getJmmChild(1), elseNode = null;
        if(node.getChildren().size() == 3) elseNode = node.getJmmChild(2);
        switch(condition.getKind()){
            case "Boolean":
                break;
            case "BinaryOp":
                String conditionType = getTypeOfBinaryOp(condition,getRelevantVars(methodName,table));
                if (!Objects.equals(conditionType, "boolean") && conditionType != "invalid_type")
                    reports.add(createReport(condition,"Expected a 'boolean' inside If condition but received a '" + conditionType + "'."));
                else if (conditionType == "invalid_type")
                    reports.addAll(visitBinaryOp(reports,condition,table,getRelevantVars(methodName,table),methodName));
                break;
            default:
                reports.add(createReport(condition,"Expected a 'boolean' inside If Condition."));
        }
        reports.addAll(visitMethodBody(reports,ifNode,table,methodName));
        if (node.getChildren().size() == 3) reports.addAll(visitMethodBody(reports,elseNode,table,methodName));
        return reports;
    }

    public List<Report> visitProgram(List<Report> reports, JmmNode node, Table table){
        Queue<JmmNode> queue = new LinkedList<>();
        queue.add(node);
        while (queue.size() > 0){
            node = queue.remove();
            System.out.println("visitProgram :: Visiting Node " + node.getKind());
            //Se for um dos abaixo, explorar os nós abaixo deles
            switch(node.getKind()){
                case "Program":
                case "ClassDeclaration":
                case "ClassName":
                case "ClassBody":
                    queue.addAll(node.getChildren());
                    break;
                case "ClassMethod":
                    reports.addAll(visitMethod(reports, node, table));
                    break;
                case "ImportPackage":
                    break;
                default:
                    System.out.println("visitProgram :: NODE TYPE NAO CONTABILIZADO " + node.getKind());
                    break;
            }
            //Se for um dos abaixo, ignorar
        }
        return reports;
    }

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){

        Table table = new Table();

        TableVisitor visitor = new TableVisitor(table);
        visitor.visit(parserResult.getRootNode(),"");
        System.out.println("Called semanticAnalysis");

        //New Code Below:
        JmmNode root = parserResult.getRootNode();
        List<Report> reports = new ArrayList<Report>();
        reports = eraseDuplicateReports(visitProgram(reports,root,table));
        JmmSemanticsResult res = new JmmSemanticsResult(parserResult, visitor.getTable(), reports);
        System.out.println("DETECTED ERRORS:");
        for(Report r : reports){
            System.out.println("Error (Line " + r.getLine() + "):" + r.getMessage());
        }
        return res;
    }
}

