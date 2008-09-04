package org.carrot2.dcs;

import org.kohsuke.args4j.*;
import org.mortbay.jetty.Server;
import org.mortbay.jetty.nio.SelectChannelConnector;
import org.mortbay.jetty.webapp.WebAppContext;

/**
 *
 */
public class DcsApp
{
    @Option(name = "-port", usage = "Port number to bind to")
    int port = 8080;

    @Option(name = "-v", aliases =
    {
        "--verbose"
    }, required = false, usage = "Print detailed messages")
    boolean verbose;

    String appName;

    Server server;

    DcsApp(String appName)
    {
        this.appName = appName;
    }

    void go() throws Exception
    {
        start();
    }

    void start() throws Exception
    {
        start(null);
    }

    void start(String webPathPrefix) throws Exception
    {
        server = new Server();
        SelectChannelConnector connector = new SelectChannelConnector();
        connector.setPort(port);
        connector.setAcceptQueueSize(20);
        server.addConnector(connector);

        WebAppContext wac = new WebAppContext();
        wac.setClassLoader(Thread.currentThread().getContextClassLoader());
        wac.setContextPath("/");
        wac.setWar(webPathPrefix != null ? webPathPrefix + "/web" : "web");
        if (System.getProperty("dcs.development.mode") != null)
        {
            wac.setDefaultsDescriptor("etc/webdefault.xml");
        }
        server.setHandler(wac);
        server.setStopAtShutdown(true);

        // Start the http server
        server.start();
    }

    void stop() throws Exception
    {
        server.stop();
    }

    public static void main(String [] args) throws Exception
    {
        final DcsApp dcs = new DcsApp("dcs");

        final CmdLineParser parser = new CmdLineParser(dcs);
        parser.setUsageWidth(80);

        try
        {
            parser.parseArgument(args);
        }
        catch (CmdLineException e)
        {
            System.out.print("Usage: " + dcs.appName);
            parser.printSingleLineUsage(System.out);
            System.out.println();
            parser.printUsage(System.out);

            System.out.println("\n" + e.getMessage());
            return;
        }

        dcs.go();
    }
}
