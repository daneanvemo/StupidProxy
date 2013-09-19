package info.wahlman.stupidproxy

import groovyx.net.http.HTTPBuilder
import groovyx.net.http.Method
import org.eclipse.jetty.server.Request
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.handler.AbstractHandler

import javax.servlet.ServletException
import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpServletResponse

class StupidProxy extends AbstractHandler {

    private String serverUrl

    public static void main(String[] args) {
        String serverUrl = "http://localhost:9090"
        if (args.length > 0) {
            serverUrl = args[0]
        }

        int serverPort = 8081
        if (args.length > 0) {
            serverPort = args[1].toInteger()
        }

        println("Starting StupidProxy on port $serverPort pointing at $serverUrl")

        Server server = new Server(serverPort)
        server.setHandler(new StupidProxy(serverUrl: serverUrl))
        server.start()
        server.join()
    }

    @Override
    void handle(String target, Request baseRequest, HttpServletRequest requestParam, HttpServletResponse responseParam) throws IOException, ServletException {
        HTTPBuilder builder = new HTTPBuilder(serverUrl)
        Method method = Method.find { it.toString() ==  requestParam.method } as Method
        Map<String, String> headerMap = new HashMap<>()
        requestParam.headerNames.each {
            if (it.toLowerCase().startsWith("postman-")) {
                headerMap.put(it.substring("postman-".length()), requestParam.getHeader(it))
            } else {
                headerMap.put(it, requestParam.getHeader(it))
            }
        }

        if (method) {
            builder.request(method) {
                headers = headerMap
                uri.path = target
                response.success = response.failure = { resp, reader ->
                    responseParam.status = resp.statusLine.statusCode
                    responseParam.contentType = resp.headers.'Content-Type'
                    if (reader) responseParam.getWriter().append(reader.text)
                }
            }
        }
        baseRequest.setHandled(true)
    }
}
