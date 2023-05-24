package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import javax.management.ObjectName;
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

    int dollarIndex = 1;

    int ifIndex = 0;

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
        addVisit("ArrayAssignment", this::dealWithArrayAssignments);
        addVisit("ArrayAccess", this::dealWithArrayAccess);
        addVisit("BinaryOp", this::dealWithBinaryOp);
        addVisit("MethodCalls", this::dealWithMethodInvocation);
        addVisit("While", this::dealWithWhile);
        addVisit("IfElse", this::dealWithIfElse);
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

    public String getParamType(String val_param, String method){

        for (Symbol param : symbolTable.getParameters(method)){
            if (Objects.equals(param.getName(), val_param))
                return getType(param.getType());
        }

        return "";
    }

    public String getFieldType(String field){

        for (Symbol f : symbolTable.getFields()){
            if (Objects.equals(f.getName(), field))
                return getType(f.getType());
        }

        return "";
    }

    public String getLocalType(String local, String method){

        for (Symbol l : symbolTable.getLocalVariables(method)){
            if (Objects.equals(l.getName(), local))
                return getType(l.getType());
        }

        return "";
    }

    private String findType(JmmNode node, String method){

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

        if (!getParamType(val, method).equals("")){
            return getParamType(val, method);
        }

        return  getFieldType(val) + getLocalType(val, method);
    }

    String findArray(String method){
        for (Symbol local_var : symbolTable.getLocalVariables(method)){
            if (local_var.getType().isArray()){
                return local_var.getName() + getType(local_var.getType());
            }
        }

        for (Symbol field : symbolTable.getFields()){
            if (field.getType().isArray()){
                return field.getName() + getType(field.getType());
            }
        }

        for (Symbol param : symbolTable.getParameters(method)){
            if (param.getType().isArray()){
                return param.getName() + getType(param.getType());
            }
        }


        return "";
    }



    private String getOptype(String op){
        if (op.equals("+") || op.equals("-") || op.equals("*") || op.equals("/") || op.equals("%")){
            return ".i32";
        }
        else if (op.equals("&&") || op.equals("||") | op.equals("^") || op.equals("<") || op.equals(">") ||
                op.equals("<=") || op.equals(">=")){
            return ".bool";
        }
        return "";
    }

    private String getReverse(String op){
        return switch (op) {
            case ">" -> "<=";
            case "<" -> ">=";
            case ">=" -> "<";
            case "<=" -> ">";
            default -> "";
        };
    }

    private Symbol getLocalVar(String method, String target){

        for (Symbol local_var : symbolTable.getLocalVariables(method)) {

            if (local_var.getName().equals(target)){
                return local_var;
            }
        }
        return null;
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

        ollirCode += method_type+ " {\n";

        for (JmmNode child :jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "MethodBody")){
                for (JmmNode c : child.getChildren()){
                    if (Objects.equals(c.getKind(), "Return")){
                        dealWithReturn(c, method);
                    }
                    else if (Objects.equals(c.getKind(), "Assignment"))
                        dealWithAssignments(c, method);
                    else if (Objects.equals(c.getKind(), "ArrayAssignment"))
                        dealWithArrayAssignments(c, method);
                    else if (Objects.equals(c.getKind(), "MethodCalls"))
                        dealWithMethodInvocation(c, method);
                    else if (Objects.equals(c.getKind(), "BinaryOp"))
                        dealWithBinaryOp(c, s);
                    else if (Objects.equals(c.getKind(), "ExprStmt")){
                        dealWithExprStmt(c,method);
                    }
                    else if (Objects.equals(c.getKind(), "While")){
                        dealWithWhile(c, method);
                    }
                    else if (Objects.equals(c.getKind(), "IfElse")){
                        dealWithIfElse(c, method);
                    }

                }
            }
        }

        if (method.equals("main")){
            ollirCode += "\t\tret" + method_type + ";\n";
        }

        ollirCode+= "\t}\n";

        methodIndex++;

        return "";
    }

    private String dealWithExprStmt(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "Assignment"))
                dealWithAssignments(child, s);
            else if (Objects.equals(child.getKind(), "ArrayAssignment"))
                dealWithArrayAssignments(child, s);
            else if (Objects.equals(child.getKind(), "ArrayAccess"))
                dealWithArrayAccess(child, s);
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
            dealWithBinaryOp(child, method);
            ollirCode += "\t\tret" + getType(ret) + " " + "t" + tempIndex + getType(ret) + ";\n";
            tempIndex++;

        }
        return "";
    }

    private String dealWithArrayAccess(JmmNode jmmNode, String method){

        if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "BinaryOp")){
            dealWithBinaryOp(jmmNode, method);
            return "";
        }

        String array_var = jmmNode.getJmmChild(0).get("value");

        JmmNode index = jmmNode.getJmmChild(1);
        int previous;
        switch (index.getKind()) {

            case "Integer", "Identifier" -> {
                ollirCode += "\t\ttemp_" + tempIndex + ".i32 :=.i32 " + index.get("value") + ".i32;\n";
                tempIndex++;

                previous = tempIndex - 1;
                ollirCode += "\t\ttemp_" + tempIndex + ".i32 :=.i32 " + array_var + "[temp_" + previous + ".i32].i32;\n";
            }
            case "MethodCalls" -> {
                ollirCode += "\t\ttemp_" + tempIndex + ".i32 :=.i32 ";

                dealWithMethodInvocation(index, method);

                previous = tempIndex- 1;
                ollirCode += "\t\ttemp_" + tempIndex + ".i32 :=.i32 temp_" + previous + ".i32;\n";

                tempIndex++;

                previous = tempIndex - 1;
                ollirCode += "\t\ttemp_" + tempIndex + ".i32 :=.i32 " + array_var + "[temp_" + previous
                        + ".i32].i32;\n";

            }
            case "BinaryOp" ->{

                dealWithBinaryOp(index, method);

                previous = tempIndex- 1;
                ollirCode += "\t\ttemp_" + tempIndex + ".i32 :=.i32 temp_" + previous + ".i32;\n";

                tempIndex++;

                previous = tempIndex - 1;
                ollirCode += "temp_" + tempIndex + ".i32 :=.i32 " + array_var + "[temp_" + previous
                        + ".i32].i32;\n";
            }
            default -> {
            }
        }

        return "";
    }


    private String dealWithArrayAssignments(JmmNode jmmNode, String method){

        String index_val = jmmNode.getJmmChild(0).get("value");

        String assign_val = jmmNode.getJmmChild(1).get("value");

        String array_var = jmmNode.get("var");

        String index = "temp_" + localIndex + ".i32";

        ollirCode += "\t\t" + index + " :=.i32 " + index_val + ".i32;\n";

        ollirCode += "\t\t" + array_var + "[" + index + "].i32 :=.i32 " + assign_val +
                findType(jmmNode.getJmmChild(1), method) + ";\n";

        localIndex++;

        return "";

    }


    private String dealWithAssignments(JmmNode jmmNode, String s){

        Symbol local_var = symbolTable.getLocalVariables(s).get(symbolTable.getLocalVariables(s).indexOf(getLocalVar(s, jmmNode.get("var"))));


        for (JmmNode child : jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "BinaryOp")){
                dealWithBinaryOp(child, s);
                String t = getType(local_var.getType());
                ollirCode += "\t\t" + local_var.getName() + t + " :=" + t+ " temp_" + tempIndex + t + ";\n";
                return "";
            }
        }

        ollirCode += "\t\t" + local_var.getName();
        String type;

        type = getType(local_var.getType());

        switch (type) {
            case ".i32" -> {

                if (Objects.equals(jmmNode.getJmmChild(0).getKind(), "ArrayAccess")){
                    ollirCode += type + " :=" + type + " ";
                    dealWithArrayAccess(jmmNode.getJmmChild(0), s);
                    String method_arg = "temp_" + tempIndex + ".i32";

                }
                else{
                    String val = jmmNode.getJmmChild(0).get("value");
                    ollirCode += type + " :=" + type + " " + val + type + ";\n";
                }


            }
            case ".bool" -> ollirCode += type + " :=" + type + " 0.bool;\n";
            case ".array.i32" ->{

                if (jmmNode.getJmmChild(0).getChildren().size() > 1){
                    String length = jmmNode.getJmmChild(0).getJmmChild(1).get("value");

                    ollirCode += type + ":=" + type + " new(array, " + length + ".i32" + ")" + type + ";\n";
                }
                else{
                    ollirCode += type + ":=" + type + "new(" + ")" + type + ";\n";
                }

            }
            default -> {
                ollirCode += type + " :=" + type + " new(" + local_var.getType().getName() + ")" + type + ";\n";
                ollirCode += "\t\tinvokespecial(" + local_var.getName() + type + ",\"<init>\").V;\n";
            }
        }


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


        String object = jmmNode.getJmmChild(0).get("value");

        String object_type = "";

        for (Symbol obj : symbolTable.getLocalVariables(method)){
            if (obj.getName().equals(object)){
                object_type = getType(obj.getType());
            }
        }

        String method_sup;

        String arg_type = "";
        if (method_aux.getNumChildren() > 0 && !isInvokeVirtual(method_name)) {

            JmmNode temp = method_aux.getJmmChild(0);
            if (temp.getJmmChild(0).hasAttribute("value"))
                method_arg = temp.getJmmChild(0).get("value");
            else if (!temp.getJmmChild(0).hasAttribute("value") && temp.getJmmChild(0).getNumChildren() > 1){
                for (JmmNode c : temp.getChildren()){
                    if (Objects.equals(c.getKind(), "ArrayAccess")){
                        dealWithArrayAccess(c, method);
                        method_arg = "temp_" + tempIndex + ".i32";
                    }
                }
            }
            else{
                method_arg = temp.getJmmChild(0).getKind();
            }

            if (method_arg.equals("Length")){
                method_arg = "temp_" + tempIndex;

                String array = findArray(method);

                ollirCode += "\t\t"+ method_arg + ".i32 :=.i32 " + "arraylength("  + array +  ").i32;\n";
            }

            method_sup = jmmNode.getJmmChild(0).get("value");

            for (Symbol symbol : symbolTable.getLocalVariables(method)){
                if (Objects.equals(method_arg, symbol.getName())){
                    arg_type += getType(symbol.getType());
                }
            }
            if (arg_type.equals("")){
                arg_type += getParamType(method_arg, method);
                arg_type += getFieldType(method_arg);

            }

            if (arg_type.equals("") && (Objects.equals(temp.getJmmChild(0).getKind(), "Integer")
                    || Objects.equals(temp.getJmmChild(0).getKind(),"Length"))){
                arg_type += ".i32";
            }

            ollirCode += "\t\tinvokestatic(" + method_sup + findType(jmmNode.getJmmChild(0), method) + ", "+ "\"" + method_name + "\", " +
                    method_arg + arg_type + ").V;\n";

        }
        else if (isInvokeVirtual(method_name)){

            JmmNode temp = method_aux.getJmmChild(0);
            if (temp.getJmmChild(0).hasAttribute("value"))
                method_arg = temp.getJmmChild(0).get("value");
            else if (!temp.getJmmChild(0).hasAttribute("value") && temp.getJmmChild(0).getNumChildren() > 1){
                for (JmmNode c : temp.getChildren()){
                    if (Objects.equals(c.getKind(), "ArrayAccess")){
                        dealWithArrayAccess(c, method);
                        method_arg = "temp_" + tempIndex + ".i32";
                    }
                }
            }
            else{
                method_arg = temp.getJmmChild(0).getKind();
            }

            if (method_arg.equals("Length")){
                method_arg = "temp_" + tempIndex;

                String array = findArray(method);

                ollirCode += "\t\t"+ method_arg + ".i32 :=.i32 " + "arraylength("  + array +  ").i32;\n";
            }
            if (symbolTable.getParameters(method_name).size() > 0){

                if (arg_type.equals("") && (Objects.equals(temp.getJmmChild(0).getKind(), "Integer")
                        || Objects.equals(temp.getJmmChild(0).getKind(),"Length"))){
                    arg_type += ".i32";
                }


                ollirCode += "\t\tinvokevirtual("+ object + object_type + ", \"" + method_name + "\", " +
                        method_arg + arg_type + ")" + getType(
                        symbolTable.getReturnType(method_name)) + ";\n";
            }
            else{
                ollirCode += "\t\tinvokevirtual(" + object + object_type + ", \"" + method_name + "\"" + ")"+ getType(
                        symbolTable.getReturnType(method_name)) + ";\n";
            }
        }

        tempIndex ++;
        return "";
    }

    private String dealWithWhile(JmmNode jmmNode, String method){

        JmmNode bin_op = jmmNode.getJmmChild(0);

        String temp_op = bin_op.get("op");
        String op;

        Type op1_type = new Type("", false), op2_type = new Type("", false);
        if (temp_op.equals("<") || temp_op.equals(">") || temp_op.equals("<=") || temp_op.equals(">=")){
            op = getReverse(temp_op);
        }
        else {
            op = temp_op;
        }

        for (Symbol var : symbolTable.getLocalVariables(method)){
            if (var.getName().equals(bin_op.getJmmChild(0).get("value"))){
                op1_type = var.getType();
            }
            if (var.getName().equals(bin_op.getJmmChild(1).get("value"))){
                op2_type = var.getType();
            }
        }

        ollirCode += "\t\tif (" + bin_op.getJmmChild(0).get("value") + getType(op1_type) + " " + op + getOptype(op)
                + " " + bin_op.getJmmChild(1).get("value") + getType(op2_type) + ") goto ENDLOOP_1;\n";
        ollirCode += "\t\tBODY_0:\n";

        if (Objects.equals(jmmNode.getJmmChild(1).getKind(), "NestedStatements")){
            for (JmmNode c : jmmNode.getJmmChild(1).getChildren()){

                if (Objects.equals(c.getKind(), "Assignment")){
                    dealWithAssignments(c, method);
                }
                else if (Objects.equals(c.getKind(), "ArrayAssignment")){
                    dealWithArrayAssignments(c, method);
                }
                else if (Objects.equals(c.getKind(), "Return")){
                    dealWithReturn(c, method);
                } else if (Objects.equals(c.getKind(), "BinaryOp")) {
                    dealWithBinaryOp(c, method);
                } else if (Objects.equals(c.getKind(), "ExprStmt")) {
                    dealWithExprStmt(c, method);
                } else if (Objects.equals(c.getKind(), "MethodCalls")) {
                    dealWithMethodInvocation(c, method);
                }
            }
        }

        ollirCode += "\t\tif (" + bin_op.getJmmChild(0).get("value") + getType(op1_type) + " " + temp_op +
                getOptype(op) + " " + bin_op.getJmmChild(1).get("value") + getType(op2_type)
                + ") goto BODY_0;\n";


        ollirCode += "\t\tEND_LOOP_1:\n";

        return "";
    }

    private void binOpInIfElse(JmmNode jmmNode, String method){
        String op = jmmNode.getJmmChild(0).get("op");

        String op_type = getOptype(op);

        JmmNode op1 = jmmNode.getJmmChild(0).getJmmChild(0);

        String op1_type = findType(op1, method);

        JmmNode op2 = jmmNode.getJmmChild(0).getJmmChild(1);

        String op2_type = findType(op2, method);



        ollirCode += "\t\tif ($" + dollarIndex + "." + op1.get("value") + op1_type + " " + op + op_type + " ";

        dollarIndex++;

        ollirCode += "$" + dollarIndex + "." + op2.get("value") + op2_type + ") goto THEN_" + dollarIndex + ";\n";

        // Else part

        JmmNode els = jmmNode.getJmmChild(2);

        for (JmmNode else_children : els.getChildren()){
            if (Objects.equals(else_children.getKind(), "Assignment")){
                dealWithAssignments(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "ArrayAssignment")) {
                dealWithArrayAssignments(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "While")) {
                dealWithWhile(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "Return")) {
                dealWithReturn(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "MethodCalls")) {
                dealWithMethodInvocation(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "BinaryOp")) {
                dealWithBinaryOp(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "ExprStmt")) {
                dealWithExprStmt(else_children, method);
            }
        }


        ollirCode += "\t\tgoto ENDIF_" + ifIndex++ + ";\n";

        ollirCode +="\t\tTHEN_" + ifIndex + ":\n";

        // If part

        JmmNode if_part = jmmNode.getJmmChild(1);

        for (JmmNode if_children : if_part.getChildren()){
            if (Objects.equals(if_children.getKind(), "Assignment")){
                dealWithAssignments(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "ArrayAssignment")) {
                dealWithArrayAssignments(if_children, method);
            }else if (Objects.equals(if_children.getKind(), "While")) {
                dealWithWhile(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "Return")) {
                dealWithReturn(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "MethodCalls")) {
                dealWithMethodInvocation(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "BinaryOp")) {
                dealWithBinaryOp(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "ExprStmt")) {
                dealWithExprStmt(if_children, method);
            }
        }

        ollirCode += "\t\tENDIF_" + ifIndex++ + ":\n";
    }

    private void identInIfElse(JmmNode jmmNode, String method){

        JmmNode val = jmmNode.getJmmChild(0);

        String val_type = findType(val, method);

        ollirCode += "\t\tif (" + val.get("value") + val_type + ") goto THEN_"+ ifIndex + ";\n";

        // Else part

        JmmNode els = jmmNode.getJmmChild(2);

        for (JmmNode else_children : els.getChildren()){
            if (Objects.equals(else_children.getKind(), "Assignment")){
                dealWithAssignments(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "ArrayAssignment")) {
                dealWithArrayAssignments(else_children, method);
            }else if (Objects.equals(else_children.getKind(), "While")) {
                dealWithWhile(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "Return")) {
                dealWithReturn(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "MethodCalls")) {
                dealWithMethodInvocation(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "BinaryOp")) {
                dealWithBinaryOp(else_children, method);
            } else if (Objects.equals(else_children.getKind(), "ExprStmt")) {
                dealWithExprStmt(else_children, method);
            }
        }

        ollirCode += "\t\tgoto ENDIF_" + ifIndex++ + ";\n";

        ollirCode +="\t\tTHEN_" + ifIndex + ":\n";

        // If part

        JmmNode if_part = jmmNode.getJmmChild(1);

        for (JmmNode if_children : if_part.getChildren()){
            if (Objects.equals(if_children.getKind(), "Assignment")){
                dealWithAssignments(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "ArrayAssignment")) {
                dealWithArrayAssignments(if_children, method);
            }else if (Objects.equals(if_children.getKind(), "While")) {
                dealWithWhile(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "Return")) {
                dealWithReturn(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "MethodCalls")) {
                dealWithMethodInvocation(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "BinaryOp")) {
                dealWithBinaryOp(if_children, method);
            } else if (Objects.equals(if_children.getKind(), "ExprStmt")) {
                dealWithExprStmt(if_children, method);
            }
        }

        ollirCode += "\t\tENDIF_" + ifIndex++ + ":\n";


    }

    private String dealWithIfElse(JmmNode jmmNode, String method){


        for (JmmNode child : jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "BinaryOp")){
                binOpInIfElse(jmmNode, method);
            }
            else if (Objects.equals(child.getKind(), "Identifier")){
                identInIfElse(jmmNode, method);
            }

            ifIndex++;
        }



        return "";
    }


    private void dealWithBinaryChild(JmmNode jmmNode, String method, int index){

        JmmNode child = jmmNode.getJmmChild(index);

        if (Objects.equals(jmmNode.getKind(), "Length") || (Objects.equals(child.getKind(), "Length")
                && jmmNode.getNumChildren() > 1)){
            String method_arg = "temp_" + tempIndex;

            String array = findArray(method);

            ollirCode += "\t\t"+ method_arg + ".i32 :=.i32 " + "arraylength("  + array +  ").i32;\n";

            return;
        }
        if (Objects.equals(child.getKind(), "ArrayAccess") && jmmNode.getNumChildren() > 1){

            dealWithArrayAccess(child, method);

            return;
        }
        else if (Objects.equals(jmmNode.getKind(), "ArrayAccess")){
            dealWithArrayAccess(jmmNode, method);
        }

        String val_type = findType(child, method);
        String op_type = getOptype(jmmNode.get("op"));
        if (index < jmmNode.getNumChildren() - 1 && !Objects.equals(child.getKind(), "Length")) {

            ollirCode += "\t\ttemp_" + tempIndex + op_type + " :=" + op_type + " ";

            ollirCode += child.get("value") + val_type + " "+ child.getJmmParent().get("op") + val_type + " ";

            if (index + 1 == jmmNode.getNumChildren() -1 && Objects.equals(
                    jmmNode.getJmmChild(index + 1).getKind(),"BinaryOp")){
                tempIndex--;
                ollirCode += "temp_" + tempIndex + val_type + ";\n";
                tempIndex++;

            }

        }
        else if (index == jmmNode.getNumChildren() -1 && !Objects.equals(child.getKind(), "Length")) {
            if (Objects.equals(jmmNode.getJmmChild(index -1).getKind(), "Length") ||
                    Objects.equals(jmmNode.getJmmChild(index - 1).getKind(), "BinaryOp")){
                ollirCode += "\t\ttemp_" + tempIndex + op_type + " :=" + op_type + " ";

                tempIndex --;
                ollirCode += "temp_" + tempIndex + op_type + " " + child.getJmmParent().get("op") + val_type + " ";
            }
            ollirCode += child.get("value") + val_type + ";\n";
            tempIndex++;
        }

    }

    private String dealWithBinaryOp(JmmNode jmmNode, String method){

        for (int i = 0; i < jmmNode.getNumChildren(); i++) {
            JmmNode child = jmmNode.getJmmChild(i);
            if (Objects.equals(child.getKind(), "BinaryOp")) {
                dealWithBinaryOp(child, method);
            }

        }

        for (int i = 0; i < jmmNode.getNumChildren(); i++) {
            JmmNode child = jmmNode.getJmmChild(i);
            if (!Objects.equals(child.getKind(), "BinaryOp")) {
                dealWithBinaryChild(jmmNode, method, i);
            }
        }

        return "";
    }

}
