package dev.dothttp.jolt.demo;

import java.io.IOException;
import java.util.Optional;

import com.bazaarvoice.jolt.Chainr;
import com.bazaarvoice.jolt.JsonUtils;
import com.bazaarvoice.jolt.Sortr;
import com.bazaarvoice.jolt.exception.JoltException;
import com.microsoft.azure.functions.ExecutionContext;
import com.microsoft.azure.functions.HttpMethod;
import com.microsoft.azure.functions.HttpRequestMessage;
import com.microsoft.azure.functions.HttpResponseMessage;
import com.microsoft.azure.functions.HttpStatus;
import com.microsoft.azure.functions.annotation.AuthorizationLevel;
import com.microsoft.azure.functions.annotation.FunctionName;
import com.microsoft.azure.functions.annotation.HttpTrigger;





class PostParams{
    private String input;
    private String spec;
    private boolean sort;
    public String getInput() {
        return input;
    }
    public boolean isSort() {
        return sort;
    }
    public void setSort(boolean sort) {
        this.sort = sort;
    }
    public String getSpec() {
        return spec;
    }
    public void setSpec(String spec) {
        this.spec = spec;
    }
    public void setInput(String input) {
        this.input = input;
    }
}


public class Function {
    @FunctionName("transform")
    public HttpResponseMessage run(
            @HttpTrigger(
                name = "req",
                methods = {HttpMethod.POST},
                authLevel = AuthorizationLevel.ANONYMOUS)
                HttpRequestMessage<Optional<PostParams>> request,
            final ExecutionContext context) {
        context.getLogger().info("Java HTTP trigger processed a request.");

        if (request.getBody().isPresent()){
            PostParams params = request.getBody().get();
            try {
                return request.createResponseBuilder(HttpStatus.OK)
                .header("content-type", "application/json")
                .body(processPostData(params)).build();
            } catch (ParseException e) {
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                    .header("content-type", "application/json")
                    .body("{\"status\": \"error\", \"reason\":\"input or spec not json parseable\"}")
                    .build();
            } catch (IOException e){
                return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
                .header("content-type", "application/json")
                .body("{\"status\": \"error\", \"reason\":\"IO Exception Occurred\"}")
                .build();
            }

        } else {
            return request.createResponseBuilder(HttpStatus.BAD_REQUEST)
            .header("content-type", "application/json")
            .body("{\"status\": \"error\", \"reason\":\"input/spec required\"}")
            .build();
        }

    }

    protected String processPostData( PostParams postParams ) throws IOException, ParseException {
        Object input, spec;

        try {
            input = JsonUtils.jsonToObject( postParams.getInput());
        }
        catch ( Exception e ) {
            throw new ParseException("input is not json parsable",e);
        }

        try {
            spec = JsonUtils.jsonToObject( postParams.getSpec());
        }
        catch ( Exception e ) {
            throw new ParseException("spec is not json parseable",e);
        }

        return  doTransform( input, spec, postParams.isSort() );
    }


    private String doTransform( Object input, Object spec, boolean doSort ) throws IOException {

        try {
            Chainr chainr = Chainr.fromSpec( spec );

            Object output = chainr.transform( input );

            if ( doSort ) {
                output = Sortr.sortJson( output );
            }

            return JsonUtils.toPrettyJsonString( output );
        }
        catch ( Exception e ) {

            StringBuilder sb = new StringBuilder();
            sb.append( "Error running the Transform.\n\n" );

            // Walk up the stackTrace printing the message for any JoltExceptions.
            Throwable exception = e;
            do {
                if ( exception instanceof JoltException ) {
                    sb.append( exception.getMessage() );
                    sb.append( "\n\n");
                }

                exception = exception.getCause();
            }
            while( exception != null );

            return sb.toString();
        }
    }
}
