package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.ast.AJmmVisitor;
import pt.up.fe.comp.jmm.ast.JmmNode;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class OllirVisitor extends AJmmVisitor<String, String> {

    OllirResult ollir_code;

    public OllirVisitor(OllirResult ollir_code) {this.ollir_code = ollir_code;}

    public OllirResult getOllir_code() {
        return ollir_code;
    }

    @Override
    protected void buildVisitor() {
        addVisit("Pão", this::dealWithPão);
    }

    private String dealWithPão(JmmNode jmmNode, String s){
        return "";
    }


}
