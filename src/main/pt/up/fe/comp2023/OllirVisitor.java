package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.List;
import java.util.Objects;

public class OllirVisitor extends AJmmVisitor<String, String> {

    String ollirCode;

    SymbolTable symbolTable;

    public OllirVisitor(String ollirCode, SymbolTable symbolTable) {
        this.ollirCode = ollirCode;
        this.symbolTable = symbolTable;
    }

    public String getOllirCode() {
        return ollirCode;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Constructor", this::dealWithConstructor);
        addVisit("Class Fields", this::dealWithClassFields);
        addVisit("Method", this::dealWithMethod);
        addVisit("Assignment", this::dealWithAssignments);
        addVisit("BinaryOp", this::dealWithBinaryOp);
    }

    private String dealWithConstructor(JmmNode jmmNode, String s){

        ollirCode += ".construct " + symbolTable.getClassName() + ".V {\n";

        ollirCode += "invokespecial(this, \"<init>\").V;\n}\n";

        return "";
    }

    private String dealWithClassFields(JmmNode jmmNode, String s){

        for (Symbol field : symbolTable.getFields()){
            ollirCode += ".field private " + field.getName();

            switch (field.getType().getName()) {
                case "Int" -> ollirCode += ".i32;\n";
                case "Bool" -> ollirCode += ".bool;\n";
                default -> {
                }
            }

        }

        return "";
    }

    //Method Structure
    private String dealWithMethod(JmmNode jmmNode, String s){

        for (String method: symbolTable.getMethods()){
            ollirCode += ".method public " + method + "(";

            List<Symbol> parameters = symbolTable.getParameters(method);

            int param_sz = parameters.size();

            for (Symbol param : parameters){
                ollirCode += param.getName();

                if (param_sz == 1){
                    switch (param.getType().toString()){

                        case "Int" -> ollirCode += ".i32";
                        case "Bool" -> ollirCode += ".bool";
                        default -> {
                        }
                    }
                }

                else if (param_sz > 1){
                    switch (param.getType().toString()){

                        case "Int" -> ollirCode += ".i32, ";
                        case "Bool" -> ollirCode += ".bool, ";
                        default -> {
                        }
                    }
                }

                param_sz -= 1;
            }

            switch (symbolTable.getReturnType(method).getName()){
                case "Int" -> ollirCode += ").i32{\n";
                case "Bool" -> ollirCode += ").bool{\n";
                default -> {
                }
            }

            for (JmmNode child: jmmNode.getChildren()){
                //TODO DEAL WITH METHOD BODY
            }

            ollirCode+= "}\n";

        }

        return "";
    }

    private String dealWithAssignments(JmmNode jmmNode, String s){

        for (Symbol local_var : symbolTable.getLocalVariables(jmmNode.get("Name"))){
            ollirCode += local_var.getName();
            String type = "";
            switch (local_var.getType().getName()){

                case "Int" -> type =  ".i32 ";
                case "Bool" -> type = ".bool ";
                default -> {
                }
            }

            if (type.equals(".i32 "))
                ollirCode += type + " :=" + type + "0.132;\n";
            else
                ollirCode += type + " :=" + type + "0.bool;\n";
        }

        return "";
    }

    private String dealWithBinaryOp(JmmNode jmmNode, String s){

        //TODO

        return "";
    }


}