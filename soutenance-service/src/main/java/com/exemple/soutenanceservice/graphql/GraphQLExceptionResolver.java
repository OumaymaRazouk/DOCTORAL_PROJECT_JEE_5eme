package com.exemple.soutenanceservice.graphql;

import graphql.GraphQLError;
import graphql.GraphqlErrorBuilder;
import graphql.schema.DataFetchingEnvironment;
import org.springframework.graphql.execution.DataFetcherExceptionResolverAdapter;
import org.springframework.graphql.execution.ErrorType;
import org.springframework.stereotype.Component;

@Component
public class GraphQLExceptionResolver extends DataFetcherExceptionResolverAdapter {

    @Override
    protected GraphQLError resolveToSingleError(Throwable ex, DataFetchingEnvironment env) {
        Throwable root = unwrap(ex);

        if (root instanceof IllegalArgumentException || root instanceof IllegalStateException) {
            return GraphqlErrorBuilder.newError(env)
                    .errorType(ErrorType.BAD_REQUEST)
                    .message(root.getMessage())
                    .build();
        }

        return null;
    }

    private Throwable unwrap(Throwable ex) {
        Throwable current = ex;
        while (current.getCause() != null
                && (current.getClass().getName().contains("CompletionException")
                || current.getClass().getName().contains("InvocationTargetException")
                || current.getClass().getName().contains("UndeclaredThrowableException")
                || current.getClass().getName().contains("DataFetcherException"))) {
            current = current.getCause();
        }
        return current;
    }
}
