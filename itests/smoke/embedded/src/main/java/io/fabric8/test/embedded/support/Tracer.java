package io.fabric8.test.embedded.support;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;

/**
 * Created by chirino on 6/12/14.
 */
public class Tracer {

    static private final Logger LOG = LoggerFactory.getLogger(Tracer.class);
    public static final List<String> traces = Collections.synchronizedList(new ArrayList<String>());

    public static int count(String msg) {
        int rc=0;
        for (String trace : traces) {
            if( msg.equals(trace) ) {
                rc++;
            }
        }
        return rc;
    }

    static public void trace() {
        _trace(null);
    }
    static public void trace(String message) {
        _trace(message);
    }

    static private void _trace(String message) {
        StackTraceElement[] st = new Exception().fillInStackTrace().getStackTrace();
        String x = st[2].getClassName() + ":" + st[2].getMethodName();
        if( message!=null ) {
            x += ":" + message;
        }
        traces.add(x);
        LOG.info(new Date() + " => " + x);
    }

}
