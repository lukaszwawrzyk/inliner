package pl.edu.agh.jvm.inliner;


import com.sun.org.apache.bcel.internal.classfile.Constant;
import com.sun.org.apache.bcel.internal.generic.*;

class Inliner {
    private final InstructionList originalInstructions;
    private final InstructionHandle invokeHandle;
    private final InstructionList instructionsToInline;
    private final InstructionList inlinedInstructions;
    private final ConstantPoolGen originalConstants;
    private final ConstantPoolGen toInlineConstants;

    Inliner(
        InstructionList originalInstructions,
        InstructionHandle invokeHandle,
        InstructionList instructionsToInline,
        ConstantPoolGen originalConstants,
        ConstantPoolGen toInlineConstants
    ) {
        this.originalInstructions = originalInstructions;
        this.invokeHandle = invokeHandle;
        this.instructionsToInline = instructionsToInline;
        this.originalConstants = originalConstants;
        this.toInlineConstants = toInlineConstants;

        this.inlinedInstructions = new InstructionList();
    }

    void inlineInstructions() {
        transformInstructionsToInline();
        replaceInvokeWithInlinedInstructions();
    }

    private void transformInstructionsToInline() {
        for (Instruction instruction : instructionsToInline.getInstructions()) {
            transformInstruction(instruction);
        }
    }

    private void transformInstruction(Instruction instruction) {
        if (isLoadThisPointer(instruction)) {
            replaceThisPointerWithInlinedObjectPointer();
        } else if (isReturnInstruction(instruction)) {
            replaceReturnWithGoto();
        } else if (isConstantPoolInstruction(instruction)) {
            putInstructionWithUpdatedConstantPool(instruction);
        } else {
            keepUnchanged(instruction);
        }
    }

    private void putInstructionWithUpdatedConstantPool(Instruction instruction) {
        CPInstruction cpInstruction = updateIndexToConstantPool((CPInstruction) instruction);
        append(cpInstruction);
    }

    private CPInstruction updateIndexToConstantPool(CPInstruction instruction) {
        int index = copyConstantFromToInlineClass(instruction.getIndex());
        instruction.setIndex(index);
        return instruction;
    }

    private int copyConstantFromToInlineClass(int index) {
        Constant constant = toInlineConstants.getConstant(index);
        return originalConstants.addConstant(constant, toInlineConstants);
    }

    private boolean isConstantPoolInstruction(Instruction instruction) {
        return instruction instanceof CPInstruction;
    }

    private void replaceThisPointerWithInlinedObjectPointer() {
        Instruction loadObjectToInlinePointerInstruction = invokeHandle.getPrev().getInstruction();
        append(loadObjectToInlinePointerInstruction);
    }

    private BranchHandle replaceReturnWithGoto() {
        return inlinedInstructions.append(new GOTO(invokeHandle.getNext()));
    }

    private InstructionHandle keepUnchanged(Instruction instruction) {
        return append(instruction);
    }

    private InstructionHandle append(Instruction instruction) {
        return inlinedInstructions.append(instruction);
    }

    private boolean isReturnInstruction(Instruction instruction) {
        return instruction instanceof ReturnInstruction;
    }

    private boolean isLoadThisPointer(Instruction instruction) {
        if (instruction instanceof ALOAD) {
            ALOAD aload = (ALOAD) instruction;
            return aload.getIndex() == 0;
        } else {
            return false;
        }
    }

    private void replaceInvokeWithInlinedInstructions() {
        try {
            InstructionHandle appendedHead = originalInstructions.append(invokeHandle, inlinedInstructions);
            originalInstructions.redirectBranches(invokeHandle.getPrev(), appendedHead);
            originalInstructions.delete(clearLineNumberTargets(invokeHandle.getPrev()), clearLineNumberTargets(invokeHandle));
        } catch (TargetLostException e) {
            e.printStackTrace();
        }
    }

    private InstructionHandle clearLineNumberTargets(InstructionHandle handle) {
        if (handle.getTargeters() != null) {
            for (InstructionTargeter instructionTargeter : handle.getTargeters()) {
                if (instructionTargeter instanceof LineNumberGen) {
                    handle.removeTargeter(instructionTargeter);
                }
            }
        }
        return handle;
    }
}
