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
    public JmmSemanticsResult addReport(JmmSemanticsResult result, JmmNode node, String message){
        Report report = createReport(node,message);
        List<Report> reports = result.getReports();
        reports.add(report);
        result = new JmmSemanticsResult((JmmParserResult) result.getRootNode(), result.getSymbolTable(), reports);
        return result;
    }

    public Report createReport(JmmNode node, String message){
        return new Report(ReportType.ERROR,Stage.SEMANTIC, Integer.parseInt(node.get("startLine")),Integer.parseInt(node.get("startColumn")),message);
    }

    public String getLocalVarType(String varName, List<Symbol> locals){
        for (Symbol var : locals){
            if (Objects.equals(var.getName(), varName)){return var.getType().toString();}
        }
        return null;
    }
    //TO BE IMPLEMENTED (NOT FINISHED)!
    public String getTypeOfBinaryOp(JmmNode node, List<Symbol> locals){
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
    public JmmSemanticsResult visitBinaryOp(JmmSemanticsResult result, JmmNode node, List<Symbol> locals){
        String operator = node.get("op");
        JmmNode first = node.getChildren().get(0), second = node.getChildren().get(1);
        String firstType,secondType;
        switch (first.getKind()) {
            case "Identifier" -> firstType = getLocalVarType(first.get("value"), locals);
            case "Boolean" -> firstType = "boolean";
            case "Integer" -> firstType = "integer";
            case "BinaryOp" -> {
                firstType = getTypeOfBinaryOp(first, locals);
                result = visitBinaryOp(result, first, locals);
            }
            default -> firstType = "invalid_type";
        }
        switch (second.getKind()) {
            case "Identifier" -> secondType = getLocalVarType(second.get("value"), locals);
            case "Boolean" -> secondType = "boolean";
            case "Integer" -> secondType = "integer";
            case "BinaryOp" -> {
                secondType = getTypeOfBinaryOp(first, locals);
                result = visitBinaryOp(result, first, locals);
            }
            default -> secondType = "invalid_type";
        }
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
                if (!Objects.equals(firstType, "integer") || !Objects.equals(secondType, "integer")) {
                    result = addReport(result, node, "Cannot use '" + operator + "' to compare between '" + firstType + "' and '" + secondType + "'.");
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
                            result = addReport(result, first, "Expected a variable, found '" + first.get("value"));
                        } else if (!Objects.equals(firstType, secondType)) {
                            result = addReport(result, first, "Incompatible types. Cannot use '" + operator + "' with '" + firstType + "' and '" + secondType + "'.");
                        }
                        if (!Objects.equals(secondType, "invalid")) {
                            if (!Objects.equals(secondType, "integer")) {
                                result = addReport(result, second, "Trying to increment '" + firstType + "' to '" + secondType + "'.");
                            }
                        }
                }
                break;
            //Logic Operators
            case "&&":
            case "||":
                if(!Objects.equals(firstType, "boolean") || !Objects.equals(secondType, "boolean")){
                    result = addReport(result,node,"Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'boolean'.");
                }
            //Bitwise Operators
            case "&":
            case "|":
            case "^":
                if(firstType != "integer" || secondType != "integer"){
                    result = addReport(result,node,"Cannot use '" + operator + "' between '" + firstType + "' and '" + secondType + "'. Both should be 'int'.");
                }
        }
        return result;
    }

    public JmmSemanticsResult visitAssignment(JmmSemanticsResult result, JmmNode node, List<Symbol> locals){
        String kind = node.getKind();
        if (Objects.equals(kind, "NewObject")){
            String varType = getLocalVarType(node.get("var"), locals);
            String assignType = node.getChildren().get(0).get("type");
            if (varType == null){
                String message = "Variable " + node.get("var") + " does not exist.";
                List<Report> reports = result.getReports();
                reports.add(createReport(node, message));
                result = new JmmSemanticsResult((JmmParserResult) result.getRootNode(), result.getSymbolTable(), reports);
            }
            else if (!Objects.equals(varType, assignType)) {
                String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                List<Report> reports = result.getReports();
                reports.add(createReport(node, message));
                result = new JmmSemanticsResult((JmmParserResult) result.getRootNode(), result.getSymbolTable(), reports);
            }
            return result;
        }
        else if (Objects.equals(kind,"Integer")){
            String varType = getLocalVarType(node.get("var"), locals);
            String assignType = "int";
            if (!Objects.equals(varType, assignType)) {
                String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                List<Report> reports = result.getReports();
                reports.add(createReport(node, message));
                result = new JmmSemanticsResult((JmmParserResult) result.getRootNode(), result.getSymbolTable(), reports);
            }
            return result;
        }
        else if (Objects.equals(kind,"Boolean")){
            String varType = getLocalVarType(node.get("var"), locals);
            String assignType = "boolean";
            if (!Objects.equals(varType, assignType)) {
                String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                List<Report> reports = result.getReports();
                reports.add(createReport(node, message));
                result = new JmmSemanticsResult((JmmParserResult) result.getRootNode(), result.getSymbolTable(), reports);
            }
            return result;
        }
        else if (Objects.equals(kind, "BinaryOp")){
            result = visitBinaryOp(result,node, locals);
            return result;
        }
        return result;
    }

    public JmmSemanticsResult visitMethod(JmmSemanticsResult result, JmmNode root, Table table){
        Queue<JmmNode> queue = new LinkedList<>();
        queue.add(root);
        String name = root.get("value");
        List<Symbol> parameters = table.getParameters(name);
        List<Symbol> localVariables = table.getLocalVariables(name);
        String type = table.getReturnType(name).toString();
        while (queue.size() > 0){
            for (JmmNode node : queue.remove().getChildren()){
                String kind = node.getKind();
                if (!(Objects.equals(kind, "Modifier") || Objects.equals(kind, "Type") || Objects.equals(kind, "Argument") || Objects.equals(kind, "Declaration"))) {
                    if (Objects.equals(kind, "Assignment")) {
                        result = visitAssignment(result, root, localVariables);
                    }
                }
            }
        }
        return result;
    }

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){

        Table table = new Table();

        TableVisitor visitor = new TableVisitor(table);
        //System.out.println(visitor.visit(parserResult.getRootNode(), ""));

        //New Code Below:
        JmmSemanticsResult res = new JmmSemanticsResult(parserResult, visitor.getTable(), parserResult.getReports());
        JmmNode root = parserResult.getRootNode();
        Queue<JmmNode> queue = new LinkedList<>();
        queue.add(root);
        System.out.println("Atts:\n");
        System.out.println(root.getChildren().get(0).getChildren().get(0).get("value"));
        while (queue.size() > 0){
            for(JmmNode node : queue.remove().getChildren()){
                //Se for um dos abaixo, explorar os nós abaixo deles
                if (Objects.equals(node.getKind(), "Program")
                        || Objects.equals(node.getKind(), "ClassDeclaration")
                        || Objects.equals(node.getKind(), "ClassName")
                        || Objects.equals(node.getKind(), "ClassBody")){
                    queue.add(node);
                }
                //Se for um dos abaixo, ignorar
                if (!Objects.equals(node.getKind(), "Modifier")) {
                    //Verificações Semânticas feitas a partir deste ponto
                    if (Objects.equals(node.getKind(), "ClassMethod")) {
                        res = visitMethod(res, node, table);
                    }
                }
            }
        }

        return res;
    }
}

