package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;


public class Analysis implements JmmAnalysis {

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){

        TableVisitor visitor = new TableVisitor();

        Table table = visitor.table;

        parserResult.getReports();

        JmmSemanticsResult result = new JmmSemanticsResult(parserResult, table, parserResult.getReports());

        return result;

    }
}

