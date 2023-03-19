package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableVisitor extends AJmmVisitor<String, String> {

    Table table = new Table();

     public TableVisitor(Table table){
        this.table = table;
    }

    public Table getTable() {
        return table;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ImportPackage", this::dealWithImports);
        addVisit("Program", this::dealWithProgram);
        addVisit("ClassDeclaration", this::dealWithClassName);
        addVisit("Super", this::dealWithSuper);
        addVisit("ClassCode", this::dealWithClassCode);
        addVisit("MethodsAndFields", this::dealWithFields);
        addVisit("Methods", this::dealWithMethods);
        addVisit("RetType", this::dealWithRetType);
        //addVisit("Statement", this::dealWithStatement);
        addVisit("Parameters", this::dealWithParameters);
        addVisit("LocalVar", this::dealWithLocalVars);
        //addVisit("MethodDeclaration", this::dealWithMethodDeclaration);

    }

    private String dealWithProgram(JmmNode jmmNode, String s){
        StringBuilder sBuilder = new StringBuilder(s != null ? s : "");

        for(JmmNode child : jmmNode.getChildren()) {
             sBuilder.append(visit(child, ""));
         }

        return sBuilder.toString();
    }

    private String dealWithImports(JmmNode jmmNode, String s){
        s = s != null?s:"";

        String ret = s+"import " + jmmNode.get("value");

        ret += ";";

        this.table.addImports(ret);

        return "";
    }

    private String dealWithClassName(JmmNode jmmNode, String s){

        //TODO
        String class_name = "";

        table.setClassName(class_name);

        return "";
    }

    private String dealWithClassCode(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()){

            if (Objects.equals(child.getKind(), "MethodDeclaration")){

                this.dealWithParameters(child, s);

                String method = child.getKind();

                this.table.addMethods(method);

            }

            else if (Objects.equals(child.getKind(), "Declaration")){
                this.dealWithFields(child, s);
            }

        }


        return "";
    }



    private String dealWithSuper(JmmNode jmmNode, String s){
        //TODO

        String sup = "";

        table.setSuper(sup);

        return "";
    }

    private String dealWithStatement(JmmNode jmmNode, String s){

         String type_name = jmmNode.get("type");

         Type type = new Type(type_name, false);
         String var = jmmNode.get("var");

         Symbol field = new Symbol(type, var);

         table.addFields(field);

         return "";
    }


    private String dealWithFields(JmmNode jmmNode, String s){

        String type_name = jmmNode.get("type");

        Type type = new Type(type_name, false);
        String var = jmmNode.get("var");

        Symbol field = new Symbol(type, var);

        table.addFields(field);

        return "";

    }

    private String dealWithMethods(JmmNode jmmNode, String s){

        //TODO

        return "";
    }

    private String dealWithRetType(JmmNode jmmNode, String s){
        //TODO

        Type ret_type = new Type("", false);

        table.setReturnType(ret_type);

        return "";
    }


    private String dealWithParameters(JmmNode jmmNode, String s){

        for (JmmNode child : jmmNode.getChildren()){

            if (Objects.equals(child.getKind(), "Argument")){

                String type_name = child.get("type");

                Type type = new Type(type_name, false);
                String var = child.get("var");

                Symbol parameter = new Symbol(type, var);

                table.addParameters(parameter);
            }
        }

        return "";
    }

    private String dealWithLocalVars(JmmNode jmmNode, String s){
        //TODO
        return "";
    }




}
