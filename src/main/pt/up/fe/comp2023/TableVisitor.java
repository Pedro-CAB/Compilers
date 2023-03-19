package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.table.Symbol;
import pt.up.fe.comp.jmm.analysis.table.Type;
import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class TableVisitor extends AJmmVisitor<String, String> {

    Table table;

    public TableVisitor(Table table) {
        this.table = table;
    }

    public Table getTable() {
        return table;
    }

    @Override
    protected void buildVisitor() {
        addVisit("ImportPackage", this::dealWithImports);
        addVisit("Program", this::dealWithProgram);
        addVisit("ClassDeclaration", this::dealWithClassDeclaration);
        addVisit("Super", this::dealWithSuper);
        addVisit("ClassBody", this::dealWithClassBody);
        addVisit("ClassField", this::dealWithFields);
        addVisit("ClassMethod", this::dealWithMethods);
        addVisit("ClassArrayMethod", this::dealWithMethods);
        addVisit("Parameters", this::dealWithParameters);
        addVisit("classIdentification", this::dealWithClassIdentification);
    }

    private String dealWithProgram(JmmNode jmmNode, String s) {
        StringBuilder sBuilder = new StringBuilder(s != null ? s : "");

        for (JmmNode child : jmmNode.getChildren()) {
            sBuilder.append(visit(child, ""));
        }

        return sBuilder.toString();
    }

    private String dealWithImports(JmmNode jmmNode, String s) {
        s = s != null ? s : "";
        String ret = s + "import " + jmmNode.get("value");
        ret += ";";
        this.table.addImports(ret);

        return "";
    }

    private String dealWithClassName(JmmNode jmmNode, String s){

        String class_name = jmmNode.get("value");

        table.setClassName(class_name);

        return "";
    }


    private String dealWithClassIdentification(JmmNode jmmNode, String s){

        for (JmmNode child: jmmNode.getChildren()){

            if (Objects.equals(child.getKind(), "className")){
                this.dealWithClassName(child, s);
            }

            else if (Objects.equals(child.getKind(), "SuperclassName")
                    || Objects.equals(child.getKind(), "ImplementedClass")){
                this.dealWithSuper(child, s);
            }

        }

        return "";
    }


    private String dealWithSuper(JmmNode jmmNode, String s) {
        String sup = jmmNode.get("value");

        table.setSuper(sup);

        return "";
    }

    private String dealWithClassDeclaration(JmmNode jmmNode, String s) {

        for (JmmNode child : jmmNode.getChildren()){
            if (Objects.equals(child.getKind(), "ClassName")){
                this.dealWithClassName(child, s);
            }
            else if (Objects.equals(child.getKind(), "SuperclassName")
                    || Objects.equals(child.getKind(), "ImplementedClass")){
                this.dealWithSuper(child, s);
            }
        }

        return "";
    }

    private String dealWithClassBody(JmmNode jmmNode, String s) {
        StringBuilder sBuilder = new StringBuilder(s);
        for (JmmNode child : jmmNode.getChildren()) {
            sBuilder.append(visit(child, ""));
        }
        s = sBuilder.toString();

        return s;
    }

    private String dealWithFields(JmmNode jmmNode, String s) {
        JmmNode declaration = jmmNode.getJmmChild(0);

        String type_name = declaration.getJmmChild(0).get("type");
        Type type = new Type(type_name, Objects.equals(jmmNode.getJmmChild(0).getKind(), "ArrayType"));

        String var = jmmNode.getJmmChild(0).get("var");
        Symbol field = new Symbol(type, var);

        table.addFields(field);

        return "";
    }

    private String dealWithMethods(JmmNode jmmNode, String s) {
        List<Symbol> parameters = new ArrayList<>();
        table.addMethods(jmmNode.get("name"));

        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "Type"))
                table.addReturnType(jmmNode.get("name"), new Type(child.get("type"), false));
            else if (Objects.equals(child.getKind(), "ArrayType"))
                table.addReturnType(jmmNode.get("name"), new Type(child.get("type"), true));
            else if (Objects.equals(child.getKind(), "Argument")){
                if(Objects.equals(child.getJmmChild(0).getKind(), "Type"))
                    parameters.add(new Symbol(new Type(child.getJmmChild(0).get("type"), false), child.get("var")));
                else if(Objects.equals(child.getJmmChild(0).getKind(), "ArrayType"))
                    parameters.add(new Symbol(new Type(child.getJmmChild(0).get("type"), false), child.get("var")));
            }
        }
        table.setParameters(jmmNode.get("name"), parameters);

        return "";
    }


    private String dealWithParameters(JmmNode jmmNode, String s) {
        for (JmmNode child : jmmNode.getChildren()) {
            if (Objects.equals(child.getKind(), "Argument")) {
                String type_name = child.get("type");
                Type type = new Type(type_name, false);

                String var = child.get("var");
                Symbol parameter = new Symbol(type, var);


            }
        }

        return "";
    }


}
