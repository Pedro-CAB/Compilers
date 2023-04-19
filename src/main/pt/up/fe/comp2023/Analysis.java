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
        System.out.println("ERROR (Line " + startLine + "): " + message);
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



    public String getLocalVarType(String varName, List<Symbol> locals){
        for (Symbol var : locals){
            if (Objects.equals(var.getName(), varName)){
                if (var.getType().isArray())
                    return var.getType().getName() + "_array";
                else
                    return var.getType().getName();
            }
        }
        return null;
    }

    public String getTypeOfBinaryOp(JmmNode node, List<Symbol> locals){
        System.out.println("Called getTypeOfBinaryOp");
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType, secondType;
        firstType = switch (first.getKind()) {
            case "Identifier" -> getLocalVarType(first.get("value"), locals);
            case "Boolean" -> "boolean";
            case "Integer" -> "integer";
            case "BinaryOp" -> getTypeOfBinaryOp(first, locals);
            default -> "invalid_type";
        };
        secondType = switch (second.getKind()) {
            case "Identifier" -> getLocalVarType(second.get("value"), locals);
            case "Boolean" -> "boolean";
            case "Integer" -> "integer";
            case "BinaryOp" -> getTypeOfBinaryOp(first, locals);
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

    public List<Report> visitBinaryOp(List<Report> reports, JmmNode node, List<Symbol> locals){
        System.out.println("Called visitBinaryOp");
        String operator = node.get("op");
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType,secondType;
        switch (first.getKind()) {
            case "Identifier" -> {
                firstType = getLocalVarType(first.get("value"), locals);
                reports.addAll(visitIdentifier(reports, second, locals,firstType));
            }
            case "Boolean" -> firstType = "boolean";
            case "Integer" -> firstType = "integer";
            case "BinaryOp" -> {
                firstType = getTypeOfBinaryOp(first, locals);
                reports.addAll(visitBinaryOp(reports, first, locals));
            }
            default -> {
                firstType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
        switch (second.getKind()) {
            case "Identifier" -> {
                secondType = getLocalVarType(second.get("value"), locals);
                reports.addAll(visitIdentifier(reports, second, locals,secondType));
            }
            case "Boolean" -> secondType = "boolean";
            case "Integer" -> secondType = "integer";
            case "BinaryOp" -> {
                secondType = getTypeOfBinaryOp(first, locals);
                reports.addAll(visitBinaryOp(reports, first, locals));
            }
            default -> {
                secondType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
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
                System.out.println(firstType + " " + node.get("op") + " " + secondType);
                if (!Objects.equals(firstType, "integer") || !Objects.equals(secondType, "integer")) {
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
                if(firstType != "integer" || secondType != "integer"){
                    reports.add(createReport(node,"Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'int'."));
                }
            default:
                System.out.println("OP TYPE NAO CONTABILIZADO EM visitBinaryOP : " + node.get("op"));
        }
        return reports;
    }

    public List<Report> visitReturn(List<Report> reports, JmmNode root, Table table, String methodName){
        JmmNode child = root.getChildren().get(0);
        List<Symbol> locals = table.getLocalVariables(methodName);
        String returnType = table.getReturnType(methodName).getName();
        switch(child.getKind()){
            case "Integer":
                if (returnType != "integer"){
                    reports.add(createReport(child,"Method " + methodName + " should return" + returnType + " but is returning 'integer'."));
                }
                break;
            case "Boolean":
                if (returnType != "boolean"){
                    reports.add(createReport(child,"Method " + methodName + " should return" + returnType + " but is returning 'boolean'."));
                }
                break;
            case "Identifier":
                String varName = child.get("value");
                String varType = getLocalVarType(varName,locals);
                visitIdentifier(reports,child,locals,varType);
                break;
            case "BinaryOp":
                String operationType = getTypeOfBinaryOp(child,locals);
                if (operationType == "invalid"){
                    reports.addAll(visitBinaryOp(reports,child,locals));
                }
                else if (operationType != returnType){
                    reports.add(createReport(child,"Method " + methodName + " should return '" + returnType + "' but is returning '" + operationType + "'."));
                }
                break;
            case "MethodCalls":
                visitMethodCalls(reports,child,table);
                break;
        }
        return reports;
    }

    public List<Report> visitMethodCalls(List<Report> reports, JmmNode root, Table table){
        String className = root.getChildren().get(0).get("value");
        List<String> imports = table.getImports();
        if(!imports.contains(className)){
            reports.add(createReport(root,"Class " + className + " doesn't exist. Maybe you should have imported it?"));
        }
        return reports;
    }

    public List<Report> visitAssignment(List<Report> reports, JmmNode root, Table table, String methodName){
        System.out.println("Called visitAssignment");
        JmmNode child = root.getChildren().get(0);
        List<Symbol> locals = table.getLocalVariables(methodName);
        System.out.println("LOCALS ---->" + locals);
        String varType = getLocalVarType(root.get("var"), locals);
        System.out.println("VARTYPE ---->" + varType);
        String kind = child.getKind();
        System.out.println("        visitAssignment :: Descendants of" + root.getKind() + " are " + root.getChildren());
        System.out.println("        visitAssignment :: Current Descendant of Assignment is " + kind);
        switch (child.getKind()){
            case "Modifier":
                System.out.println("???? -> " + child);
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
                    System.out.println(varType + " == " + assignType + "??");
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
                varType = getLocalVarType(varName,locals);
                visitIdentifier(reports,child,locals,varType);
                break;
            case "BinaryOp":
                String operationType = getTypeOfBinaryOp(child,locals);
                if(operationType == "invalid") {
                    reports.addAll(visitBinaryOp(reports, child, locals));
                }
                else if (operationType != varType){
                    reports.add(createReport(child,"Assignment between a '" + varType + "' and a '" + operationType +"'."));
                }
                break;
            case "MethodCalls":
                visitMethodCalls(reports,child,table);
                break;
            default:
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitAssignment : " + child.getKind());
                break;
            }
        return reports;
    }

    public List<Report> visitIdentifier(List<Report> reports, JmmNode root, List<Symbol> locals, String varType){
        String assignType = getLocalVarType(root.get("value"),locals);
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
        List<Symbol> locals = table.getLocalVariables(methodName);
        if (getLocalVarType(varName,locals) == null){
            reports.add(createReport(root,"Variable " + varName + " is already declared."));
        }
        return reports;
    }

    public List<Report> visitMethodBody(List<Report> reports, JmmNode node, Table table, String methodName){
        for(JmmNode child : node.getChildren()){
            switch(child.getKind()) {
                case "Assignment":
                    reports.addAll(visitAssignment(reports, child, table, methodName));
                    break;
                case "Declaration":
                    //visitDeclaration(result,child,localVariables);
                    break;
                case "MethodCalls":
                    visitMethodCalls(reports,child,table);
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
        List<Symbol> parameters = table.getParameters(name);
        List<Symbol> localVariables = table.getLocalVariables(name);
        while (queue.size() > 0){
            System.out.println("Queue :" + queue);
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

