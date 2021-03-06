/*
 * Copyright 2013 Radoslav Husár
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jboss.test.clusterbench.common.debug;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Date;
import java.util.Enumeration;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.jboss.test.clusterbench.common.ClusterBenchConstants;
import org.jboss.test.clusterbench.common.SerialBean;

/**
 * Servlet which outputs debug information provided by the {@link #getContainerSpecificDebugInfo(HttpServletRequest)} method.
 *
 * @author Radoslav Husar
 * @version April 2012
 */
public abstract class AbstractCommonDebugServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(AbstractCommonDebugServlet.class.getName());
    public static final String KEY = AbstractCommonDebugServlet.class.getName();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        HttpSession session = req.getSession(true);
        PrintWriter out = resp.getWriter();

        if (session.isNew()) {
            log.log(Level.INFO, "New session created: {0}", session.getId());
            session.setAttribute(KEY, new SerialBean());
        } else if (session.getAttribute(KEY) == null) {
            log.log(Level.INFO, "Session is not new, creating SerialBean: {0}", session.getId());
            session.setAttribute(KEY, new SerialBean());
        }

        SerialBean bean = (SerialBean) session.getAttribute(KEY);

        resp.setContentType("text/plain");

        // Readonly?
        if (req.getParameter(ClusterBenchConstants.READONLY) != null) {
            out.print(bean.getSerial());
            out.println(this.getContainerSpecificDebugInfo(req));
            return;
        }

        int serial = bean.getSerial();
        bean.setSerial(serial + 1);

        // Now store bean in the session
        session.setAttribute(KEY, bean);

        // Write out request headers
        Enumeration<String> headers = req.getHeaderNames();
        while (headers.hasMoreElements()) {
            String header = headers.nextElement();
            out.println("Request header: " + header + "=" + req.getHeader(header));
        }

        // Common debug info
        out.println("Serial: " + serial);
        // Get the session ID with the route (if present)
        out.println("Session ID: " + req.getSession().getId());
        // Current invocation time; useful for testing session timeouts
        out.println("Current time: " + new Date());
        // Display Server/Local ports
        out.println("ServletRequest.getServerPort(): " + req.getServerPort());
        out.println("ServletRequest.getLocalPort(): " + req.getLocalPort());
        // Fetch just the node name for now
        out.println("Node name: " + System.getProperty("jboss.node.name"));

        // Container/EE-specific debug info
        out.println(this.getContainerSpecificDebugInfo(req));

        // Invalidate?
        if (req.getParameter(ClusterBenchConstants.INVALIDATE) != null) {
            log.log(Level.INFO, "Invalidating: {0}", session.getId());
            session.invalidate();
        }
    }

    @Override
    public String getServletInfo() {
        return "Debug servlet.";
    }

    /**
     * Implement this method to print out any debug info specific to the container or EE version.
     *
     * @param req HttpServletRequest
     * @return debug info String
     */
    abstract public String getContainerSpecificDebugInfo(HttpServletRequest req);
}
