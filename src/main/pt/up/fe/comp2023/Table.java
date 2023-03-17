package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.ArrayList;
import java.util.List;

public class Table implements SymbolTable {

    List<String> imports, methods;

    List<Symbol> fields, parameters, local_var;
    String class_name, super_class; //classes

    Type ret_type;
    int b;

    public Table(){
        this.imports = new ArrayList<>();
        this.methods = new ArrayList<>();
        this.fields = new ArrayList<>();
        this.parameters = new ArrayList<>();
        this.local_var = new ArrayList<>();
        this.class_name = "";
        this.super_class = "";
        this.ret_type = new Type("", false);
        this.b = 1;
    }

    public void addImports(String imports) {
        this.imports.add(imports);
    }

    @Override
    public List<String> getImports() {
        return imports;
    }

    public void setClassName(String class_name) {
        this.class_name = class_name;
    }

    public String getClassName() {
        return class_name;
    }

    public void setSuper(String superClass){
        this.super_class = superClass;
    }

    public String getSuper() {
        return super_class;
    }

    public void setFields(List<Symbol> fields) {
        this.fields = fields;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    public void setMethods(List<String> methods) {
        this.methods = methods;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }

    public void setReturnType(Type ret_type) {
        this.ret_type = ret_type;
    }

    public Type getReturnType(String methodSignature) {
        return ret_type;
    }

    public void setParameters(List<Symbol> parameters) {
        this.parameters = parameters;
    }

    public List<Symbol> getParameters(String methodSignature) {

        return parameters;
    }

    public void setLocalVariables(List<Symbol> local_var) {
        this.local_var = local_var;
    }
    public List<Symbol> getLocalVariables(String methodSignature){

        return local_var;
    }
}
