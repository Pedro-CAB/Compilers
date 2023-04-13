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
    public Report createReport(int line, int column, String message){
        return new Report(ReportType.ERROR,Stage.SEMANTIC,line,column,message);
    }

    public String getLocalVarType(String varName, List<Symbol> locals){
        for (Symbol var : locals){
            if (Objects.equals(var.getName(), varName)){return var.getType().toString();}
        }
        return null;
    }

    public JmmSemanticsResult visitBinaryOp(JmmSemanticsResult result, JmmNode root){
        return result;
    }

    public JmmSemanticsResult visitAssignment(JmmSemanticsResult result, JmmNode node, List<Symbol> locals){
        String kind = node.getKind();
        if (Objects.equals(kind, "NewObject")){
            String varType = getLocalVarType(node.get("var"), locals);
            String assignType = node.getChildren().get(0).get("type");
            if (!Objects.equals(varType, assignType)) {
                String message = "Assignment between a '" + varType + "' and a '" + assignType + "'.";
                List<Report> reports = result.getReports();
                reports.add(createReport(-1, -1, message));
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
                reports.add(createReport(-1, -1, message));
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
                reports.add(createReport(-1, -1, message));
                result = new JmmSemanticsResult((JmmParserResult) result.getRootNode(), result.getSymbolTable(), reports);
            }
            return result;
        }
        else if (Objects.equals(kind, "BinaryOp")){
            result = visitBinaryOp(result,node);
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
                if (kind == "Modifier" || kind == "Type" || kind == "Argument" || kind == "Declaration"){continue;}
                else if (kind == "Assignment"){
                    result = visitAssignment(result,root,localVariables);
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
        Queue<JmmNode> queue = new LinkedList<JmmNode>();
        queue.add(root);
        System.out.println("Atts:\n");
        System.out.println(root.getChildren().get(0).getChildren().get(0).get("value"));
        while (queue.size() > 0){
            for(JmmNode node : queue.remove().getChildren()){
                //Se for um dos abaixo, explorar os nós abaixo deles
                if (node.getKind() == "Program"
                        || node.getKind() == "ClassDeclaration"
                        || node.getKind() == "ClassName"
                        || node.getKind() == "ClassBody"){
                    queue.add(node);
                }
                //Se for um dos abaixo, ignorar
                else if (node.getKind() == "Modifier"){
                    continue;
                }
                //Verificações Semânticas feitas a partir deste ponto
                else if (node.getKind() == "ClassMethod"){
                    res = visitMethod(res,node, table);
                }
            }
        }

        return res;
    }
}

