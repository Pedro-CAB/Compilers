package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.SymbolTable;
import pt.up.fe.comp.jmm.analysis.table.Type;

import java.util.List;

public class Table implements SymbolTable {

    List<String> imports, methods;

    List<Symbol> fields, parameters, local_var;
    String class_name, superClass; //classes

    Type return_type;

    @Override
    public List<String> getImports() {
        return imports;
    }

    public String getClassName() {
        return class_name;
    }

    public String getSuper() {
        return superClass;
    }

    @Override
    public List<Symbol> getFields() {
        return fields;
    }

    @Override
    public List<String> getMethods() {
        return methods;
    }


    public Type getReturnType(String methodSignature) {

        //TODO

        return return_type;
    }
    public List<Symbol> getParameters(String methodSignature) {

        //TODO
        return parameters;
    }

    public List<Symbol> getLocalVariables(String methodSignature){

        //TODO
        return local_var;
    }
}
