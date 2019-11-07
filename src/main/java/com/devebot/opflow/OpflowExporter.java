package com.devebot.opflow;

import com.devebot.opflow.exception.OpflowBootstrapException;
import com.devebot.opflow.supports.OpflowEnvtool;
import io.prometheus.client.exporter.HTTPServer;
import io.prometheus.client.hotspot.DefaultExports;

/**
 *
 * @author acegik
 */
public class OpflowExporter {

    public final static String DEFAULT_PROM_EXPORTER_PORT = "9450";
    public final static String DEFAULT_PROM_EXPORTER_PORT_KEY = "opflow.exporter.port";
    public final static String DEFAULT_PROM_EXPORTER_PORT_ENV = "OPFLOW_EXPORTER_PORT";

    private static OpflowExporter instance;
    private static OpflowEnvtool envtool = OpflowEnvtool.instance;

    private OpflowExporter() throws OpflowBootstrapException {
        String port1 = envtool.getEnvironVariable(DEFAULT_PROM_EXPORTER_PORT_ENV, null);
        String port2 = envtool.getSystemProperty(DEFAULT_PROM_EXPORTER_PORT_KEY, port1);
        String port3 = (port2 != null) ? port2 : DEFAULT_PROM_EXPORTER_PORT;
        try {
            DefaultExports.initialize();
            HTTPServer server = new HTTPServer(Integer.parseInt(port3));
        } catch (Exception exception) {
            throw new OpflowBootstrapException("Exporter connection refused, port: " + port3, exception);
        }
    }
 
    public static OpflowExporter getInstance() throws OpflowBootstrapException {
        if (instance == null) {
            instance = new OpflowExporter();
        }
        return instance;
    }
}
