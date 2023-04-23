package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.*;

public class OllirVisitor extends AJmmVisitor<String, String> {

    String ollirCode;

    SymbolTable symbolTable;

    public OllirVisitor(String ollirCode, SymbolTable symbolTable) {
        this.ollirCode = ollirCode;
        this.symbolTable = symbolTable;
    }
    int importIndex = 0;
    int localIndex = 0;

    int methodIndex = 0;

    int tempIndex = 1;

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
        addVisit("MethodCalls", this::dealWithMethodInvocation);
        addVisit("ExprStmt", this::dealWithExprStmt);
        addVisit("Return", this::dealWithReturn);
    }

    public String getType(Type type) {

        StringBuilder ollir = new StringBuilder();

        if (type.isArray()) ollir.append(".array");

        if ("int".equals(type.getName())) {
            ollir.append(".i32");
        } else if ("boolean".equals(type.getName())) {
            ollir.append(".bool");
        } else if ("void".equals(type.getName())) {
            ollir.append(".V");
        } else {
            ollir.append(".").append(type.getName());
        }

        return ollir.toString();
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

    private boolean isInvokeVirtual(String method_name){

        return symbolTable.getMethods().contains(method_name);
    }

    private String dealWithProgram(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()) {
            s += visit(child, "");
        }

        ollirCode += "}\n";
        return "";
    }

    private String dealWithImport(JmmNode jmmNode, String s){

        ollirCode += symbolTable.getImports().get(importIndex);

        importIndex ++;
        return "";

    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s){

        String sup = symbolTable.getSuper();

        if (Objects.equals(sup, "")){
            ollirCode += symbolTable.getClassName() + "{\n";
        }
        else{
            ollirCode += symbolTable.getClassName() + " extends " + sup + "{\n";
        }

        for (Symbol field : symbolTable.getFields()){



            ollirCode += "\t.field private " + field.getName();

            String type = getType(field.getType());

            ollirCode += type + ";\n";
        }

        ollirCode += "\t.construct " + symbolTable.getClassName() + "()" + ".V {\n";

        ollirCode += "\t\tinvokespecial(this, \"<init>\").V;\n\t}\n";

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
        return "";
    }

    //Method Structure
    private String dealWithMethod(JmmNode jmmNode, String s){

        String method = symbolTable.getMethods().get(methodIndex);

        if (method.equals("main")){
            ollirCode += "\t.method public static " + method + "(";
        }
        else{
            ollirCode += "\t.method public " + method + "(";
        }

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

        for (JmmNode child :jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "MethodBody")){
                for (JmmNode c : child.getChildren()){
                    if (Objects.equals(c.getKind(), "Return")){
                        dealWithReturn(c, method);
                    }
                    else if (Objects.equals(c.getKind(), "Assignment"))
                        dealWithAssignments(c, method);
                    else if (Objects.equals(c.getKind(), "MethodCalls"))
                        dealWithMethodInvocation(c, method);

                    else if (Objects.equals(c.getKind(), "BinaryOp"))
                        dealWithBinaryOp(c, s);
                    else if (Objects.equals(c.getKind(), "ExprStmt")){
                        dealWithExprStmt(c,method);
                    }
                }
            }
        }

        ollirCode+= "\t}\n";

        methodIndex++;

        return "";
    }

    private String dealWithExprStmt(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "Assignment"))
                dealWithAssignments(child, s);
            else if (Objects.equals(child.getKind(), "MethodCalls"))
                dealWithMethodInvocation(child, s);
            else if (Objects.equals(child.getKind(), "BinaryOp"))
                dealWithBinaryOp(child, s);
            else if (Objects.equals(child.getKind(), "ExprStmt")){
                dealWithExprStmt(child,s);
            }
        }

        return "";
    }


    private String dealWithReturn(JmmNode jmmNode, String method){

        Type ret = symbolTable.getReturnType(method);

        JmmNode child = jmmNode.getJmmChild(0);

        if (!Objects.equals(child.getKind(), "BinaryOp")){
            ollirCode += "\t\tret" + getType(ret) + " " + child.get("value") + getType(ret) + ";\n";
            localIndex = 0;
        }
        else {

            String temp = "t" + tempIndex + getType(ret);
            String exp = temp + " :=" + getType(ret)+ " ";
            exp += dealWithBinaryOp(child, method);

            exp += ";\n";

            ollirCode += "\t\t" + exp;
            ollirCode += "\t\tret" + getType(ret) + " " + temp + ";\n";
            tempIndex++;

        }
        return "";
    }


    private String dealWithAssignments(JmmNode jmmNode, String s){

        Symbol local_var = symbolTable.getLocalVariables(s).get(localIndex);


        ollirCode += "\t\t" + local_var.getName();
        String type;

        type = getType(local_var.getType());

        if (type.equals(".i32"))
            ollirCode += type + " :=" + type + " 0.i32;\n";
        else if (type.equals(".bool"))
            ollirCode += type + " :=" + type + " 0.bool;\n";
        else if (type.equals(".array.i32")) {
            ollirCode += type + ":=" + type + "new(array)" + type+ ";\n";
        } else
            ollirCode += type + " :=" + type + ";\n";

        localIndex++;

        if (localIndex == symbolTable.getLocalVariables(s).size()){
            localIndex = 0;
        }

        return "";
    }

    private String dealWithMethodInvocation(JmmNode jmmNode, String method){

        JmmNode method_aux = jmmNode.getJmmChild(1);

        String method_name = method_aux.get("methodName");
        String method_arg = "";

        String method_sup;

        String arg_type = "";
        if (method_aux.getNumChildren() > 0) {

            JmmNode temp = method_aux.getJmmChild(0);
            method_arg = temp.getJmmChild(0).get("value");

            method_sup = jmmNode.getJmmChild(0).get("value");

            for (Symbol symbol : symbolTable.getLocalVariables(method)){
                if (Objects.equals(method_arg, symbol.getName())){
                    arg_type += getType(symbol.getType());
                }
            }

            if (arg_type.equals("") && Objects.equals(temp.getJmmChild(0).getKind(), "Integer")){
                arg_type += ".i32";
            }

            ollirCode += "\t\tinvokestatic(" + method_sup + ", "+ "\"" + method_name + "\", " +
                    method_arg + arg_type + ").V;\n";

        }
        else if (isInvokeVirtual(method_name)){

            if (symbolTable.getParameters(method_name).size() > 0){
                ollirCode += "\t\tinvokevirtual(this, " + "\"" + method_name + "\", " +
                        method_arg + arg_type + ").V;\n";
            }
            else{
                ollirCode += "\t\tinvokevirtual(this, " + "\"" + method_name + "\"" + ")"+ getType(
                        symbolTable.getReturnType(method_name)) + ";\n";
            }
        }
        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String method){

        String s = "";
        int i = 0;

        JmmNode child;
        for (i = 0; i < jmmNode.getNumChildren(); i++){
            child = jmmNode.getJmmChild(i);

            if (Objects.equals(child.getKind(), "BinaryOp")){
                dealWithBinaryOp(child, method);
                i++;
            }

            Symbol local = symbolTable.getLocalVariables(method).get(localIndex);
            child = jmmNode.getJmmChild(i);
            if (local.getName().equals(child.get("value")) && i == jmmNode.getNumChildren()- 1){
                s += child.get("value") + getType(local.getType());
            }
            else{
                s += child.get("value") + getType(local.getType()) + " " + getOpType(jmmNode.get("op"));
            }
            localIndex++;


        }
        return s;
    }

}