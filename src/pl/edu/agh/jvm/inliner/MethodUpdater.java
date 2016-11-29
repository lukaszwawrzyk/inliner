package pl.edu.agh.jvm.inliner;

import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;

class MethodUpdater {
    private final Method method;
    private final JavaClass callerClass;
    private final ConstantPoolGen callerClassConstants;
    private final JavaClass classToInline;
    private final ConstantPoolGen classToInlineConstants;

    MethodUpdater(Method method, JavaClass callerClass, ConstantPoolGen callerClassConstants, JavaClass classToInline) {
        this.method = method;
        this.callerClass = callerClass;
        this.callerClassConstants = callerClassConstants;
        this.classToInline = classToInline;
        ClassGen classToInlineGen = new ClassGen(classToInline);
        classToInlineConstants = classToInlineGen.getConstantPool();
    }

    Method update() {
        MethodGen methodToUpdate = new MethodGen(method, callerClass.getClassName(), callerClassConstants);
        InstructionList originalInstructions = methodToUpdate.getInstructionList();

        for (InstructionHandle instructionHandle : originalInstructions.getInstructionHandles()) {
            Instruction instruction = instructionHandle.getInstruction();
            if (instruction instanceof INVOKEVIRTUAL) {
                INVOKEVIRTUAL invoke = (INVOKEVIRTUAL) instruction;
                if (invokeIsOnClassToInline(invoke)) {
                    performInlining(originalInstructions, instructionHandle, invoke);
                }
            }
        }

        methodToUpdate.setMaxStack();
        methodToUpdate.setMaxLocals();
        methodToUpdate.removeLineNumbers();

        return methodToUpdate.getMethod();
    }

    private boolean invokeIsOnClassToInline(INVOKEVIRTUAL invoke) {
        String invokedClass = invoke.getClassName(callerClassConstants);
        return classToInline.getClassName().equals(invokedClass);
    }

    private void performInlining(InstructionList originalInstructions, InstructionHandle instructionHandle, INVOKEVIRTUAL invoke) {
        InstructionList instructionsToInline = getImplementationToInline(invoke);
        String classToInlineName = classToInline.getClassName();
        new Inliner(originalInstructions, instructionHandle, instructionsToInline, callerClassConstants, classToInlineConstants, classToInlineName).inlineInstructions();
    }


    private InstructionList getImplementationToInline(INVOKEVIRTUAL invoke) {
        String methodToInlineId = getMethodId(invoke);
        for (Method method : classToInline.getMethods()) {
            if (getMethodId(method).equals(methodToInlineId)) {
                MethodGen methodGen = new MethodGen(method, classToInline.getClassName(), classToInlineConstants);
                return methodGen.getInstructionList();
            }
        }
        throw new RuntimeException("Tried to inline method that was not present in class to inline");
    }


    private String getMethodId(Method method) {
        String methodName = method.getName();
        String methodSignature = method.getSignature();
        return methodName + methodSignature;
    }

    private String getMethodId(INVOKEVIRTUAL invoke) {
        String methodName = invoke.getMethodName(callerClassConstants);
        String methodSignature = invoke.getSignature(callerClassConstants);
        return methodName + methodSignature;
    }
}
