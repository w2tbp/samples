package com.w2tbp.samples.annotation;

import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.intellij.psi.augment.PsiAugmentProvider;
import com.intellij.psi.impl.light.LightFieldBuilder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class LogArgumentProvider extends PsiAugmentProvider {

    private static final String SUPPORT_ANNOTATION_NAME = "com.w2tbp.samples.annotation.MyLog";
    private static final String VAR_TYPE = "org.apache.logging.log4j.Logger";
    private static final String VAR_INIT = "org.apache.logging.log4j.LogManager.getLogger(%s)";

    @Override
    protected @NotNull <Psi extends PsiElement> List<Psi> getAugments(@NotNull PsiElement element, @NotNull Class<Psi> type, @Nullable String nameHint) {

        final List<Psi> emptyResult = Collections.emptyList();

        if (!PsiField.class.isAssignableFrom(type)) {
            return emptyResult;
        }

        if (!(element instanceof PsiClass)){
            return emptyResult;
        }

        final PsiClass psiClass = (PsiClass) element;
        if (psiClass.isAnnotationType() || psiClass.isInterface()) {
            return emptyResult;
        }

        PsiAnnotation psiAnnotation = psiClass.getAnnotation(SUPPORT_ANNOTATION_NAME);

        if (null != psiAnnotation) {
            List<PsiField> psiElementList = new ArrayList<>();
            PsiField loggerField = createLoggerField(psiClass, psiAnnotation);
            psiElementList.add(loggerField);

            return (List<Psi>) psiElementList;
        }

        return emptyResult;
    }

    private PsiField createLoggerField(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
        // called only after validation succeeded
        final Project project = psiClass.getProject();
        final PsiManager manager = psiClass.getContainingFile().getManager();

        final PsiElementFactory psiElementFactory = JavaPsiFacade.getElementFactory(project);
        String loggerType = getLoggerType(psiClass);
        if (loggerType == null) {
            throw new IllegalStateException("Invalid custom log declaration."); // validated
        }
        final PsiType psiLoggerType = psiElementFactory.createTypeFromText(loggerType, psiClass);

        LightFieldBuilder loggerField = new LightFieldBuilder(manager, getLoggerName(psiClass), psiLoggerType);
        loggerField.setContainingClass(psiClass);
        loggerField.setModifiers(PsiModifier.FINAL, PsiModifier.PRIVATE, PsiModifier.STATIC);
        loggerField.setNavigationElement(psiAnnotation);

        final String loggerInitializerParameters = createLoggerInitializeParameters(psiClass, psiAnnotation);
        final String initializerText = String.format(getLoggerInitializer(psiClass), loggerInitializerParameters);
        final PsiExpression initializer = psiElementFactory.createExpressionFromText(initializerText, psiClass);
        loggerField.setInitializer(initializer);
        return loggerField;
    }

    @NotNull
    private String createLoggerInitializeParameters(@NotNull PsiClass psiClass, @NotNull PsiAnnotation psiAnnotation) {
        return "";
    }

    public static String getLoggerName(@NotNull PsiClass psiClass) {
        return "log";
    }

    public String getLoggerType(@NotNull PsiClass psiClass) {
        return VAR_TYPE;
    }

    public String getLoggerInitializer(@NotNull PsiClass psiClass) {
        return String.format(VAR_INIT, psiClass.getQualifiedName()+".class");
    }

}