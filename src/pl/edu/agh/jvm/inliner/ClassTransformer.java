package pl.edu.agh.jvm.inliner;

import com.sun.org.apache.bcel.internal.classfile.ClassParser;
import com.sun.org.apache.bcel.internal.classfile.JavaClass;
import com.sun.org.apache.bcel.internal.classfile.Method;
import com.sun.org.apache.bcel.internal.generic.*;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;

class ClassTransformer {
    public static void main(String[] args) throws IOException {
        String callerClass = args[0];
        String classToInline = args[1];
        ClassTransformer classTransformer = new ClassTransformer(callerClass, classToInline);
        classTransformer.updateAndSave(callerClass);
    }

    private final JavaClass callerClass;
    private final ClassGen callerClassGen;
    private final ConstantPoolGen callerClassConstants;

    private final JavaClass classToInline;

    private ClassTransformer(String callerClassFile, String classToInlineFile) throws IOException {
        callerClass = parseClassFile(callerClassFile);
        callerClassGen = new ClassGen(callerClass);
        callerClassConstants = callerClassGen.getConstantPool();

        classToInline = parseClassFile(classToInlineFile);
    }

    private JavaClass parseClassFile(String classFile) throws IOException {
        ClassParser parser = new ClassParser(new FileInputStream(classFile), classFile);
        return parser.parse();
    }

    private void updateAndSave(String resultFile) throws IOException {
        updateMethods();
        saveClass(resultFile);
    }

    private void saveClass(String path) throws IOException {
        JavaClass updatedCallerClass = callerClassGen.getJavaClass();
        updatedCallerClass.dump(new File(path));
    }

    private void updateMethods() {
        for (Method method : callerClass.getMethods()) {
            if (isNotAnInitializer(method)) {
                updateMethod(method);
            }
        }
    }

    private boolean isNotAnInitializer(Method method) {
        return !Arrays.asList("<init>", "<clinit>").contains(method.getName());
    }

    private void updateMethod(Method method) {
        Method updatedMethod = getUpdatedMethod(method);
        callerClassGen.replaceMethod(method, updatedMethod);
    }

    private Method getUpdatedMethod(Method method) {
        return new MethodUpdater(method, callerClass, callerClassConstants, classToInline).update();
    }

}