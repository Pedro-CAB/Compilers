package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.JmmOptimization;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class Ollir implements JmmOptimization {

    public OllirResult toOllir(JmmSemanticsResult var1){

        String config = "";

        OllirResult ollirResult = new OllirResult(var1, config, var1.getReports());

        OllirVisitor ollirVisitor = new OllirVisitor(ollirResult);

        return ollirVisitor.getOllir_code();
    }

}
