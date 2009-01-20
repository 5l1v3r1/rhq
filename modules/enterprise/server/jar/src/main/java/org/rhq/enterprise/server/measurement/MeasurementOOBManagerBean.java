/*
 * RHQ Management Platform
 * Copyright (C) 2005-2008 Red Hat, Inc.
 * All rights reserved.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation version 2 of the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 675 Mass Ave, Cambridge, MA 02139, USA.
 */
package org.rhq.enterprise.server.measurement;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;
import java.util.List;

import javax.ejb.Stateless;
import javax.ejb.TransactionAttribute;
import javax.ejb.TransactionAttributeType;
import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import javax.persistence.Query;
import javax.sql.DataSource;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import org.rhq.core.db.DatabaseType;
import org.rhq.core.db.DatabaseTypeFactory;
import org.rhq.core.db.OracleDatabaseType;
import org.rhq.core.db.PostgresqlDatabaseType;
import org.rhq.core.domain.auth.Subject;
import org.rhq.core.domain.measurement.MeasurementDataNumeric1H;
import org.rhq.core.domain.measurement.MeasurementOOB;
import org.rhq.core.domain.measurement.composite.MeasurementOOBComposite;
import org.rhq.core.util.jdbc.JDBCUtil;
import org.rhq.enterprise.server.RHQConstants;

/**
 * Manager bean for Out-of-Bound measurements.
 *
 * @author Heiko W. Rupp
 */
@Stateless
@javax.annotation.Resource(name = "RHQ_DS", mappedName = RHQConstants.DATASOURCE_JNDI_NAME)
public class MeasurementOOBManagerBean implements MeasurementOOBManagerLocal {

    private final Log log = LogFactory.getLog(MeasurementOOBManagerBean.class);

    @PersistenceContext(unitName = RHQConstants.PERSISTENCE_UNIT_NAME)
    private EntityManager entityManager;

    @javax.annotation.Resource(name = "RHQ_DS")
    private DataSource rhqDs;


    /**
     * Compute oobs from the values in the 1h measurement table that just got added.
     * For the total result, this is an incremental computation. The idea is that
     * it gets run *directly* after the 1h compression (and the baseline recalculation too)
     * @param subject Subject of the caller
     * @param begin Start time of the 1h entries to look at
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void computeOOBsFromHourBeginingAt(Subject subject, long begin) {

        Connection conn = null;
        PreparedStatement stmt = null;

        try {
            log.info("Calculating OOBs for hour " + new Date(begin));
            conn = rhqDs.getConnection();
            DatabaseType dbType = DatabaseTypeFactory.getDatabaseType(conn);

            if (dbType instanceof PostgresqlDatabaseType)
                stmt = conn.prepareStatement(MeasurementOOB.INSERT_QUERY_POSTGRES);
            else if (dbType instanceof OracleDatabaseType)
                stmt = conn.prepareStatement(MeasurementOOB.INSERT_QUERY_ORACLE);
            else
                throw new IllegalArgumentException("Unknown database type, can't continue: " + dbType);

            stmt.setLong(1,begin);
            stmt.setLong(2,begin);
            stmt.setLong(3,begin);
            long t0 = System.currentTimeMillis();
            int count = stmt.executeUpdate();
            long t1 = System.currentTimeMillis();
            log.info("Done calculating OOBs. [" + count + "] new entries in [" + (t1-t0) + "] ms");
        }
        catch (SQLException e) {
            log.error(e);
        }
        catch (Exception e) {
            log.error(e);
        }
        finally {
            JDBCUtil.safeClose(conn,stmt,null);
        }
    }

    /**
     * Computes the OOBs for the last hour.
     * This is done by getting the latest timestamp of the 1h table and invoking
     * #computeOOBsFromHourBeginingAt
     * @param subject
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void computeOOBsFromLastHour(Subject subject) {

        Query q = entityManager.createNamedQuery(MeasurementDataNumeric1H.GET_MAX_TIMESTAMP);
        Object res  = q.getSingleResult();
        if (res==null) {
            if (log.isDebugEnabled())
                log.debug("No data yet in 1h table, nothing to do");
            return; // no data in that table yet - nothing to do.
        }
        long timeStamp = (Long)res;

        computeOOBsFromHourBeginingAt(subject,timeStamp);

    }

    /**
     * Remove old OOB entries from the database
     * @param subject Subject of the caller
     * @param end oldest value to keep
     */
    @TransactionAttribute(value = TransactionAttributeType.REQUIRES_NEW)
    public void removeOldOOBs(Subject subject, long end) {

        Query q = entityManager.createQuery("DELETE FROM MeasurementOOB mo WHERE mo.id.timestamp < :time");
        q.setParameter("time",end);
        int count = q.executeUpdate();
        log.info("Removed [" + count + "] old OOB entries");
    }

    /**
     * Return OOB Composites that contain all information about the OOBs in a given time as aggregates.
     * @param subject The caller
     * @param begin begin time we are interested in
     * @param end end time we are interested in
     * @return List of schedules with the corresponing oob aggregates
     */
    public List<MeasurementOOBComposite> getSchedulesWithOOBs(Subject subject, long begin, long end) {

        Query q = entityManager.createNamedQuery(MeasurementOOB.GET_SCHEDULES_WITH_OOB_AGGREGATE);
        q.setParameter("begin",begin);
        q.setParameter("end",end);
        List<MeasurementOOBComposite> res = q.getResultList();

        return res;
    }

    /**
     * Get the individual oob values for the given time frame and schedule Id.
     * @param subject Caller
     * @param scheduleId PK of the schedule we are interested in
     * @param begin Start timestamp of the time frame
     * @param end End timestamp of the time frame
     * @return A list of individual OOB entries
     * @todo Do we want to fill gaps with count=0 or factor=0 in the result or not?
     */
    public List<MeasurementOOB> getOObsForSchedule(Subject subject, int scheduleId, long begin, long end) {

        Query q = entityManager.createNamedQuery(MeasurementOOB.GET_OOBS_FOR_SCHEDULE_RAW);
        q.setParameter("begin",begin);
        q.setParameter("end",end);
        q.setParameter("scheduleId", scheduleId);
        List<MeasurementOOB> res = q.getResultList();

        return res;

    }
}
