package pt.up.fe.comp2023.ollir;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class Ollir implements JmmOptimization {

    public OllirResult toOllir(JmmSemanticsResult var1){

        String config = "";

        OllirVisitor ollirVisitor = new OllirVisitor(config, var1.getSymbolTable());

        OllirResult ollirResult = new OllirResult(var1, ollirVisitor.getOllirCode(), var1.getReports());

        return ollirResult;
    }

}
