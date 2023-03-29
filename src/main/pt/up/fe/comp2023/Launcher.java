package pt.up.fe.comp2023;

import java.io.File;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import pt.up.fe.comp.TestUtils;
import pt.up.fe.comp.jmm.analysis.JmmAnalysis;
import pt.up.fe.comp.jmm.analysis.JmmSemanticsResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.parser.JmmParserResult;
import pt.up.fe.specs.util.SpecsIo;
import pt.up.fe.specs.util.SpecsLogs;
import pt.up.fe.specs.util.SpecsSystem;

public class Launcher {

    public static void main(String[] args) {
        // Setups console logging and other things
        SpecsSystem.programStandardInit();

        // Parse arguments as a map with predefined options
        var config = parseArgs(args);

        // Get input file
        File inputFile = new File(config.get("inputFile"));
        // Check if file exists
        if (!inputFile.isFile()) {
            throw new RuntimeException("Expected a path to an existing input file, got '" + inputFile + "'.");
        }


        // Read contents of input file
        String code = SpecsIo.read(inputFile);

        // Instantiate JmmParser
        SimpleParser parser = new SimpleParser();

        // Parse stage
        JmmParserResult parserResult = parser.parse(code, config);

        // Check if there are parsing errors
        TestUtils.noErrors(parserResult.getReports());

        //Prints the tree nodes
        System.out.println(parserResult.getRootNode().toTree());

        // ... add remaining stages
        Analysis analysis = new Analysis();
        Jasmin jasmin = new Jasmin();

        System.out.println("\n\nPrinting Symbol Table\n");
        JmmSemanticsResult semanticsResult = analysis.semanticAnalysis(parserResult);

        OllirResult ollirResult = new OllirResult("""
                import io;

                myClass {
                    .field private a.i32;

                \t.construct myClass().V{
                \t\tinvokespecial(this, "<init>").V;
                \t}

                \t.method public sum(A.array.i32).i32 {
                \t\tsum.i32 :=.i32 0.i32;
                \t\ti.i32 :=.i32 0.i32;

                \t\tLoop:
                \t\t\tt1.i32 :=.i32 arraylength($1.A.array.i32).i32;
                \t\t\tif (i.i32 >=.bool t1.i32) goto End;
                \t\t\tt2.i32 :=.i32 $1.A[i.i32].i32;
                \t\t\tsum.i32 :=.i32 sum.i32 +.i32 t2.i32;
                \t\t\ti.i32 :=.i32 i.i32 +.i32 1.i32;
                \t\t\tgoto Loop;
                \t\tEnd:
                \t\t\tret.i32 sum.i32;
                \t}
                }""", new HashMap<>());
        jasmin.toJasmin(ollirResult);
    }

    private static Map<String, String> parseArgs(String[] args) {
        SpecsLogs.info("Executing with args: " + Arrays.toString(args));

        // Check if there is at least one argument
        if (args.length != 1) {
            throw new RuntimeException("Expected a single argument, a path to an existing input file.");
        }

        // Create config
        Map<String, String> config = new HashMap<>();
        config.put("inputFile", args[0]);
        config.put("optimize", "false");
        config.put("registerAllocation", "-1");
        config.put("debug", "false");

        return config;
    }

}
