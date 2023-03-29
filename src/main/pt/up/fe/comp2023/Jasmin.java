package pt.up.fe.comp2023;

import org.specs.comp.ollir.ClassUnit;
import org.specs.comp.ollir.Field;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

public class Jasmin implements JasminBackend {
    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        StringBuilder jasminCode = new StringBuilder();

        ClassUnit classUnit = ollirResult.getOllirClass();
        if(classUnit ==  null) return null;

        // Add class declaration
        jasminCode.append(".class ");
        switch (classUnit.getClassAccessModifier()){
            case PUBLIC -> jasminCode.append("public");
            case PRIVATE -> jasminCode.append("private");
            case PROTECTED -> jasminCode.append("protected");
        }
        if(classUnit.getPackage() != null) jasminCode.append(" ").append(classUnit.getPackage());
        jasminCode.append(classUnit.getClassName());

        // Add superclass
        jasminCode.append("\n.super ");
        if(classUnit.getSuperClass() != null) jasminCode.append(classUnit.getSuperClass());
        else jasminCode.append("java/lang/Object");

        // Add fields
        for(Field field : classUnit.getFields()){
            jasminCode.append("\n.field");
        }


        return new JasminResult(jasminCode.toString());
    }
}

