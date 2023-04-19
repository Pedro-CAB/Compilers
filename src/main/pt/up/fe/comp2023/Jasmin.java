package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.util.ArrayList;
import java.util.List;

public class Jasmin implements JasminBackend {
    private ClassUnit classUnit;
    private static int stackLimit;
    private static int currentStack;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        classUnit = ollirResult.getOllirClass();
        if(classUnit ==  null) return null;

        StringBuilder jasminCode = new StringBuilder();

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
            jasminCode.append("\n.field ");
            switch (field.getFieldAccessModifier()){
                case PUBLIC -> jasminCode.append("public ");
                case PRIVATE -> jasminCode.append("private ");
                case PROTECTED -> jasminCode.append("protected ");
            }
            if(field.isStaticField()) jasminCode.append("static ");
            if(field.isFinalField()) jasminCode.append("final ");
            jasminCode.append(field.getFieldName());
            jasminCode.append(" ").append(getType(field.getFieldType()));
            if(field.isInitialized()) jasminCode.append(" = ").append(field.getInitialValue());
        }

        // Add methods
        for(Method method : classUnit.getMethods()){
            jasminCode.append("\n.method ");
            switch (method.getMethodAccessModifier()){
                case PUBLIC -> jasminCode.append("public ");
                case PRIVATE -> jasminCode.append("private ");
                case PROTECTED -> jasminCode.append("protected ");
            }
            if(method.isStaticMethod()) jasminCode.append("static ");
            if(method.isFinalMethod()) jasminCode.append("final ");
            if(method.isConstructMethod()) jasminCode.append("<init>(");
            else jasminCode.append(method.getMethodName()).append("(");
            ArrayList<Element> params = method.getParams();
            for(int i = 0; i < params.size(); i++){
                jasminCode.append(getType(params.get(i).getType()));
                if(i != (params.size()-1)) jasminCode.append(";");
            }
            jasminCode.append(")").append(getType(method.getReturnType()));
            for(Instruction instruction : method.getInstructions()){
                jasminCode.append(dealWithInstruction(instruction, method.getLabels(instruction)));
            }
            jasminCode.append("\n.end method");
        }

        return new JasminResult(jasminCode.toString());
    }

    public String getType(Type type){
        String s = "";
        switch (type.getTypeOfElement()){
            case INT32 -> s += "I";
            case BOOLEAN -> s += "Z";
            case ARRAYREF -> {
                ArrayType arrayType = (ArrayType) type;
                s += "[" + getType(new Type(arrayType.getArrayType()));
            }
            case OBJECTREF -> s += "a";
            case CLASS -> {
                ClassType classType = (ClassType) type;
                s += "L" + classType.getName();
            }
            case THIS -> s += "A";
            case STRING -> s += "[C";
            case VOID -> s += "V";
        }
        return s;
    }

    public String dealWithInstruction(Instruction instruction, List<String> labels){
        StringBuilder s = new StringBuilder("\n\t");

        for(String label : labels){
            s.append(label).append(":\n");
        }

        switch (instruction.getInstType()){
            case ASSIGN -> s.append(dealWithAssign((AssignInstruction) instruction));
            case CALL -> s.append(dealWithCall((CallInstruction) instruction));
            case GOTO -> s.append(dealWithGoto((GotoInstruction) instruction));
            case BRANCH -> s.append(dealWithBranch((CondBranchInstruction) instruction));
            case RETURN -> s.append(dealWithReturn((ReturnInstruction) instruction));
            case PUTFIELD -> s.append(dealWithPutField((PutFieldInstruction) instruction));
            case GETFIELD -> s.append(dealWithGetField((GetFieldInstruction) instruction));
            case UNARYOPER -> s.append(dealWithUnaryOp((UnaryOpInstruction) instruction));
            case BINARYOPER -> s.append(dealWithBinaryOp((BinaryOpInstruction) instruction));
            case NOPER -> {}
        }
        return s.toString();
    }

    public String dealWithAssign(AssignInstruction assignInstruction){
        StringBuilder s = new StringBuilder();
        Operand o1 = (Operand)assignInstruction.getDest();

        if(o1.getType().getTypeOfElement() == ElementType.ARRAYREF){

        }

        for(Node node : assignInstruction.getSuccessors()){
            assignInstruction.getRhs().addSucc(node);
        }

        return s.toString();
    }

    public String dealWithCall(CallInstruction callInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithGoto(GotoInstruction gotoInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithBranch(CondBranchInstruction branchInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithReturn(ReturnInstruction returnInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithPutField(PutFieldInstruction putFieldInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithGetField(GetFieldInstruction getFieldInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithUnaryOp(UnaryOpInstruction opInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }

    public String dealWithBinaryOp(BinaryOpInstruction opInstruction){
        StringBuilder s = new StringBuilder();
        return s.toString();
    }
}

