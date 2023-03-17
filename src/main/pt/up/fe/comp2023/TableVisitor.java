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
        s = s != null?s:"";
        List<String> imports = new ArrayList<>();
        String ret = s+"import " + jmmNode.getObject("path");

        ret += jmmNode.getObject("value");

        ret += ";";

        imports.add(ret);

        this.table.setImports(imports);

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
