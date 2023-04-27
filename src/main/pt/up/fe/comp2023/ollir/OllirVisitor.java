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
        addVisit("Declaration", this::dealWithClassDeclaration);
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
        } else if ("string".equals(type.getName())) {
            ollir.append(".string");
        } else if ("void".equals(type.getName())) {
            ollir.append(".V");
        } else {
            ollir.append(".").append(type.getName());
        }
        return ollir.toString();
    }

    public String getParamType(String val_param, String method) {

        for (Symbol param : symbolTable.getParameters(method)) {
            if (Objects.equals(param.getName(), val_param))
                return getType(param.getType());
        }

        return "";
    }

    public String getFieldType(String field) {

        for (Symbol f : symbolTable.getFields()) {
            if (Objects.equals(f.getName(), field))
                return getType(f.getType());
        }

        return "";
    }

    public String getLocalType(String local, String method) {

        for (Symbol l : symbolTable.getLocalVariables(method)) {
            if (Objects.equals(l.getName(), local))
                return getType(l.getType());
        }

        return "";
    }

    private String findType(JmmNode node, String method) {

        String val = node.get("value");

        switch (node.getKind()) {
            case "Integer" -> {
                return ".i32";
            }
            case "Boolean" -> {
                return ".bool";
            }
            case "String" -> {
                return ".string";
            }
            case "Void" -> {
                return ".V";
            }
            default -> {
            }
        }

        return getParamType(val, method) + getFieldType(val) + getLocalType(val, method);
    }


    private String getOptype(String op) {
        if (op.equals("+") | op.equals("-") | op.equals("*") | op.equals("/") | op.equals("%")) {
            return ".i32";
        } else if (op.equals("&&") | op.equals("||") | op.equals("^")) {
            return ".bool";
        }
        return "";
    }

    private boolean isInvokeStatic(String method_sup) {
        for (String imp : symbolTable.getImports()) {

            int dot_index = imp.indexOf('.');

            if (dot_index > 0) {
                return imp.substring(7, dot_index).equals(method_sup);

            } else {
                if (imp.substring(7, imp.length() - 2).equals(method_sup))
                    return true;

            }
        }
        return false;
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {

        for (JmmNode child : jmmNode.getChildren()) {
            s += visit(child, "");
        }

        ollirCode += "}\n";
        return "";
    }

    private String dealWithImport(JmmNode jmmNode, String s) {

        ollirCode += symbolTable.getImports().get(importIndex);

        importIndex++;
        return "";

    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {

        String sup = symbolTable.getSuper();

        if (Objects.equals(sup, "")) {
            ollirCode += symbolTable.getClassName() + "{\n";
        } else {
            ollirCode += symbolTable.getClassName() + " extends " + sup + "{\n";
        }

        for (Symbol field : symbolTable.getFields()) {

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


    private String dealWithClassFields(JmmNode jmmNode, String s) {
        return "";
    }

    //Method Structure
    private String dealWithMethod(JmmNode jmmNode, String s) {

        String method = symbolTable.getMethods().get(methodIndex);

        if (method.equals("main")) {
            ollirCode += "\t.method public static " + method + "(";
        } else {
            ollirCode += "\t.method public " + method + "(";
        }

        List<Symbol> parameters = symbolTable.getParameters(method);

        int param_sz = parameters.size();

        if (param_sz == 0) {
            ollirCode += ")";
        } else {
            for (Symbol param : parameters) {
                ollirCode += param.getName();
                String type = getType(param.getType());

                if (param_sz == 1) {
                    ollirCode += type + ")";

                } else if (param_sz > 1) {
                    ollirCode += type + ", ";
                }

                param_sz -= 1;
            }
        }


        String method_type = getType(symbolTable.getReturnType(method));

        ollirCode += method_type + "{\n";

        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "MethodBody")) {
                for (JmmNode c : child.getChildren()) {
                    if (Objects.equals(c.getKind(), "Return")) {
                        dealWithReturn(c, method);
                    } else if (Objects.equals(c.getKind(), "Declaration"))
                        dealWithDeclaration(c, method);
                    else if (Objects.equals(c.getKind(), "Assignment"))
                        dealWithAssignments(c, method);
                    else if (Objects.equals(c.getKind(), "MethodCalls"))
                        dealWithMethodInvocation(c, method);
                    else if (Objects.equals(c.getKind(), "BinaryOp"))
                        dealWithBinaryOp(c, method);
                    else if (Objects.equals(c.getKind(), "ExprStmt")) {
                        dealWithExprStmt(c, method);
                    }
                }
            }
        }

        ollirCode += "\t}\n";

        methodIndex++;

        return "";
    }

    private String dealWithExprStmt(JmmNode jmmNode, String s) {

        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "Assignment"))
                dealWithAssignments(child, s);
            else if (Objects.equals(child.getKind(), "MethodCalls"))
                dealWithMethodInvocation(child, s);
            else if (Objects.equals(child.getKind(), "BinaryOp"))
                dealWithBinaryOp(child, s);
            else if (Objects.equals(child.getKind(), "ExprStmt")) {
                dealWithExprStmt(child, s);
            }
        }

        return "";
    }


    private String dealWithReturn(JmmNode jmmNode, String method) {

        Type ret = symbolTable.getReturnType(method);

        JmmNode child = jmmNode.getJmmChild(0);

        if (!Objects.equals(child.getKind(), "BinaryOp")) {
            ollirCode += "\t\tret" + getType(ret) + " " + child.get("value") + getType(ret) + ";\n";
            localIndex = 0;
        } else {
            dealWithBinaryOp(child, method);
            ollirCode += "\t\tret" + getType(ret) + " " + "t" + tempIndex + getType(ret) + ";\n";
            tempIndex++;

        }
        return "";
    }

    private String dealWithDeclaration(JmmNode jmmNode, String method) {

        Symbol local_var = symbolTable.getLocalVariables(method).get(localIndex);

        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "NewObject")) {
                String type = getType(local_var.getType());
                ollirCode += "\t\tinvokespecial(" + local_var.getName() + type + ",\"<init>\")" + type + ";\n";
            }
        }

        return "";
    }


    private String dealWithAssignments(JmmNode jmmNode, String s) {

        Symbol local_var = symbolTable.getLocalVariables(s).get(localIndex);

        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "BinaryOp")) {
                dealWithBinaryOp(child, s);
                String t = getType(local_var.getType());
                ollirCode += "\t\t" + local_var.getName() + t + " :=" + t + " t" + tempIndex + t + ";\n";
                return "";
            }
        }

        ollirCode += "\t\t" + local_var.getName();
        String type;

        type = getType(local_var.getType());

        switch (type) {
            case ".i32" -> {
                String val;
                if (!Objects.equals(jmmNode.getJmmChild(0).getKind(), "MethodCalls")) {
                    val = jmmNode.getJmmChild(0).get("value");
                    ollirCode += type + " :=" + type + " " + val + type + ";\n";
                } else {
                    ollirCode += type + " :=" + type + " ";
                    dealWithMethodInvocation(jmmNode.getJmmChild(0), s);
                }
            }
            case ".bool" -> ollirCode += type + " :=" + type + " 0.bool;\n";
            case ".array.i32" -> ollirCode += type + ":=" + type + " new(array)" + type + ";\n";
            default -> {
            }
        }

        localIndex++;

        if (localIndex == symbolTable.getLocalVariables(s).size()) {
            localIndex = 0;
        }

        return "";
    }

    private String dealWithMethodInvocation(JmmNode jmmNode, String method) {

        String method_name = "";
        String method_sup = "";
        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "MethodCall")) {

                method_name = child.get("methodName");
            } else if (Objects.equals(child.getKind(), "Identifier")) {
                method_sup = child.get("value");

            }
            if (!method_sup.equals("") && !method_name.equals("")) {
                if (!isInvokeStatic(method_sup)) {
                    if (child.getNumChildren() > 0) {
                        String method_arg = child.getJmmChild(0).getJmmChild(0).get("value");

                        String arg_type = findType(child.getJmmChild(0).getJmmChild(0), method);
                        if (symbolTable.getMethods().contains(method_name)) {
                            ollirCode += "\t\tinvokevirtual(" + method_sup + ", \"" + method_name + "\", " +
                                    method_arg + arg_type + ")" + getType(
                                    symbolTable.getReturnType(method_name)) + ";\n";
                        } else if (Objects.equals(jmmNode.getJmmParent().getKind(), "Assignment")) {
                            ollirCode += "invokevirtual(" + method_sup + ", \"" + method_name + "\", " +
                                    method_arg + arg_type + ")";
                        }
                    } else {
                        if (symbolTable.getMethods().contains(method_name)) {
                            ollirCode += "\t\tinvokevirtual(" + method_sup + ", \"" + method_name + "\", " +
                                    ")" + getType(
                                    symbolTable.getReturnType(method_name)) + ";\n";
                        } else if (Objects.equals(jmmNode.getJmmParent().getKind(), "Assignment")) {
                            ollirCode += "invokevirtual(" + method_sup + ", \"" + method_name + "\")";
                        }
                    }
                } else {
                    if (child.getNumChildren() > 0) {
                        String method_arg = child.getJmmChild(0).getJmmChild(0).get("value");

                        String arg_type = findType(child.getJmmChild(0).getJmmChild(0), method);
                        if (symbolTable.getMethods().contains(method_name)) {
                            ollirCode += "\t\tinvokestatic(" + method_sup + ", " + "\"" + method_name + "\", " +
                                    method_arg + arg_type + ").V;\n";
                        } else if (Objects.equals(jmmNode.getJmmParent().getKind(), "Assignment")) {
                            ollirCode += "invokestatic(" + method_sup + ", " + "\"" + method_name + "\", " +
                                    method_arg + arg_type + ").V;\n";
                        }
                    } else {
                        if (symbolTable.getMethods().contains(method_name)) {
                            ollirCode += "\t\tinvokestatic(" + method_sup + ", " + "\"" + method_name + "\"" + ").V;\n";
                        } else if (Objects.equals(jmmNode.getJmmParent().getKind(), "Assignment")) {
                            ollirCode += "\t\tinvokestatic(" + method_sup + ", " + "\"" + method_name + "\"" + ").V;\n";
                        }
                    }
                }
            }
        }

        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String method){

        for (int i = 0; i < jmmNode.getNumChildren(); i++){
            JmmNode child = jmmNode.getJmmChild(i);
            if (Objects.equals(child.getKind(), "BinaryOp")){
                dealWithBinaryOp(child, method);
            }
            else{
                String val_type = findType(child, method);

                if (i == jmmNode.getNumChildren()-1){
                    if (tempIndex > 1){
                        ollirCode += "\t\tt" + tempIndex + val_type + " :=" + val_type + " ";
                        tempIndex--;
                        ollirCode += "t" + tempIndex + val_type + " " + child.getJmmParent().get("op") + val_type + " ";
                    }
                    ollirCode+= child.get("value") + val_type + ";\n";
                    tempIndex++;
                }
                else{
                    if (Objects.equals(child.getJmmParent().getKind(), "BinaryOp")){
                        String op_type = getOptype(jmmNode.get("op"));
                        ollirCode += "\t\tt" + tempIndex + op_type + " :=" + op_type + " ";
                        if (tempIndex == 1){
                            ollirCode += child.get("value") + val_type + " "+ child.getJmmParent().get("op") + val_type + " ";
                        }

                        else{
                            tempIndex--;
                            ollirCode += "t" + tempIndex + val_type + " " + child.getJmmParent().get("op") + val_type + " ";
                            tempIndex++;
                        }
                    }

                }
            }
        }

        return "";
    }

}