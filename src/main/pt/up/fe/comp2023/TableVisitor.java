package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableVisitor extends AJmmVisitor<String, String> {

    Table table = new Table();
    @Override
    protected void buildVisitor() {
        addVisit("packageImport", this::dealWithImports);
        addVisit("Class Name", this::dealWithClassName);
        addVisit("Super", this::dealWithSuper);
        addVisit("Fields", this::dealWithFields);
        addVisit("Methods", this::dealWithMethods);
        addVisit("RetType", this::dealWithRetType);
        addVisit("Parameters", this::dealWithParameters);
        addVisit("LocalVar", this::dealWithLocalVars);

    }

    private String dealWithImports(JmmNode jmmNode, String s){

        //TODO
        List<String> imports = new ArrayList<>();

        String path = "import " + jmmNode.get("path");
        String value = "";
        for (JmmNode child :jmmNode.getChildren()){
            path += visit(child, value);
        }

        path += ";";

        imports.add(path);

        return "";
    }

    private String dealWithClassName(JmmNode jmmNode, String s){

        //TODO
        String class_name = "";

        table.setClassName(class_name);

        return "";
    }

    private String dealWithSuper(JmmNode jmmNode, String s){
        //TODO

        String sup = "";

        table.setSuper(sup);

        return "";
    }

    private String dealWithFields(JmmNode jmmNode, String s){
        //TODO

        List<Symbol> fields = new ArrayList<>();

        table.setFields(fields);

        return "";
    }

    private String dealWithMethods(JmmNode jmmNode, String s){
        //TODO

        List<String> methods = new ArrayList<>();

        table.setMethods(methods);

        return "";
    }

    private String dealWithRetType(JmmNode jmmNode, String s){
        //TODO

        Type ret_type = new Type("", false);

        table.setReturnType(ret_type);

        return "";
    }

    private String dealWithParameters(JmmNode jmmNode, String s){
        //TODO
        return "";
    }

    private String dealWithLocalVars(JmmNode jmmNode, String s){
        //TODO
        return "";
    }




}
