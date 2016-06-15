/**
 * Copyright 2012 Rafal Lewczuk <rafal.lewczuk@jitlogic.com>
 * <p/>
 * This is free software. You can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 * <p/>
 * This software is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * <p/>
 * You should have received a copy of the GNU General Public License
 * along with this software. If not, see <http://www.gnu.org/licenses/>.
 */

package com.jitlogic.zorka.core.spy;

import java.util.Set;

import com.jitlogic.zorka.common.tracedata.*;
import com.jitlogic.zorka.common.util.ZorkaConfig;
import com.jitlogic.zorka.common.util.ZorkaLog;
import com.jitlogic.zorka.common.util.ZorkaLogger;
import com.jitlogic.zorka.core.spy.plugins.*;
import com.jitlogic.zorka.core.util.OverlayClassLoader;

/**
 * Tracer library contains functions for configuring and using tracer.
 *
 * @author rafal.lewczuk@jitlogic.com
 */
public class TracerLib {

    public static final ZorkaLog log = ZorkaLogger.getLog(TracerLib.class);


    private Tracer tracer;

    private SymbolRegistry symbolRegistry;

    private ZorkaConfig config;


    /**
     * Creates tracer library object.
     *
     * @param tracer reference to spy instance
     */
    public TracerLib(SymbolRegistry symbolRegistry, Tracer tracer, ZorkaConfig config) {
        this.symbolRegistry = symbolRegistry;
        this.tracer = tracer;
        this.config = config;
    }


    public void traceBufOutput(TraceBufOutput bufOutput) {
        tracer.setBufOutput(bufOutput);
    }


    public void clearOutputs() {
        tracer.shutdown();
    }


    /**
     * Adds matching method to tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void include(String... matchers) {
        for (String matcher : matchers) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Tracer include: " + matcher);
            tracer.include(SpyMatcher.fromString(matcher.toString()));
        }
    }

    public void include(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Tracer include: " + matcher);
            tracer.include(matcher);
        }
    }

    /**
     * Exclude classes/methods from tracer.
     *
     * @param matchers spy matcher objects (created using spy.byXxxx() functions)
     */
    public void exclude(String... matchers) {
        for (String matcher : matchers) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Tracer exclude: " + matcher);
            tracer.include(SpyMatcher.fromString(matcher.toString()).exclude());
        }
    }

    public void exclude(SpyMatcher... matchers) {
        for (SpyMatcher matcher : matchers) {
            log.info(ZorkaLogger.ZAG_CONFIG, "Tracer exclude: " + matcher);
            tracer.include((matcher).exclude());
        }

    }

    public String listIncludes() {
        StringBuilder sb = new StringBuilder();
        for (SpyMatcher sm : tracer.getMatcherSet().getMatchers()) {
            sb.append(sm.hasFlags(SpyMatcher.EXCLUDE_MATCH) ? "excl: " : "incl: ");
            sb.append(sm);
            sb.append("\n");
        }
        return sb.toString();
    }

    /**
     * Starts a new (named) trace.
     *
     * @param name trace name
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name) {
        return begin(name, -1);
    }


    /**
     * Starts new trace.
     *
     * @param name             trace name
     * @param minimumTraceTime minimum trace time
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name, long minimumTraceTime) {
        return begin(name, minimumTraceTime, 0);
    }


    /**
     * Starts new trace.
     *
     * @param name             trace name
     * @param minimumTraceTime minimum trace time
     * @param flags            initial flags
     * @return spy processor object marking new trace
     */
    public SpyProcessor begin(String name, long minimumTraceTime, int flags) {
        return new TraceBeginProcessor(tracer, name, minimumTraceTime * 1000000L, flags, symbolRegistry);
    }


    public void traceBegin(String name) {
        traceBegin(name, 0);
    }


    public void traceBegin(String name, long minimumTraceTime) {
        traceBegin(name, minimumTraceTime, 0);
    }


    public void traceBegin(String name, long minimumTraceTime, int flags) {
        TraceRecorder traceBuilder = tracer.getRecorder();
        traceBuilder.traceBegin(symbolRegistry.stringId(name), System.currentTimeMillis(), flags);
        traceBuilder.setMinimumTraceTime(minimumTraceTime);
    }


    public SpyProcessor inTrace(String traceName) {
        return new TraceCheckerProcessor(tracer, symbolRegistry.stringId(traceName));
    }


    public boolean isInTrace(String traceName) {
        return tracer.getRecorder().isInTrace(symbolRegistry.stringId(traceName));
    }


    /**
     * Creates spy processor that attaches attribute to trace record.
     *
     * @param attrName destination attribute name (in trace data)
     * @param srcField source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor attr(String attrName, String srcField) {
        return attr(attrName, null, srcField);
    }


    /**
     * Creates spy processor that attaches
     * @param traceName
     * @param attrName
     * @param srcField
     * @return
     */
    public SpyProcessor traceAttr(String traceName, String attrName, String srcField) {
        return traceAttr(traceName, attrName, null, srcField);
    }


    public SpyProcessor getTraceAttr(String dstField, String attrName) {
        return getTraceAttr(dstField, null, attrName);
    }

    /**
     * Looks for trace of given attribute name in trace stack.
     *
     * @param dstField
     * @param traceName
     * @param attrName
     * @return
     */
    public SpyProcessor getTraceAttr(String dstField, String traceName, String attrName) {
        return new TraceAttrGetterProcessor(tracer, dstField,
            traceName != null ? symbolRegistry.stringId(traceName) : 0,
            symbolRegistry.stringId(attrName));
    }

    /**
     * Creates spy processor that formats a string and attaches it as attribute to trace record.
     *
     * @param attrName  destination attribute name (in trace data)
     * @param srcFormat source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor formatAttr(String attrName, String srcFormat) {
        return formatAttr(attrName, null, srcFormat);
    }


    public SpyProcessor formatTraceAttr(String traceName, String attrName, String srcFormat) {
        return formatTraceAttr(traceName, attrName, null, srcFormat);
    }


    /**
     * Creates spy processor that attaches tagged attribute to trace record.
     *
     * @param attrName destination attribute name (in trace data)
     * @param attrTag  attribute tag;
     * @param srcField source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor attr(String attrName, String attrTag, String srcField) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.FIELD_GETTING_PROCESSOR,
                srcField, attrName, attrTag);
    }


    public SpyProcessor traceAttr(String traceName, String attrName, String attrTag, String srcField) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.FIELD_GETTING_PROCESSOR,
                srcField, traceName, attrName, attrTag);
    }


    /**
     * Creates spy processor that formats a string and attaches it as tagged attribute to trace record.
     *
     * @param attrName  destination attribute name (in trace data)
     * @param attrTag   attribute tag;
     * @param srcFormat source field name (from spy record)
     * @return spy processor object adding new trace attribute
     */
    public SpyProcessor formatAttr(String attrName, String attrTag, String srcFormat) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.STRING_FORMAT_PROCESSOR,
                srcFormat, attrName, attrTag);
    }


    public SpyProcessor formatTraceAttr(String traceName, String attrName, String attrTag, String srcFormat) {
        return new TraceAttrProcessor(symbolRegistry, tracer, TraceAttrProcessor.STRING_FORMAT_PROCESSOR,
                srcFormat, traceName, attrName, attrTag);
    }


    /**
     * Adds trace attribute to trace record immediately. This is useful for programmatic attribute setting.
     *
     * @param attrName attribute name
     * @param value    attribute value
     */
    public void newAttr(String attrName, Object value) {
        tracer.getRecorder().newAttr(-1, symbolRegistry.stringId(attrName), value);
    }


    /**
     * @param traceName
     * @param attrName
     * @param value
     */
    public void newTraceAttr(String traceName, String attrName, Object value) {
        tracer.getRecorder().newAttr(symbolRegistry.stringId(traceName), symbolRegistry.stringId(attrName), value);
    }

    public SpyProcessor markError() {
        return flags(0);
    }

    /**
     * Creates spy processor that sets flags in trace marker.
     *
     * @param flags flags to set
     * @return spy processor object
     */
    public SpyProcessor flags(int flags) {
        return new TraceFlagsProcessor(tracer, null, 0, flags);
    }


    public SpyProcessor traceFlags(String traceName, int flags) {
        return new TraceFlagsProcessor(tracer, null, symbolRegistry.stringId(traceName), flags);
    }


    public void newFlags(int flags) {
        tracer.getRecorder().markTraceFlags(0, flags);
    }


    /**
     * Creates spy processor that sets flags in trace marker only if given record field is null.
     *
     * @param srcField spy record field to be checked
     * @param flags    flags to set
     * @return spy processor object
     */
    public SpyProcessor flags(String srcField, int flags) {
        return new TraceFlagsProcessor(tracer, srcField, 0, flags);
    }


    public SpyProcessor traceFlags(String srcField, String traceName, int flags) {
        return new TraceFlagsProcessor(tracer, srcField, symbolRegistry.stringId(traceName), flags);
    }




    public SpyProcessor filterBy(String srcField, Boolean defval, Set<Object> yes, Set<Object> no, Set<Object> maybe) {
        return new TraceFilterProcessor(tracer, srcField, defval, yes, no, maybe);
    }



    public long getTracerMinMethodTime() {
        return Tracer.getMinMethodTime();
    }


    /**
     * Sets minimum traced method execution time. Methods that took less time
     * will be discarded from traces and will only reflect in summary call/error counters.
     *
     * @param methodTime minimum execution time (in nanoseconds, 250 microseconds by default)
     */
    public void setTracerMinMethodTime(int methodTime) {
        Tracer.setMinMethodTime(methodTime);
    }


    /**
     * Sets minimum traced method execution time. Methods that took less time
     * will be discarded from traces and will only reflect in summary call/error counters.
     *
     * @param methodTime minimum execution time (in nanoseconds, 250 microseconds by default)
     */
    public void setTracerMinMethodTime(long methodTime) {
        Tracer.setMinMethodTime(methodTime);
    }


    public long getTracerMinTraceTime() {
        return Tracer.getMinTraceTime() / 1000000L;
    }


    /**
     * Sets minimum trace execution time. Traces that laster for shorted period
     * of time will be discarded. Not that this is default setting that can be
     * overridden with spy.begin() method.
     *
     * @param traceTime minimum trace execution time (50 milliseconds by default)
     */
    public void setTracerMinTraceTime(int traceTime) {
        Tracer.setMinTraceTime(traceTime * 1000000L);
    }


    /**
     * Sets minimum trace execution time. Traces that laster for shorted period
     * of time will be discarded. Not that this is default setting that can be
     * overridden with spy.begin() method.
     *
     * @param traceTime minimum trace execution time (50 milliseconds by default)
     */
    public void setTracerMinTraceTime(long traceTime) {
        Tracer.setMinTraceTime(traceTime * 1000000L);
    }


    /**
     * Sets maximum number of records that will be stored in a single trace.
     * This setting prevents agent from overrunning memory when instrumented
     * code has very long (and complex) execution path. After maximum number
     * is reached, all remaining records will be discarded but numbers of calls
     * and errors of discarded methods will be reflected in summary data.
     *
     * @param maxRecords maximum numbner of trace records
     */
    public void setTracerMaxTraceRecords(int maxRecords) {
        Tracer.setMaxTraceRecords(maxRecords);
    }


    public void setTracerMaxTraceRecords(long maxRecords) {
        Tracer.setMaxTraceRecords((int) maxRecords);
    }


    public int getTracerMaxTraceRecords() {
        return Tracer.getMaxTraceRecords();
    }


    public void setTraceSpyMethods(boolean tsm) {
        tracer.setTraceSpyMethods(tsm);
    }


    public boolean isTraceSpyMethods() {
        return tracer.isTraceSpyMethods();
    }


    public ClassLoader overlayClassLoader(ClassLoader parent, String pattern, ClassLoader overlay) {
        return new OverlayClassLoader(parent, pattern, overlay);
    }

}
