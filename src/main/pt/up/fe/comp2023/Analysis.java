package pt.up.fe.comp2023;

import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;


public class Analysis implements JmmAnalysis {

    public JmmSemanticsResult semanticAnalysis(JmmParserResult parserResult){

        Table table = new Table();

        TableVisitor visitor = new TableVisitor(table);
        visitor.visit(parserResult.getRootNode(), "");

        return new JmmSemanticsResult(parserResult, visitor.getTable(), parserResult.getReports());
    }
}

