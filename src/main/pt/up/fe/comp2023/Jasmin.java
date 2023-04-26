package pt.up.fe.comp2023;

import org.specs.comp.ollir.*;
import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class Jasmin implements JasminBackend {
    private ClassUnit classUnit;
    private HashMap<String, Descriptor> vars;
    private int stackLimit;
    private int currentStack;
    private int numCond;

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        classUnit = ollirResult.getOllirClass();
        if (classUnit == null) return null;

        StringBuilder jasminCode = new StringBuilder("\n");
        stackLimit = 0;
        currentStack = 0;
        numCond = 0;

        // Add class declaration
        jasminCode.append(".class ");
        switch (classUnit.getClassAccessModifier()) {
            case PUBLIC -> jasminCode.append("public");
            case PRIVATE -> jasminCode.append("private");
            case PROTECTED -> jasminCode.append("protected");
        }
        if (classUnit.getPackage() != null) jasminCode.append(" ").append(classUnit.getPackage());
        jasminCode.append(classUnit.getClassName());

        // Add superclass
        jasminCode.append("\n.super ");
        if (classUnit.getSuperClass() != null) jasminCode.append(getClassFullName(classUnit.getSuperClass())).append("\n");
        else jasminCode.append("java/lang/Object\n");

        // Add fields
        for (Field field : classUnit.getFields()) {
            jasminCode.append("\n.field ");
            switch (field.getFieldAccessModifier()) {
                case PUBLIC -> jasminCode.append("public ");
                case PRIVATE -> jasminCode.append("private ");
                case PROTECTED -> jasminCode.append("protected ");
            }
            if (field.isStaticField()) jasminCode.append("static ");
            if (field.isFinalField()) jasminCode.append("final ");
            jasminCode.append(field.getFieldName());
            jasminCode.append(" ").append(getType(field.getFieldType()));
            if (field.isInitialized()) jasminCode.append(" = ").append(field.getInitialValue());
        }

        // Add methods
        for (Method method : classUnit.getMethods()) {
            jasminCode.append("\n\n.method ");
            switch (method.getMethodAccessModifier()) {
                case PUBLIC -> jasminCode.append("public ");
                case PRIVATE -> jasminCode.append("private ");
                case PROTECTED -> jasminCode.append("protected ");
            }
            if (method.isStaticMethod()) jasminCode.append("static ");
            if (method.isFinalMethod()) jasminCode.append("final ");
            if (method.isConstructMethod()) jasminCode.append("<init>(");
            else jasminCode.append(method.getMethodName()).append("(");
            ArrayList<Element> params = method.getParams();
            for (int i = 0; i < params.size(); i++) {
                jasminCode.append(getType(params.get(i).getType()));
                if (i != (params.size() - 1)) jasminCode.append(";");
            }
            jasminCode.append(")").append(getType(method.getReturnType()));
            StringBuilder methodInstructions = new StringBuilder();
            for (Instruction instruction : method.getInstructions()) {
                vars = method.getVarTable();
                methodInstructions.append(dealWithInstruction(instruction, method.getLabels(instruction), method.getReturnType(), false));
            }
            jasminCode.append("\n\t.limit stack ").append(stackLimit).append("\n");
            jasminCode.append("\t.limit locals ").append(method.getVarTable().size()).append(method.getVarTable().containsKey("this") || method.isStaticMethod() ? 0 : 1).append("\n");
            jasminCode.append(methodInstructions);
            jasminCode.append("\n.end method");
        }

        JasminResult jasminResult = new JasminResult(jasminCode.toString());
        File outputDir = new File("test/fixtures/public/testing");
        jasminResult.compile(outputDir);
        return new JasminResult(ollirResult, jasminCode.toString(), Collections.emptyList());
    }

    private String dealWithInstruction(Instruction instruction, List<String> labels, Type returnType, Boolean assign) {
        StringBuilder s = new StringBuilder("\n");

        for (String label : labels) {
            s.append(label).append(":\n");
        }

        switch (instruction.getInstType()) {
            case ASSIGN -> s.append(dealWithAssign((AssignInstruction) instruction, labels, returnType, assign));
            case CALL -> s.append(dealWithCall((CallInstruction) instruction, returnType, assign));
            case GOTO -> s.append(dealWithGoto((GotoInstruction) instruction));
            case BRANCH -> s.append(dealWithBranch((CondBranchInstruction) instruction, labels, returnType, assign));
            case RETURN -> s.append(dealWithReturn((ReturnInstruction) instruction));
            case PUTFIELD -> s.append(dealWithPutField((PutFieldInstruction) instruction));
            case GETFIELD -> s.append(dealWithGetField((GetFieldInstruction) instruction));
            case UNARYOPER -> s.append(dealWithUnaryOp((UnaryOpInstruction) instruction));
            case BINARYOPER -> s.append(dealWithBinaryOp((BinaryOpInstruction) instruction));
            case NOPER -> s.append(dealWithNoper(instruction, labels, returnType, assign));
        }
        return s.toString();
    }

    private String dealWithAssign(AssignInstruction assignInstruction, List<String> labels, Type returnType, Boolean assign) {
        StringBuilder s = new StringBuilder();
        Operand op = (Operand) assignInstruction.getDest();

        if (op.getType().getTypeOfElement() == ElementType.ARRAYREF) {
            ArrayOperand arrayOp = (ArrayOperand) op;
            if(arrayOp.isParameter()) s.append("\taload ").append(arrayOp.getParamId()).append("\n\t");
            else{
                Descriptor d;
                if((d = vars.get(arrayOp.getName())) != null) s.append("\taload ").append(d.getVirtualReg()).append("\n\t");
                else s.append("\taload_1\n\t");
                s.append("\n");
                if(!arrayOp.getIndexOperands().isEmpty()) s.append(load(arrayOp.getIndexOperands().get(0)));
                currentStack+=1;
            }
        }

        for (Node node : assignInstruction.getSuccessors()) {
            assignInstruction.getRhs().addSucc(node);
        }

        String rhs = checkIINC(op, assignInstruction.getRhs());
        if(rhs.equals("")) s.append(dealWithInstruction(assignInstruction.getRhs(), labels, returnType, assign)).append(store(op));
        else s.append(rhs);

        return s.toString();
    }

    private String dealWithCall(CallInstruction callInstruction, Type returnType, Boolean assign) {
        StringBuilder s = new StringBuilder();

        switch (callInstruction.getInvocationType()){
            case invokevirtual -> s.append(dealWithInvokeVirtual(callInstruction, returnType, assign));
            case invokeinterface -> s.append(dealWithInvokeInterface(callInstruction, assign));
            case invokespecial -> s.append(dealWithInvokeSpecial(callInstruction, assign));
            case invokestatic -> s.append(dealWithInvokeStatic(callInstruction, assign));
            case NEW -> s.append(dealWithNEW(callInstruction));
            case arraylength -> s.append(dealWithArrayLength(callInstruction));
            case ldc -> s.append(dealWithLDC(callInstruction));
        }
        return s.toString();
    }

    private String dealWithInvokeVirtual(CallInstruction callInstruction, Type returnType, Boolean assign){
        StringBuilder s = new StringBuilder();

        Operand op = (Operand) callInstruction.getFirstArg();
        LiteralElement element = (LiteralElement) callInstruction.getSecondArg();
        String secondArg = element.getLiteral().replace("\"", "");

        s.append("\t").append(load(op));

        for(Element e : callInstruction.getListOfOperands()){
            s.append("\t").append(load(e));
        }

        s.append("\tinvokevirtual ").append(getType(op.getType())).append("/").append(secondArg).append("(");

        Stream<String> opTypes = callInstruction.getListOfOperands().stream().map(e -> getType(e.getType()));
        s.append(opTypes.collect(Collectors.joining()));

        s.append(")").append(getType(callInstruction.getReturnType())).append("\n");

        stackLimit = Math.max(stackLimit, currentStack);
        for(int i = 0; i <= callInstruction.getListOfOperands().size(); i++){
            currentStack--;
        }

        if(returnType.getTypeOfElement() != ElementType.VOID && !assign){
            s.append("\tpop\n");
            stackLimit = Math.max(stackLimit, currentStack);
            currentStack--;
        }

        currentStack++;

        return s.toString();
    }

    private String dealWithInvokeInterface(CallInstruction callInstruction, Boolean assign){
        String s = "";
        return s;
    }

    private String dealWithInvokeSpecial(CallInstruction callInstruction, Boolean assign){
        StringBuilder s = new StringBuilder();

        Operand op = (Operand) callInstruction.getFirstArg();
        LiteralElement element = (LiteralElement) callInstruction.getSecondArg();
        String method = element.getLiteral().replace("\"", "");

        s.append("\t").append(load(op));

        for(Element e : callInstruction.getListOfOperands()){
            s.append("\t").append(load(e));
        }

        s.append("\tinvokespecial ").append(getType(op.getType())).append("/").append(method).append("(");

        Stream<String> opTypes = callInstruction.getListOfOperands().stream().map(e -> getType(e.getType()));
        s.append(opTypes.collect(Collectors.joining()));

        s.append(")").append(getType(callInstruction.getReturnType())).append("\n");

        stackLimit = Math.max(stackLimit, currentStack);
        for(int i = 0; i < callInstruction.getListOfOperands().size(); i++){
            currentStack--;
        }

        if(callInstruction.getReturnType().getTypeOfElement() != ElementType.VOID && !assign){
            s.append("\tpop\n");
        }

        return s.toString();
    }

    private String dealWithInvokeStatic(CallInstruction callInstruction, Boolean assign){
        StringBuilder s = new StringBuilder();

        Operand op = (Operand) callInstruction.getFirstArg();
        String className = op.getName();
        LiteralElement element = (LiteralElement) callInstruction.getSecondArg();
        String method = element.getLiteral().replace("\"", "");

        for(Element e : callInstruction.getListOfOperands()){
            s.append("\t").append(load(e));
        }

        s.append("\tinvokestatic ");
        if(className == null) s.append("java/lang/Object");
        else s.append(getClassFullName(className));
        s.append("/").append(method).append("(");


        Stream<String> opTypes = callInstruction.getListOfOperands().stream().map(e -> getType(e.getType()));
        s.append(opTypes.collect(Collectors.joining()));

        s.append(")").append(getType(callInstruction.getReturnType())).append("\n");

        stackLimit = Math.max(stackLimit, currentStack);
        for(int i = 0; i < callInstruction.getListOfOperands().size(); i++){
            currentStack--;
        }

        if(callInstruction.getReturnType().getTypeOfElement() != ElementType.VOID && !assign){
            s.append("\tpop\n");
            stackLimit = Math.max(stackLimit, currentStack);
            currentStack--;
        }

        currentStack++;

        return s.toString();
    }

    private String dealWithNEW(CallInstruction callInstruction){
        StringBuilder s = new StringBuilder();

        Operand op = (Operand) callInstruction.getFirstArg();

        for(Element e : callInstruction.getListOfOperands()){
            s.append("\t").append(load(e));
        }

        s.append("\tnew");
        switch (op.getType().getTypeOfElement()){
            case CLASS -> s.append("\t").append(op.getName()).append("\n\tdup\n");
            case ARRAYREF -> s.append("array int");
            case OBJECTREF -> s.append("\t").append(op.getName()).append("\n");
        }

        stackLimit = Math.max(stackLimit, currentStack);
        for(int i = 1; i < callInstruction.getListOfOperands().size(); i++){
            currentStack--;
        }

        return s.toString();
    }

    private String dealWithArrayLength(CallInstruction callInstruction){
        stackLimit = Math.max(stackLimit, currentStack);
        return "\t" + load(callInstruction.getFirstArg()) + "\tarraylength\n";
    }

    private String dealWithLDC(CallInstruction callInstruction){
        return "\t" + load(callInstruction.getFirstArg());
    }

    private String dealWithGoto(GotoInstruction gotoInstruction) {
        return "\tgoto " + gotoInstruction.getLabel() + "\n";
    }

    private String dealWithBranch(CondBranchInstruction branchInstruction, List<String> labels, Type returnType, Boolean assign) {
        return dealWithInstruction(branchInstruction.getCondition(), labels, returnType, assign) + branchInstruction.getLabel() + "\n";
    }

    private String dealWithReturn(ReturnInstruction returnInstruction) {
        if(!returnInstruction.hasReturnValue()){
            return "";
        }

        String returnType = "";
        switch (returnInstruction.getOperand().getType().getTypeOfElement()) {
            case INT32, BOOLEAN -> returnType = "i";
            case STRING -> returnType = "Ljava/lang/String;";
            case ARRAYREF, OBJECTREF -> returnType = "a";
        }
        String s = "\t" + load(returnInstruction.getOperand()) + returnType + "return\n";

        stackLimit = Math.max(stackLimit, currentStack);
        currentStack--;

        return s;
    }

    private String dealWithPutField(PutFieldInstruction putFieldInstruction) {
        StringBuilder s = new StringBuilder();

        Element e1 = putFieldInstruction.getFirstOperand();
        Element e2 = putFieldInstruction.getSecondOperand();
        Element e3 = putFieldInstruction.getThirdOperand();
        Operand op1 = (Operand) e1;

        s.append("\t").append(load(op1));
        s.append("\t").append(load(e3));

        s.append("\tputfield ");
        String str;
        if(e2.isLiteral()){
            str = ((LiteralElement) e2).getLiteral();
        }
        else{
            Operand op2 = (Operand) e2;
            str = op2.getName();
        }
        if(op1.getType().getTypeOfElement() == ElementType.THIS){
            s.append(classUnit.getClassName()).append("/").append(str);
        }
        else{
            s.append(getClassFullName(op1.getName())).append("/").append(str);
        }
        s.append(" ").append(getType(e3.getType())).append("\n");

        stackLimit = Math.max(stackLimit, currentStack);
        currentStack-=2;

        return s.toString();
    }

    private String dealWithGetField(GetFieldInstruction getFieldInstruction) {
        StringBuilder s = new StringBuilder();

        Operand op1 = (Operand) getFieldInstruction.getFirstOperand();
        Element e2 = getFieldInstruction.getSecondOperand();

        s.append("\t").append(load(op1));

        s.append("\tgetfield ");
        String str;
        if(e2.isLiteral()){
            str = ((LiteralElement) e2).getLiteral();
        }
        else{
            Operand op2 = (Operand) e2;
            str = op2.getName();
        }
        if(op1.getType().getTypeOfElement() == ElementType.THIS){
            s.append(classUnit.getClassName()).append("/").append(str);
        }
        else{
            s.append(getClassFullName(op1.getName())).append("/").append(str);
        }
        s.append(" ").append(getType(getFieldInstruction.getFieldType())).append("\n");

        stackLimit = Math.max(stackLimit, currentStack);

        return s.toString();
    }

    private String dealWithUnaryOp(UnaryOpInstruction opInstruction) {
        StringBuilder s = new StringBuilder();
        Operation operation = opInstruction.getOperation();
        s.append("\t").append(load(opInstruction.getOperand()));

        switch (operation.getOpType()){
            case NOT, NOTB -> {
                s.append("\tifne TRUE").append(numCond).append("\n");
                s.append("\riconst_1\n");
                s.append("\tgoto FALSE").append(numCond).append("\n");
                s.append("TRUE").append(numCond).append(":\n");
                s.append("\ticonst_0\n");
                s.append("FALSE").append(numCond).append(":\n");
                stackLimit = Math.max(stackLimit, currentStack);
                numCond++;
            }
        }
        return s.toString();
    }

    private String dealWithNoper(Instruction instruction, List<String> labels, Type returnType, Boolean assign) {
        if(instruction instanceof SingleOpInstruction singleOpInstruction) return dealWithSingleOp(singleOpInstruction);
        if(instruction instanceof SingleOpCondInstruction singleOpInstruction) return dealWithSingleCondOp(singleOpInstruction, labels, returnType, assign);
        return "";
    }

    private String dealWithSingleOp(SingleOpInstruction singleOpInstruction) {
        return "\t" + load(singleOpInstruction.getSingleOperand());
    }

    private String dealWithSingleCondOp(SingleOpCondInstruction singleOpInstruction, List<String> labels, Type returnType, Boolean assign) {

        String s = dealWithInstruction(singleOpInstruction.getCondition(), labels, returnType, assign) +
                "\tifne " + singleOpInstruction.getLabel() + " \n";

        stackLimit = Math.max(stackLimit, currentStack);
        currentStack--;

        return s;
    }

    private String dealWithBinaryOp(BinaryOpInstruction opInstruction) {
        StringBuilder s = new StringBuilder();

        Operation operation = opInstruction.getOperation();
        Element leftOp = opInstruction.getLeftOperand();
        Element rightOp = opInstruction.getRightOperand();

        switch (operation.getOpType()){
            case ADD -> s.append(getArithmetic("iadd", leftOp, rightOp));
            case SUB -> s.append(getArithmetic("isub", leftOp, rightOp));
            case MUL -> s.append(getArithmetic("imul", leftOp, rightOp));
            case DIV -> s.append(getArithmetic("idiv", leftOp, rightOp));
            case SHR -> {}
            case SHL -> {}
            case SHRR -> {}
            case XOR -> {
                s.append(load(leftOp));
                s.append(load(rightOp));
                s.append("\tixor\n");
                stackLimit = Math.max(stackLimit, currentStack);
                currentStack--;
            }
            case AND -> {
                s.append(load(leftOp));
                s.append(load(rightOp));
                s.append("\tiand\n");
                stackLimit = Math.max(stackLimit, currentStack);
                currentStack--;
            }
            case OR -> {
                s.append(load(leftOp));
                s.append(load(rightOp));
                s.append("\tior\n");
                stackLimit = Math.max(stackLimit, currentStack);
                currentStack--;
            }
            case LTH -> s.append(getCond("lt", leftOp, rightOp));
            case GTH -> s.append(getCond("gt", leftOp, rightOp));
            case EQ -> s.append(getCond("eq", leftOp, rightOp));
            case NEQ -> s.append(getCond("ne", leftOp, rightOp));
            case LTE -> s.append(getCond("le", leftOp, rightOp));
            case GTE -> s.append(getCond("ge", leftOp, rightOp));
            case ANDB -> s.append(getAndOr("and", leftOp, rightOp));
            case ORB -> s.append(getAndOr("or", leftOp, rightOp));
            case NOTB -> {
                s.append("\tif_ne TRUE").append(numCond).append("\n");
                s.append("\ticonst_1\n");
                s.append("\tgoto FALSE").append(numCond).append("\n");
                s.append("TRUE").append(numCond).append(":\n");
                s.append("\ticonst_0");
                s.append("FALSE").append(numCond).append(":\n");
                numCond++;
            }
            case NOT -> {}
        }
        return s.toString();
    }

    private String checkIINC(Operand dest, Instruction instruction) {
        if(instruction.getInstType() == InstructionType.BINARYOPER) {
            BinaryOpInstruction binaryOpInstruction = (BinaryOpInstruction) instruction;

            if(binaryOpInstruction.getOperation().getOpType() == OperationType.ADD) {
                LiteralElement literalElement;
                Operand op;
                boolean leftOpLiteral = binaryOpInstruction.getLeftOperand().isLiteral();
                boolean rightOpLiteral = binaryOpInstruction.getRightOperand().isLiteral();

                if(leftOpLiteral && !rightOpLiteral) {
                    literalElement = (LiteralElement) binaryOpInstruction.getLeftOperand();
                    op = (Operand) binaryOpInstruction.getRightOperand();
                }
                else if(!leftOpLiteral && rightOpLiteral) {
                    literalElement = (LiteralElement) binaryOpInstruction.getRightOperand();
                    op = (Operand) binaryOpInstruction.getLeftOperand();
                }
                else return "";

                if(vars.get(dest.getName()).getVirtualReg() == vars.get(op.getName()).getVirtualReg()) {
                    return "\tiinc " + vars.get(op.getName()).getVirtualReg() + " " + literalElement.getLiteral() + "\n";
                }
                else if(instruction.getPredecessors() != null) {
                    Instruction successor = (Instruction) instruction.getSuccessors().get(0);
                    if(successor.getInstType() == InstructionType.ASSIGN) {
                        Instruction rhsInstruction = ((AssignInstruction) successor).getRhs();

                        if (rhsInstruction instanceof SingleOpInstruction) {
                            Operand destOperand = (Operand) ((AssignInstruction) successor).getDest();
                            Operand assignOperand = (Operand) ((SingleOpInstruction) rhsInstruction).getSingleOperand();

                            if(vars.get(destOperand.getName()).getVirtualReg()  == vars.get(op.getName()).getVirtualReg()) {
                                return "\tiinc " + vars.get(destOperand.getName()).getVirtualReg() + " " + literalElement.getLiteral() + "\n" +
                                        "\t" + load(destOperand) +
                                        "\t" + load(assignOperand);
                            }
                        }
                    }

                }
            }
        }
        return "";
    }

    private String getArithmetic(String instruction, Element leftOp, Element rightOp) {
        StringBuilder s = new StringBuilder();

        String exception;
        if(instruction.equals("imul") && (exception = checkMulException(leftOp, rightOp)) != null){
            return exception;
        }
        if(instruction.equals("idiv") && (exception = checkDivException(leftOp, rightOp)) != null){
            return exception;
        }

        if(leftOp.isLiteral() && rightOp.isLiteral()){
            LiteralElement result;
            int leftInt = Integer.parseInt(((LiteralElement) leftOp).getLiteral());
            int rightInt = Integer.parseInt(((LiteralElement) rightOp).getLiteral());

            switch (instruction){
                case "iadd" -> result = new LiteralElement((leftInt + rightInt) + "", new Type(ElementType.INT32));
                case "isub" -> result = new LiteralElement((leftInt - rightInt) + "", new Type(ElementType.INT32));
                case "imul" -> result = new LiteralElement((leftInt * rightInt) + "", new Type(ElementType.INT32));
                case "idiv" -> result = new LiteralElement((leftInt / rightInt) + "", new Type(ElementType.INT32));
                default -> result = new LiteralElement("", new Type(ElementType.INT32));
            }

            s.append("\t").append(load(result));
            stackLimit = Math.max(stackLimit, currentStack);
        }
        else {
            s.append("\t").append(load(leftOp)).append("\t").append(load(rightOp));
            s.append("\t").append(instruction).append("\n");
            stackLimit = Math.max(stackLimit, currentStack);
            currentStack--;
        }

        return s.toString();
    }

    private String getCond(String instruction, Element leftOp, Element rightOp) {
        StringBuilder s = new StringBuilder();
        LiteralElement literalElement = null;
        Operand operand = null;
        String prefix = "_icmp";

        if(leftOp.isLiteral() && rightOp.isLiteral()) {
            currentStack++;
            switch (instruction){
                case "eq", "ne", "and", "or" -> {
                    return bitwise(instruction, ((LiteralElement) leftOp).getLiteral(), ((LiteralElement) rightOp).getLiteral());
                }
                case "lt", "le", "gt", "ge" -> {
                    return compare(instruction, ((LiteralElement) leftOp).getLiteral(), ((LiteralElement) rightOp).getLiteral());
                }
            }
        }
        else if(leftOp.isLiteral() && !rightOp.isLiteral()) {
            literalElement = (LiteralElement) leftOp;
            operand = (Operand) rightOp;
        }
        else if(!leftOp.isLiteral() && rightOp.isLiteral()) {
            literalElement = (LiteralElement) rightOp;
            operand = (Operand) leftOp;
        }

        if(literalElement != null && literalElement.getLiteral().equals("0")) {
            prefix = "";
        }

        if(prefix.equals("")) {
            s.append("\t").append(load(operand));
            stackLimit = Math.max(stackLimit, currentStack);
            currentStack--;
        }
        else {
            s.append("\t").append(load(leftOp));
            s.append("\t").append(load(rightOp));
            stackLimit = Math.max(stackLimit, currentStack);
            currentStack -= 2;
        }

        s.append("\t" + "if").append(prefix).append(instruction).append(" FALSE").append(numCond).append("\n");
        s.append("\ticonst_0\n");
        s.append("\tgoto TRUE").append(numCond).append("\n");
        s.append("FALSE").append(numCond).append(":\n");
        s.append("\ticonst_1\n");
        s.append("TRUE").append(numCond).append(":\n");
        currentStack++;
        numCond++;

        return s.toString();
    }

    private String getAndOr(String instruction, Element leftOp, Element rightOp) {
        var s = new StringBuilder();
        LiteralElement literal = null;
        String result = null;

        if(leftOp.isLiteral() && rightOp.isLiteral()) return getCond(instruction, leftOp, rightOp);

        if(leftOp.isLiteral() && !rightOp.isLiteral()) literal = (LiteralElement) leftOp;
        else if(!leftOp.isLiteral() && rightOp.isLiteral()) literal = (LiteralElement) rightOp;

        if(literal != null && instruction.equals("and") && literal.getLiteral().equals("0")) return "\ticonst_0\n";
        if(literal != null && instruction.equals("or") && literal.getLiteral().equals("1")) return "\ticonst_1\n";

        s.append("\t").append(load(leftOp));
        s.append("\tifeq" + " FALSE").append(numCond).append("\n");
        stackLimit = Math.max(stackLimit, currentStack);
        currentStack--;
        s.append("\t").append(load(rightOp));
        s.append("\tifeq" + " FALSE").append(numCond).append("\n");
        stackLimit = Math.max(stackLimit, currentStack);
        s.append("\ticonst_1\n");
        s.append("\tgoto TRUE").append(numCond).append("\n");
        s.append("FALSE").append(numCond).append(":\n");
        s.append("\ticonst_0\n");
        s.append("TRUE").append(numCond).append(":\n");
        numCond++;

        return s.toString();
    }

    private String checkMulException(Element leftOp, Element rightOp) {
        int num;
        LiteralElement literal = null;
        Element element = null;

        if(leftOp.isLiteral()){
            literal = (LiteralElement) leftOp;
            element = rightOp;
        }
        else if(rightOp.isLiteral()){
            literal = (LiteralElement) rightOp;
            element = leftOp;
        }

        if(literal != null){
            num = Integer.parseInt(literal.getLiteral());

            if(element instanceof LiteralElement && isPowerOfTwo(num)){
                num = Integer.parseInt(((LiteralElement) element).getLiteral());
                element = literal;
            }

            if(isPowerOfTwo(num)){
                LiteralElement literalElement = new LiteralElement((int)(Math.log(num)/Math.log(2)) + "", new Type(ElementType.INT32));
                return getArithmetic("ishl", element, literalElement);
            }
        }

        return null;
    }

    private String checkDivException(Element leftOp, Element rightOp) {
        int num;

        if(rightOp.isLiteral() && isPowerOfTwo((num = Integer.parseInt(((LiteralElement) rightOp).getLiteral())))){
            LiteralElement literal = new LiteralElement((int)(Math.log(num)/Math.log(2)) + "", new Type(ElementType.INT32));
            return getArithmetic("ishr", leftOp, literal);
        }

        return null;
    }

    private String bitwise(String instruction, String leftOp, String rightOp) {
        boolean bool1 = leftOp.equals("1");
        boolean bool2 = rightOp.equals("1");

        if(instruction.equals("eq") && bool1 == bool2) return "\ticonst_1\n";
        if(instruction.equals("ne") && bool1 != bool2) return "\ticonst_1\n";
        if(instruction.equals("and") && (bool1 && bool2)) return "\ticonst_1\n";
        if(instruction.equals("or") && (bool1 || bool2)) return "\ticonst_1\n";

        return "\ticonst_0\n";
    }

    private String compare(String instruction, String leftOp, String rightOp) {
        int int1 = Integer.parseInt(leftOp);
        int int2 = Integer.parseInt(rightOp);

        if(instruction.equals("lt") && int1 < int2) return "\ticonst_1\n";
        if(instruction.equals("le") && int1 <= int2) return "\ticonst_1\n";
        if(instruction.equals("gt") && int1 > int2) return "\ticonst_1\n";
        if(instruction.equals("ge") && int1 >= int2) return "\ticonst_1\n";

        return "\ticonst_0\n";
    }

    private Boolean isPowerOfTwo(int n) {
        while(n % 2 == 0) {
            n = n / 2;
        }
        return n == 1;
    }

    private String load(Element e) {
        String s = "";
        int numLoads = 1;

        if(e.isLiteral()){
            LiteralElement literalElement = (LiteralElement) e;
            switch (literalElement.getType().getTypeOfElement()){
                case INT32, BOOLEAN -> {
                    int val = Integer.parseInt(literalElement.getLiteral());
                    if(val >= 0 && val < 6) s += "iconst_" + literalElement.getLiteral();
                    else if(val >= 0 && val < 128) s += "bipush " + literalElement.getLiteral();
                    else if(val >= 0 && val < 32768) s += "sipush " + literalElement.getLiteral();
                    else s += "ldc " + literalElement.getLiteral();
                }
                default -> s += "\tldc " + literalElement.getLiteral();
            }
        }
        else{
            Operand op = (Operand) e;
            int id;

            if(op.isParameter()) id = op.getParamId();
            else id = vars.get(op.getName()).getVirtualReg();

            if(id != -1){
                switch (op.getType().getTypeOfElement()){
                    case INT32 -> {
                        if(op instanceof ArrayOperand arrayOp){
                            s += "aload" + (id < 4 ? '_' : ' ') + id + "\n\t";
                            if(!arrayOp.getIndexOperands().isEmpty()) s += load(arrayOp.getIndexOperands().get(0));
                            s += "iaload";
                            numLoads += 2;
                        }
                        else{
                            s += "iload" + (id < 4 ? '_' : ' ') + id;
                        }
                    }
                    case BOOLEAN -> s += "iload" + (id < 4 ? '_' : ' ') + id;
                    case ARRAYREF, OBJECTREF, CLASS, STRING -> s += "aload" + (id < 4 ? '_' : ' ') + id;
                    case THIS -> s += "aload_0";
                }
            }
            else{
                s += "aload_0\n";
                s += "\tgetfield " + classUnit.getClassName() + "/" + op.getName();
                s += getType(e.getType());

                if(op instanceof ArrayOperand arrayOp){
                    s += "\n\t";
                    if(!arrayOp.getIndexOperands().isEmpty()) s += load(arrayOp.getIndexOperands().get(0));
                    s += "iaload";
                    numLoads += 2;
                }
            }
        }
        s += "\n";
        currentStack += numLoads;
        return s;
    }

    private String store(Element e) {
        String s = "";

        if(e.isLiteral()){
            LiteralElement literalElement = (LiteralElement) e;
            int id = Integer.parseInt(literalElement.getLiteral());
            if (literalElement.getType().getTypeOfElement() == ElementType.INT32) {
                s += "\tistore" + (id < 4 ? "_" : " ") + id;
            } else {
                s += "\tstore " + id;
            }
        }
        else{
            Operand op = (Operand) e;
            int id;

            if(op.isParameter()) id = op.getParamId();
            else id = vars.get(op.getName()).getVirtualReg();

            if(id != -1){
                switch (op.getType().getTypeOfElement()){
                    case INT32 -> {
                        if(op instanceof ArrayOperand){
                            s += "\tiastore";
                        }
                        else{
                            s += "\tistore" + (id < 4 ? "_" : " ") + id;
                        }
                    }
                    case BOOLEAN -> s += "\tistore" + (id < 4 ? "_" : " ") + id;
                    case ARRAYREF, OBJECTREF, CLASS, STRING -> s += "\tastore" + (id < 4 ? '_' : ' ') + id;
                    case THIS -> s += "astore_0";
                }
            }
            else{
                s +=  "\tputfield " + getType(e.getType()) + "/" + op.getName() + " " + getType(e.getType());
            }
        }
        s += "\n";
        stackLimit = Math.max(stackLimit, currentStack);
        currentStack--;
        return s;
    }

    private String getType(Type type) {
        String s = "";
        switch (type.getTypeOfElement()) {
            case INT32 -> s += "I";
            case BOOLEAN -> s += "Z";
            case ARRAYREF -> {
                ArrayType arrayType = (ArrayType) type;
                s += "[" + getType(arrayType.getElementType());
            }
            case OBJECTREF -> s += "a";
            case CLASS -> {
                ClassType classType = (ClassType) type;
                s += "L" + classType.getName();
            }
            case THIS -> s += "A";
            case STRING -> s += "Ljava/lang/String";
            case VOID -> s += "V";
        }
        return s;
    }

    private String getClassFullName(String className) {
        for(String imp : classUnit.getImports()){
                String[] splited = imp.split("\\.");

                if(splited.length == 0 && imp.equals(className)){
                    return imp.replace('.', '/');
                }
                if(splited[splited.length-1].equals(className)){
                    return splited[splited.length-1].replace('.', '/');
                }
        }

        return className;
    }
}
