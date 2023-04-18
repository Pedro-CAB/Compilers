package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.List;
import java.util.Objects;

/*
enum Actions {
    Storetemp,
    ReturnValue
}*/


public class OllirVisitor extends AJmmVisitor<String, String> {

    String ollirCode;

    SymbolTable symbolTable;

    public OllirVisitor(String ollirCode, SymbolTable symbolTable) {
        this.ollirCode = ollirCode;
        this.symbolTable = symbolTable;
    }
    int importIndex = 0;

    int fieldIndex = 0;

//    int tempIndex;

    public String getOllirCode() {
        return ollirCode;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Program", this::dealWithProgram);
        addVisit("ImportPackage", this::dealWithImport);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("ClassBody", this::dealWithClassBody);
        addVisit("ClassField", this::dealWithClassFields);
        addVisit("Method", this::dealWithMethod);
        addVisit("Assignment", this::dealWithAssignments);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("Method Invocation", this::dealWithMethodInvocation);
    }

    public String getType(Type type) {

        if (type.isArray()) ollirCode += ".array";

        if ("int".equals(type.getName())) {
            ollirCode += ".i32";
        } else if ("boolean".equals(type.getName())) {
            ollirCode += ".bool";
        } else if ("void".equals(type.getName())) {
            ollirCode += ".V";
        } else {
            ollirCode += "." + type.getName();
        }

        return "";
    }

    public static String getType(String type) {
        StringBuilder ollir = new StringBuilder();

        switch (type) {
            case "array" -> ollir.append(".array");
            case "int" -> ollir.append(".i32");
            case "bool" -> ollir.append(".bool");
            case "void" -> ollir.append(".V");
            default -> ollir.append(".").append(type);
        }

        return ollir.toString();
    }


    public String getParamType(String val_param, String method){

        for (Symbol param : symbolTable.getParameters(method)){
            if (Objects.equals(param.getName(), val_param))
                return getType(param.getType());
        }

        return "";
    }

    public String getOpType(String op){
        if(op.equals("&") || op.equals("|") || op.equals("^")
                || op.equals("&&") || op.equals("||") ||
                op.equals("<") || op.equals("<=")  || op.equals(">")
                || op.equals(">=")
        ){
            return op + ".bool ";
        }

        else if (op.equals("+")  || op.equals("-") || op.equals("*") ||
                op.equals("/") || op.equals("%")){
            return op + ".i32 ";
        }

        else
            return "";
    }

    private String dealWithProgram(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()) {
            s += visit(child, "");
        }

        return "";
    }


    private String dealWithImport(JmmNode jmmNode, String s){

        ollirCode += symbolTable.getImports().get(importIndex);

        importIndex ++;
        return "";

    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s){

        ollirCode += ".construct " + symbolTable.getClassName() + ".V {\n";

        ollirCode += "\tinvokespecial(this, \"<init>\").V;\n}\n";

        return "";
    }

    private String dealWithClassBody(JmmNode jmmNode, String s) {
        StringBuilder ret = new StringBuilder(s != null ? s : "");
        for (JmmNode child : jmmNode.getChildren()) {
            ret.append(visit(child, "\n\t"));
        }
        s = ret.toString();

        return s;
    }


    private String dealWithClassFields(JmmNode jmmNode, String s){

        Symbol field = symbolTable.getFields().get(fieldIndex);

        ollirCode += ".field private " + field.getName();

        String type = getType(field.getType());

        ollirCode += type + ";\n";

        fieldIndex++;

        return "";
    }

    //Method Structure
    private String dealWithMethod(JmmNode jmmNode, String s){

        for (String method: symbolTable.getMethods()){
            ollirCode += ".method public " + method + "(";

            List<Symbol> parameters = symbolTable.getParameters(method);

            int param_sz = parameters.size();

            if (param_sz == 0){
                ollirCode += ")";
            }
            else {
                for (Symbol param : parameters){
                    ollirCode += param.getName();
                    String type = getType(param.getType());

                    if (param_sz == 1){
                        ollirCode += type + ")";

                    }
                    else if (param_sz > 1){
                        ollirCode += type + ", ";
                    }

                    param_sz -= 1;
                }
            }


            String method_type = getType(symbolTable.getReturnType(method));

            ollirCode += method_type+ "{\n";

            for (JmmNode child: jmmNode.getChildren()){
                if (Objects.equals(child.getKind(), "Assignment"))
                    dealWithAssignments(child, s);
                else if (Objects.equals(child.getKind(), "Method Invocation"))
                    dealWithMethodInvocation(child, s);

                else if (Objects.equals(child.getKind(), "BinaryOp"))
                    dealWithBinaryOp(child, s);
            }

            ollirCode+= "}\n";

        }

        return "";
    }

    private String dealWithAssignments(JmmNode jmmNode, String s){

        for (Symbol local_var : symbolTable.getLocalVariables(jmmNode.get("Name"))){
            ollirCode += local_var.getName();
            String type = "";

            type = getType(local_var.getType());

            if (type.equals(".i32"))
                ollirCode += type + " :=" + type + "0.132;\n";
            else if (type.equals(".bool"))
                ollirCode += type + " :=" + type + "0.bool;\n";
            else
                ollirCode += type + " :=" + type + ";\n";
        }

        return "";
    }

    private String dealWithMethodInvocation(JmmNode jmmNode, String s){

        for (String method : symbolTable.getMethods()){
            if (Objects.equals(jmmNode.get("Name"), method)){
                ollirCode += method + "(" + jmmNode.get("value") +
                        getParamType(jmmNode.get("value"), method)
                        + symbolTable.getReturnType(method);
            }
        }
        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "value")){
                ollirCode += child.get("value") + getType(child.get("type"));
            }

            if (Objects.equals(child.getKind(), "op")){
                ollirCode += getOpType(child.get("op"));
            }
        }

        return "";
    }

}