package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.*;


public class Analysis implements JmmAnalysis {

    public Report createReport(JmmNode node, String message){
        Integer startLine = Integer.parseInt(node.get("lineStart")), startColumn = Integer.parseInt(node.get("colStart"));
        System.out.println("NEW ERROR IN LINE " + startLine + "  ! -> " + message);
        return new Report(ReportType.ERROR,Stage.SEMANTIC, startLine,startColumn,message);
    }

    public List<Report> eraseDuplicateReports (List<Report> reports){
        Set<Report> set = new HashSet<Report>();
        for (Report r : reports){
            set.add(r);
        }
        List<Report> list = new ArrayList<Report>();
        for (Report r : set){
            list.add(r);
        }
        return list;
    }



    public String getVarType(String varName, List<Symbol> vars){
        for (Symbol var : vars){
            if (Objects.equals(var.getName(), varName)){
                if (var.getType().isArray())
                    return var.getType().getName() + "_array";
                else
                    return var.getType().getName();
            }
        }
        return null;
    }

    public String getTypeOfBinaryOp(JmmNode node, List<Symbol> vars){
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
        switch(node.get("op")){
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
                if(!Objects.equals(firstType, "integer") || !Objects.equals(secondType, "integer")){return "invalid";}
                else return "integer";
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
                if(!Objects.equals(first.getKind(), "Identifier") || (!Objects.equals(second.getKind(), "Integer") && !Objects.equals(second.getKind(), "Identifier"))) return "invalid";
            default:
                return "invalid";
        }
    }

    public List<Report> visitBinaryOp(List<Report> reports, JmmNode node, List<Symbol> vars){
        System.out.println("Called visitBinaryOp");
        String operator = node.get("op");
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType,secondType;
        switch (first.getKind()) {
            case "Identifier" -> {
                firstType = getVarType(first.get("value"), vars);
                reports.addAll(visitIdentifier(reports, first, vars,firstType));
            }
            case "Boolean" -> firstType = "boolean";
            case "Integer" -> firstType = "int";
            case "BinaryOp" -> {
                firstType = getTypeOfBinaryOp(first, vars);
                reports.addAll(visitBinaryOp(reports, first, vars));
            }
            default -> {
                firstType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
        switch (second.getKind()) {
            case "Identifier" -> {
                secondType = getVarType(second.get("value"), vars);
                reports.addAll(visitIdentifier(reports, second, vars,secondType));
            }
            case "Boolean" -> secondType = "boolean";
            case "Integer" -> secondType = "int";
            case "BinaryOp" -> {
                secondType = getTypeOfBinaryOp(first, vars);
                reports.addAll(visitBinaryOp(reports, first, vars));
            }
            default -> {
                secondType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
        System.out.println("Evaluating " + first.getKind() + " and " + second.getKind());
        System.out.println("Evaluating " + firstType + " " + operator + " " + secondType);
        switch(operator){
            //Comparators
            case ">":
            case "<":
            case "==":
            case "!=":
            case ">=":
            case "<=":
            //Algebraic Operators
            case "+":
            case "-":
            case "*":
            case "/":
            case "%":
                if (!Objects.equals(firstType, "int") || !Objects.equals(secondType, "int")) {
                    reports.add(createReport(node, "Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'."));
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
                                reports.add(createReport(second,"Trying to increment '" + firstType + "' to '" + secondType + "'."));
                            }
                        }
                }
                break;
            //Logic Operators
            case "&&":
            case "||":
                if(!Objects.equals(firstType, "boolean") || !Objects.equals(secondType, "boolean")){
                    reports.add(createReport(node,"Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'boolean'."));
                }
            //Bitwise Operators
            case "&":
            case "|":
            case "^":
                if(firstType != "int" || secondType != "int"){
                    reports.add(createReport(node,"Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'int'."));
                }
            default:
                System.out.println("OP TYPE NAO CONTABILIZADO EM visitBinaryOP : " + node.get("op"));
        }
        return reports;
    }

    public List<Report> visitReturn(List<Report> reports, JmmNode root, Table table, String methodName){
        JmmNode child = root.getChildren().get(0);
        List<Symbol> vars = table.getLocalVariables(methodName);
        vars.addAll(table.getParameters(methodName));
        String returnType = table.getReturnType(methodName).getName();
        switch(child.getKind()){
            case "Integer":
                if (returnType != "int"){
                    reports.add(createReport(child,"Method " + methodName + " should return" + returnType + " but is returning 'int'."));
                }
                break;
            case "Boolean":
                if (returnType != "boolean"){
                    reports.add(createReport(child,"Method " + methodName + " should return" + returnType + " but is returning 'boolean'."));
                }
                break;
            case "Identifier":
                String varName = child.get("value");
                String varType = getVarType(varName,vars);
                reports.addAll(visitIdentifier(reports,child,vars,varType));
                break;
            case "BinaryOp":
                String operationType = getTypeOfBinaryOp(child,vars);
                System.out.println("OPERATION TYPE -> " + operationType);
                if (operationType == "invalid"){
                    System.out.println("CALLING VISITBINARYOP OVER -> " + child.getKind());
                    reports.addAll(visitBinaryOp(reports,child,vars));
                }
                else if (operationType != returnType){
                    reports.add(createReport(child,"Method " + methodName + " should return '" + returnType + "' but is returning '" + operationType + "'."));
                }
                break;
            case "MethodCalls":
                visitMethodCalls(reports,child,table,vars);
                break;
        }
        return reports;
    }

    public List<Report> visitMethodCalls(List<Report> reports, JmmNode root, Table table, List<Symbol> vars){
        String methodClassName = getVarType(root.getChildren().get(0).get("value"), vars);
        String className = table.getClassName();
        List<String> imports = table.getImports();
        if(!imports.contains(methodClassName) && !Objects.equals(methodClassName,className)){
            reports.add(createReport(root,"Class " + methodClassName + " doesn't exist. Maybe you should have imported it?"));
        }
        return reports;
    }

    public List<Report> visitAssignment(List<Report> reports, JmmNode root, Table table, String methodName){
        System.out.println("Called visitAssignment");
        JmmNode child = root.getChildren().get(0);
        List<Symbol> vars = table.getLocalVariables(methodName);
        vars.addAll(table.getParameters(methodName));
        String varType = getVarType(root.get("var"), vars);
        String kind = child.getKind();
        System.out.println("        visitAssignment :: Descendants of" + root.getKind() + " are " + root.getChildren());
        switch (child.getKind()){
            case "Modifier":
                System.out.println("???? -> " + child);
                break;
            case "NewObject":
                String assignType = child.getChildren().get(0).get("type");
                if (assignType == "int") assignType += "_array";
                if (varType == null){
                    String message = "Variable " + root.get("var") + " does not exist.";
                    reports.add(createReport(root, message));
                }
                else if (!Objects.equals(varType, assignType)) {
                    String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                    reports.add(createReport(root, message));
                }
                break;
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
                String varName = child.get("value");
                varType = getVarType(varName,vars);
                reports.addAll(visitIdentifier(reports,child,vars,varType));
                break;
            case "BinaryOp":
                String operationType = getTypeOfBinaryOp(child,vars);
                if(operationType == "invalid") {
                    reports.addAll(visitBinaryOp(reports, child, vars));
                }
                else if (operationType != varType){
                    reports.add(createReport(child,"Assignment between a '" + varType + "' and a '" + operationType +"'."));
                }
                break;
            case "MethodCalls":
                visitMethodCalls(reports,child,table,vars);
                break;
            default:
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitAssignment : " + child.getKind());
                break;
            }
        return reports;
    }

    public List<Report> visitIdentifier(List<Report> reports, JmmNode root, List<Symbol> vars, String varType){
        System.out.println("IDENTIFIER --> " + root.getKind());
        String assignType = getVarType(root.get("value"),vars);
        if (assignType == null){
            reports.add(createReport(root,"Variable " + root.get("value") + " is not declared."));
        }
        else if (assignType != varType){
            reports.add(createReport(root,"Assignment between a '" + varType + "' and a '" + assignType + "'."));
        }
        return reports;
    }

    public List<Report> visitDeclaration(List<Report> reports, JmmNode root, Table table, String methodName){
        String varName = root.get("var");
        List<Symbol> vars = table.getLocalVariables(methodName);
        vars.addAll(table.getParameters(methodName));
        String className = table.getClassName(), methodClassName = root.getChildren().get(0).get("type");
        if (Objects.equals(methodClassName,className)){
            reports.add(createReport(root,"Creating an object of type '" + className + "' inside the class body."));
        }
        if (getVarType(varName,vars) == null){
            reports.add(createReport(root,"Variable " + varName + " is already declared."));
        }
        return reports;
    }

    public List<Report> visitMethodBody(List<Report> reports, JmmNode node, Table table, String methodName){
        List<Symbol> vars = table.getLocalVariables(methodName);
        vars.addAll(table.getParameters(methodName));
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
                default:
                    System.out.println("    visitMethod :: NODE TYPE NAO CONTABILIZADO " + child.getKind());
                    break;
            }
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
                    default:
                        System.out.println("    visitMethod :: NODE TYPE NAO CONTABILIZADO " + node.getKind());
                        break;
                    }
        }
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
                default:
                    System.out.println("semanticAnalysis :: NODE TYPE NAO CONTABILIZADO " + node.getKind());
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
        JmmSemanticsResult res = new JmmSemanticsResult(parserResult, visitor.getTable(), parserResult.getReports());
        JmmNode root = parserResult.getRootNode();
        List<Report> reports = new ArrayList<Report>();
        reports = eraseDuplicateReports(visitProgram(reports,root,table));
        res = new JmmSemanticsResult(parserResult, visitor.getTable(), reports);
        System.out.println("DETECTED ERRORS:");
        for(Report r : reports){
            System.out.println("Error (Line " + r.getLine() + "):" + r.getMessage());
        }
        return res;
    }
}

