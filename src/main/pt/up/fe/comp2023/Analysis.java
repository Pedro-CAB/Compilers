package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.comp.jmm.report.Report;
import pt.up.fe.comp.jmm.report.ReportType;
import pt.up.fe.comp.jmm.report.Stage;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Queue;


public class Analysis implements JmmAnalysis {
    public JmmSemanticsResult updateResult(JmmParserResult parserResult, TableVisitor visitor, List<Report> reports){
        List<Report> currentReports = parserResult.getReports();
        currentReports.addAll(reports);
        return new JmmSemanticsResult(parserResult, visitor.getTable(), currentReports);
    }

    public Report createReport(JmmNode node, String message){
        Integer startLine = Integer.parseInt(node.get("startLine")), startColumn = Integer.parseInt(node.get("startColumn"));
        System.out.println("ERROR (Line " + startLine + " : " + message);
        return new Report(ReportType.ERROR,Stage.SEMANTIC, startLine,startColumn,message);
    }

    public String getLocalVarType(String varName, List<Symbol> locals){
        for (Symbol var : locals){
            if (Objects.equals(var.getName(), varName)){return var.getType().toString();}
        }
        return null;
    }
    //TO BE IMPLEMENTED (NOT FINISHED)!
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
                if(Objects.equals(firstType, "boolean") || Objects.equals(secondType, "boolean")){return "invalid";}
                else return "boolean";
            case "+=":
            case "-=":
            case "*=":
            case "/=":
            case "%=":
                if(!Objects.equals(first.getKind(), "Identifier")) return "invalid";
            default:
                return "invalid";
        }
    }
    //TO BE IMPLEMENTED (NOT FINISHED)!
    public List<Report> visitBinaryOp(JmmSemanticsResult result, JmmNode node, List<Symbol> locals){
        List<Report> reports = result.getReports();
        System.out.println("Called visitBinaryOp");
        String operator = node.get("op");
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType,secondType;
        switch (first.getKind()) {
            case "Identifier" -> firstType = getLocalVarType(first.get("value"), locals);
            case "Boolean" -> firstType = "boolean";
            case "Integer" -> firstType = "integer";
            case "BinaryOp" -> {
                firstType = getTypeOfBinaryOp(first, locals);
                reports.addAll(visitBinaryOp(result, first, locals));
            }
            default -> {
                firstType = "invalid_type";
                System.out.println("NODE TYPE NAO CONTABILIZADO EM visitBinaryOp : " + node.getKind());
            }
        }
        switch (second.getKind()) {
            case "Identifier" -> secondType = getLocalVarType(second.get("value"), locals);
            case "Boolean" -> secondType = "boolean";
            case "Integer" -> secondType = "integer";
            case "BinaryOp" -> {
                secondType = getTypeOfBinaryOp(first, locals);
                reports.addAll(visitBinaryOp(result, first, locals));
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

    public List<Report> visitAssignment(JmmSemanticsResult result, JmmNode node, List<Symbol> locals){
        List<Report> reports = result.getReports();
        System.out.println("Called visitAssignment \n");
        String kind = node.getKind();
        System.out.println("Descendant of Assignement is " + kind);
            switch (node.getKind()){
                case "NewObject":
                    String varType = getLocalVarType(node.get("var"), locals);
                    String assignType = node.getChildren().get(0).get("type");
                    if (varType == null){
                        String message = "Variable " + node.get("var") + " does not exist.";
                        reports.add(createReport(node, message));
                    }
                    else if (!Objects.equals(varType, assignType)) {
                        String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                        reports.add(createReport(node, message));
                    }
                    break;
                case "Integer":
                    varType = getLocalVarType(node.get("var"), locals);
                    assignType = "int";
                    if (!Objects.equals(varType, assignType)) {
                        String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                        reports.add(createReport(node, message));
                    }
                    break;
                case "Boolean":
                    varType = getLocalVarType(node.get("var"), locals);
                    assignType = "boolean";
                    if (!Objects.equals(varType, assignType)) {
                        String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                        reports.add(createReport(node, message));
                    }
                    break;
                case "BinaryOp":
                    reports.addAll(visitBinaryOp(result,node, locals));
                    break;
                default:
                    System.out.println("NODE TYPE NAO CONTABILIZADO EM visitAssignment : " + node.getKind());
                    break;
            }
        return reports;
    }

    public List<Report> visitMethod(JmmSemanticsResult result, JmmNode root, Table table){
        List<Report> reports = result.getReports();
        System.out.println("Called visitMethod \n");
        Queue<JmmNode> queue = new LinkedList<>();
        queue.addAll(root.getChildren());
        String name = root.get("name"), returnType;
        Boolean isPrivate = Boolean.FALSE, isStatic = Boolean.FALSE;
        List<Symbol> parameters = table.getParameters(name);
        List<Symbol> localVariables = table.getLocalVariables(name);
        //String type = table.getReturnType(name).toString();
        while (queue.size() > 0){
                JmmNode node = queue.remove();
                System.out.println("Visiting Node " + node.getKind());
                String kind = node.getKind();
                switch(kind){
                    case "Argument":
                        //Por agora não é necessário explorar os argumentos
                        break;
                    case "Assignment":
                        visitAssignment(result,node,localVariables);
                        break;
                    case "Declaration":
                        //AAA
                        break;
                    case "MethodBody":
                        for (JmmNode child : node.getChildren()) queue.add(child);
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
                    case "Return":
                        reports.addAll(visitAssignment(result, root, localVariables));
                        break;
                    default:
                        System.out.println("NODE TYPE NAO CONTABILIZADO EM visitMethod : " + node.getKind());
                        break;
                    }
        }
        return reports;
    }

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){

        Table table = new Table();

        TableVisitor visitor = new TableVisitor(table);
        System.out.println("Called semanticAnalysis");

        //New Code Below:
        JmmSemanticsResult res = new JmmSemanticsResult(parserResult, visitor.getTable(), parserResult.getReports());
        JmmNode root = parserResult.getRootNode();
        Queue<JmmNode> queue = new LinkedList<>();
        queue.add(root);
        List<Report> reports = parserResult.getReports();
        while (queue.size() > 0){
                JmmNode node = queue.remove();
                System.out.println("Visiting Node " + node.getKind());
                //Se for um dos abaixo, explorar os nós abaixo deles
                switch(node.getKind()){
                    case "Program":
                    case "ClassDeclaration":
                    case "ClassName":
                    case "ClassBody":
                        queue.addAll(node.getChildren());
                        break;
                    case "ClassMethod":
                    case "Return":
                        reports.addAll(visitMethod(res, node, table));
                        break;
                    default:
                        System.out.println("NODE TYPE NAO CONTABILIZADO EM semanticAnalysis : " + node.getKind());
                        break;
                }
                //Se for um dos abaixo, ignorar
        }
        return res;
    }
}

