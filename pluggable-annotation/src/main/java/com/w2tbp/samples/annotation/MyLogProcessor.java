package com.w2tbp.samples.annotation;

import com.sun.tools.javac.api.JavacTrees;
import com.sun.tools.javac.code.Flags;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.tree.JCTree;
import com.sun.tools.javac.tree.TreeMaker;
import com.sun.tools.javac.tree.TreeTranslator;
import com.sun.tools.javac.util.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import java.util.Set;

@SupportedAnnotationTypes("com.w2tbp.samples.annotation.MyLog")
public class MyLogProcessor extends AbstractProcessor {

    private JavacTrees javacTrees;
    private TreeMaker treeMaker;
    private Names names;

    private static final Class<MyLog> ANNOTATION_TO_PROCESS = MyLog.class;
    private static final String LOG_CLASS_TYPE_NAME = Logger.class.getName();
    private static final String LOG_FACTORY_CLASS_TYPE_NAME = LogManager.class.getName();

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        this.javacTrees = JavacTrees.instance(processingEnv);
        Context context = ((JavacProcessingEnvironment) processingEnv).getContext();
        this.treeMaker = TreeMaker.instance(context);
        this.names = Names.instance(context);
    }

    @Override
    public SourceVersion getSupportedSourceVersion() {
        return SourceVersion.latest();
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        JCTree.JCExpression varTypeName = strToJCExpression(LOG_CLASS_TYPE_NAME);
        JCTree.JCExpression varFactoryName = strToJCExpression(LOG_FACTORY_CLASS_TYPE_NAME);
        Name varName = names.fromString("log");

        Set<? extends Element> elements = roundEnv.getElementsAnnotatedWith(ANNOTATION_TO_PROCESS);
        for (Element element : elements) {
            String className = element.getSimpleName().toString()+".class";

            JCTree tree = javacTrees.getTree(element);
            tree.accept(new TreeTranslator() {
                @Override
                public void visitClassDef(JCTree.JCClassDecl jcClassDecl) {
                    JCTree.JCVariableDecl variableDecl = treeMaker.VarDef(
                            treeMaker.Modifiers(Flags.PRIVATE + Flags.STATIC + Flags.FINAL),
                            varName, varTypeName,
                            treeMaker.Apply(
                                    List.nil(),
                                    treeMaker.Select(
                                            varFactoryName,
                                            names.fromString("getLogger")
                                    ),
                                    List.of(strToJCExpression(className))
                            )
                    );
                    jcClassDecl.defs = jcClassDecl.defs.prepend(variableDecl);
                    super.visitClassDef(jcClassDecl);
                }
            });
        }
        return true;
    }

    private JCTree.JCExpression strToJCExpression(String components) {
        String[] componentArray = components.split("\\.");
        JCTree.JCExpression expr = treeMaker.Ident(names.fromString(componentArray[0]));
        for (int i = 1; i < componentArray.length; i++) {
            expr = treeMaker.Select(expr, names.fromString(componentArray[i]));
        }
        return expr;
    }

}
