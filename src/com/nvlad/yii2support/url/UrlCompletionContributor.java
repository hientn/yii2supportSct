package com.nvlad.yii2support.url;

import com.intellij.codeInsight.completion.CompletionParameters;
import com.intellij.codeInsight.completion.CompletionProvider;
import com.intellij.codeInsight.completion.CompletionResultSet;
import com.intellij.codeInsight.completion.CompletionType;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.patterns.ElementPattern;
import com.intellij.patterns.PlatformPatterns;
import com.intellij.psi.PsiElement;
import com.intellij.util.ProcessingContext;
import com.jetbrains.php.lang.parser.parsing.expressions.Expression;
import com.jetbrains.php.lang.psi.elements.ArrayCreationExpression;
import com.jetbrains.php.lang.psi.elements.Method;
import com.jetbrains.php.lang.psi.elements.MethodReference;
import com.nvlad.yii2support.database.ParamsCompletionProvider;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by oleg on 25.04.2017.
 */
public class UrlCompletionContributor extends com.intellij.codeInsight.completion.CompletionContributor  {
    public UrlCompletionContributor() {
        extend(CompletionType.BASIC, PlatformPatterns.psiElement(), new CompletionProvider<CompletionParameters>() {
            @Override
            protected void addCompletions(@NotNull CompletionParameters completionParameters, ProcessingContext processingContext, @NotNull CompletionResultSet completionResultSet) {
                PsiElement position = completionParameters.getPosition();
                if (position.getParent().getParent().getParent() instanceof ArrayCreationExpression &&
                   position.getParent().getParent().getParent().getParent().getParent() instanceof MethodReference) {
                     MethodReference mRef = (MethodReference)position.getParent().getParent().getParent().getParent().getParent();
                     if (mRef.getName().equals("to") && mRef.getClassReference() != null && mRef.getClassReference().getName().equals("Url")) {
                         HashMap<String, Method> routes = UrlUtils.getRoutes(position.getProject());
                         Iterator it = routes.entrySet().iterator();
                         while (it.hasNext()) {
                             Map.Entry pair = (Map.Entry)it.next();
                             Method method = (Method)pair.getValue();

                             LookupElementBuilder builder = LookupElementBuilder.create(pair.getValue(), pair.getKey().toString());
                             builder = builder.withTypeText("appName", true);
                             completionResultSet.addElement(builder);
                             it.remove();
                         }
                     }
                }
            }
        });
    }

    @Override
    public boolean invokeAutoPopup(@NotNull PsiElement position, char typeChar) {
        if ((typeChar == '\'' || typeChar == '"') && position.getParent() instanceof ArrayCreationExpression) {
            return true;
        }

        return false;
    }

    private static ElementPattern<PsiElement> ElementPattern() {
        return
                PlatformPatterns.or(
                        PlatformPatterns.psiElement().withSuperParent(3, ArrayCreationExpression.class),
                        PlatformPatterns.psiElement().withSuperParent(4, ArrayCreationExpression.class));
    }
}
